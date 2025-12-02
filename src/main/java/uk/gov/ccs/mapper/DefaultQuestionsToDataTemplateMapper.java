package uk.gov.ccs.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.model.agreements.Dependency;
import uk.gov.ccs.model.agreements.Requirement;
import uk.gov.ccs.model.agreements.RequirementGroup;
import uk.gov.ccs.model.agreements.TemplateCriteria;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper to convert flat DefaultQuestions table rows to hierarchical DataTemplate structure
 * Returns the same format as agreements-service
 */
@Component
public class DefaultQuestionsToDataTemplateMapper {
    
    @Autowired
    private Rollbar rollbar;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert flat DefaultQuestions list to hierarchical DataTemplate structure
     * Matches the exact structure returned by agreements-service
     * 
     * @param defaultQuestions Flat list of default questions from database
     * @return List of DataTemplate objects (same format as agreements-service)
     */
    public List<DataTemplate> mapToDataTemplate(List<DefaultQuestions> defaultQuestions) {
        if (defaultQuestions == null || defaultQuestions.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Group by criteria_id to create TemplateCriteria
            Map<String, List<DefaultQuestions>> byCriteria = defaultQuestions.stream()
                .collect(Collectors.groupingBy(DefaultQuestions::getCriteriaId));
            
            List<DataTemplate> dataTemplates = new ArrayList<>();
            
            // For each criteria, create a DataTemplate
            for (Map.Entry<String, List<DefaultQuestions>> criteriaEntry : byCriteria.entrySet()) {
                String criteriaId = criteriaEntry.getKey();
                List<DefaultQuestions> criteriaQuestions = criteriaEntry.getValue();
                
                if (criteriaQuestions.isEmpty()) {
                    continue;
                }
                
                // Get criterion metadata from first question
                DefaultQuestions firstQuestion = criteriaQuestions.get(0);
                String criterionTitle = firstQuestion.getCriterionTitle();
                
                // Group by group_id to create RequirementGroups
                Map<String, List<DefaultQuestions>> byGroup = criteriaQuestions.stream()
                    .collect(Collectors.groupingBy(DefaultQuestions::getGroupId));
                
                // Build RequirementGroups
                Set<RequirementGroup> requirementGroups = new LinkedHashSet<>();
                for (Map.Entry<String, List<DefaultQuestions>> groupEntry : byGroup.entrySet()) {
                    String groupId = groupEntry.getKey();
                    List<DefaultQuestions> groupQuestions = groupEntry.getValue();
                    
                    RequirementGroup group = buildRequirementGroup(groupId, groupQuestions);
                    if (group != null) {
                        requirementGroups.add(group);
                    }
                }
                
                // Build TemplateCriteria
                TemplateCriteria criteria = TemplateCriteria.builder()
                    .id(criteriaId)
                    .title(criterionTitle)
                    .description(null) // Not available in default_questions table
                    .source(null) // Not available in default_questions table
                    .relatesTo(null) // Not available in default_questions table
                    .relateItems(null) // Not available in default_questions table
                    .inheritanceNonOCDS(null) // Not available in default_questions table
                    .requirementGroups(requirementGroups)
                    .build();
                
                // Build DataTemplate
                // Note: id, templateName, parent, mandatory are not stored in default_questions table
                // These are typically null in agreements-service response as well
                DataTemplate dataTemplate = DataTemplate.builder()
                    .id(null) // Not available in default_questions table
                    .templateName(null) // Not available in default_questions table
                    .parent(null) // Not available in default_questions table
                    .mandatory(null) // Not available in default_questions table
                    .criteria(List.of(criteria))
                    .build();
                
                dataTemplates.add(dataTemplate);
            }
            
            return dataTemplates;
        } catch (Exception ex) {
            rollbar.error(ex, "Error mapping DefaultQuestions to DataTemplate");
            return Collections.emptyList();
        }
    }
    
    /**
     * Build RequirementGroup from flat default questions
     */
    private RequirementGroup buildRequirementGroup(String groupId, List<DefaultQuestions> groupQuestions) {
        if (groupQuestions == null || groupQuestions.isEmpty()) {
            return null;
        }
        
        DefaultQuestions first = groupQuestions.get(0);
        
        // Build OCDS part
        RequirementGroup.OCDS ocds = RequirementGroup.OCDS.builder()
            .id(groupId)
            .description(first.getGroupDescription())
            .requirements(groupQuestions.stream()
                .sorted(Comparator.comparing(DefaultQuestions::getQuestionOrder, 
                    Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::buildRequirement)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()))
            .build();
        
        // Build nonOCDS part
        RequirementGroup.NonOCDS nonOCDS = RequirementGroup.NonOCDS.builder()
            .task(first.getGroupTask())
            .order(first.getGroupOrder())
            .prompt(first.getGroupPrompt())
            .mandatory(first.getGroupMandatory())
            .inheritance(null) // Not available in default_questions table
            .build();
        
        return RequirementGroup.builder()
            .ocds(ocds)
            .nonOCDS(nonOCDS)
            .build();
    }
    
    /**
     * Build Requirement from DefaultQuestions entity
     */
    private Requirement buildRequirement(DefaultQuestions question) {
        try {
            // Build OCDS part
            Requirement.OCDS ocds = Requirement.OCDS.builder()
                .id(question.getQuestionId())
                .title(question.getQuestionTitle())
                .description(question.getQuestionDescription())
                .pattern(null) // Not available in default_questions table
                .dataType(question.getQuestionDataType())
                .expectedValue(null) // Not available in default_questions table
                .minValue(null) // Not available in default_questions table
                .maxValue(null) // Not available in default_questions table
                .period(null) // Not available in default_questions table
                .build();
            
            // Build nonOCDS part
            Requirement.NonOCDS.NonOCDSBuilder nonOCDSBuilder = Requirement.NonOCDS.builder()
                .order(question.getQuestionOrder())
                .mandatory(question.getQuestionMandatory())
                .multiAnswer(question.getQuestionMultiAnswer())
                .questionType(question.getQuestionType())
                .answered(question.getQuestionAnswered() != null ? question.getQuestionAnswered() : false)
                .length(null) // Not available in default_questions table
                .inheritance(null) // Not available in default_questions table
                .inheritsFrom(null) // Not available in default_questions table
                .timelineDependency(null) // Not available in default_questions table
                .options(null); // Not available in default_questions table (answers stored separately)
            
            // Parse dependency if exists
            Dependency dependency = null;
            if (question.getQuestionDependency() != null && !question.getQuestionDependency().trim().isEmpty()) {
                try {
                    Map<String, Object> dependencyMap = objectMapper.readValue(
                        question.getQuestionDependency(), 
                        new TypeReference<Map<String, Object>>() {}
                    );
                    // Map to Dependency object
                    dependency = mapDependency(dependencyMap);
                } catch (Exception ex) {
                    rollbar.warning("Error parsing question dependency: " + ex.getMessage());
                }
            }
            nonOCDSBuilder.dependency(dependency);
            
            Requirement.NonOCDS nonOCDS = nonOCDSBuilder.build();
            
            return Requirement.builder()
                .ocds(ocds)
                .nonOCDS(nonOCDS)
                .build();
        } catch (Exception ex) {
            rollbar.warning("Error building Requirement from DefaultQuestions: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Map dependency JSON to Dependency object
     */
    private Dependency mapDependency(Map<String, Object> dependencyMap) {
        try {
            // Convert Map to Dependency using ObjectMapper
            return objectMapper.convertValue(dependencyMap, Dependency.class);
        } catch (Exception ex) {
            rollbar.warning("Error mapping dependency: " + ex.getMessage());
            return null;
        }
    }
}

