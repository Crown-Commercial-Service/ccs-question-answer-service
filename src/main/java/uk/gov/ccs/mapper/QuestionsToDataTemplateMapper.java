package uk.gov.ccs.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.model.agreements.Dependency;
import uk.gov.ccs.model.agreements.Requirement;
import uk.gov.ccs.model.agreements.RequirementGroup;
import uk.gov.ccs.model.agreements.TemplateCriteria;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper to convert flat Questions table rows to hierarchical DataTemplate structure
 * Returns the same format as agreements-service
 */
@Component
public class QuestionsToDataTemplateMapper {
    
    @Autowired
    private Rollbar rollbar;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert flat Questions list to hierarchical DataTemplate structure
     * Matches the exact structure returned by agreements-service
     * 
     * @param questions Flat list of questions from database
     * @return List of DataTemplate objects (same format as agreements-service)
     */
    public List<DataTemplate> mapToDataTemplate(List<Questions> questions) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Group by criteria_id to create TemplateCriteria
            Map<String, List<Questions>> byCriteria = questions.stream()
                .collect(Collectors.groupingBy(Questions::getCriteriaId));
            
            List<DataTemplate> dataTemplates = new ArrayList<>();
            
            // For each criteria, create a DataTemplate
            for (Map.Entry<String, List<Questions>> criteriaEntry : byCriteria.entrySet()) {
                String criteriaId = criteriaEntry.getKey();
                List<Questions> criteriaQuestions = criteriaEntry.getValue();
                
                if (criteriaQuestions.isEmpty()) {
                    continue;
                }
                
                // Get criterion metadata from first question
                Questions firstQuestion = criteriaQuestions.get(0);
                String criterionTitle = firstQuestion.getCriterionTitle();
                
                // Group by group_id to create RequirementGroups
                Map<String, List<Questions>> byGroup = criteriaQuestions.stream()
                    .collect(Collectors.groupingBy(Questions::getGroupId));
                
                // Build RequirementGroups
                Set<RequirementGroup> requirementGroups = new LinkedHashSet<>();
                for (Map.Entry<String, List<Questions>> groupEntry : byGroup.entrySet()) {
                    String groupId = groupEntry.getKey();
                    List<Questions> groupQuestions = groupEntry.getValue();
                    
                    RequirementGroup group = buildRequirementGroup(groupId, groupQuestions);
                    if (group != null) {
                        requirementGroups.add(group);
                    }
                }
                
                // Build TemplateCriteria
                TemplateCriteria criteria = TemplateCriteria.builder()
                    .id(criteriaId)
                    .title(criterionTitle)
                    .description(null) // Not available in questions table
                    .source(null) // Not available in questions table
                    .relatesTo(null) // Not available in questions table
                    .relateItems(null) // Not available in questions table
                    .inheritanceNonOCDS(null) // Not available in questions table
                    .requirementGroups(requirementGroups)
                    .build();
                
                // Build DataTemplate
                // Note: id, templateName, parent, mandatory are not stored in questions table
                // These are typically null in agreements-service response as well
                DataTemplate dataTemplate = DataTemplate.builder()
                    .id(null) // Not available in questions table
                    .templateName(null) // Not available in questions table
                    .parent(null) // Not available in questions table
                    .mandatory(null) // Not available in questions table
                    .criteria(List.of(criteria))
                    .build();
                
                dataTemplates.add(dataTemplate);
            }
            
            return dataTemplates;
        } catch (Exception ex) {
            rollbar.error(ex, "Error mapping Questions to DataTemplate");
            return Collections.emptyList();
        }
    }
    
    /**
     * Build RequirementGroup from flat questions
     */
    private RequirementGroup buildRequirementGroup(String groupId, List<Questions> groupQuestions) {
        if (groupQuestions == null || groupQuestions.isEmpty()) {
            return null;
        }
        
        Questions first = groupQuestions.get(0);
        
        // Build OCDS part
        RequirementGroup.OCDS ocds = RequirementGroup.OCDS.builder()
            .id(groupId)
            .description(first.getGroupDescription())
            .requirements(groupQuestions.stream()
                .sorted(Comparator.comparing(Questions::getQuestionOrder, 
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
            .inheritance(null) // Not available in questions table
            .build();
        
        return RequirementGroup.builder()
            .ocds(ocds)
            .nonOCDS(nonOCDS)
            .build();
    }
    
    /**
     * Build Requirement from Questions entity
     */
    private Requirement buildRequirement(Questions question) {
        try {
            // Build OCDS part
            Requirement.OCDS ocds = Requirement.OCDS.builder()
                .id(question.getQuestionId())
                .title(question.getQuestionTitle())
                .description(question.getQuestionDescription())
                .pattern(null) // Not available in questions table
                .dataType(question.getQuestionDataType())
                .expectedValue(null) // Not available in questions table
                .minValue(null) // Not available in questions table
                .maxValue(null) // Not available in questions table
                .period(null) // Not available in questions table
                .build();
            
            // Build nonOCDS part
            Requirement.NonOCDS.NonOCDSBuilder nonOCDSBuilder = Requirement.NonOCDS.builder()
                .order(question.getQuestionOrder())
                .mandatory(question.getQuestionMandatory())
                .multiAnswer(question.getQuestionMultiAnswer())
                .questionType(question.getQuestionType())
                .answered(question.getQuestionAnswered() != null ? question.getQuestionAnswered() : false)
                .length(null) // Not available in questions table
                .inheritance(null) // Not available in questions table
                .inheritsFrom(null) // Not available in questions table
                .timelineDependency(null) // Not available in questions table
                .options(null); // Not available in questions table (answers stored separately)
            
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
            rollbar.warning("Error building Requirement from Questions: " + ex.getMessage());
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

