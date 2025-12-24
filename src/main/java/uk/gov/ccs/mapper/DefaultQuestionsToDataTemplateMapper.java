package uk.gov.ccs.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.model.agreements.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper to convert flat DefaultQuestions table rows to hierarchical DataTemplate structure
 * Returns the same format as agreements-service
 */
@Component
public class DefaultQuestionsToDataTemplateMapper extends BaseMapper {

    
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
            // Group by criteriaId to form the base structure (DataTemplate -> TemplateCriteria)
            return defaultQuestions.stream()
                    .collect(Collectors.groupingBy(DefaultQuestions::getCriteriaId))
                    .entrySet().stream()
                    // Map each criteria group to a DataTemplate
                    .map(criteriaEntry -> {
                        String criteriaId = criteriaEntry.getKey();
                        List<DefaultQuestions> criteriaQuestions = criteriaEntry.getValue();

                        if (criteriaQuestions.isEmpty()) {
                            return null;
                        }

                        // Get criterion metadata from first question
                        DefaultQuestions firstQuestion = criteriaQuestions.get(0);
                        String criterionTitle = firstQuestion.getCriterionTitle();

                        // Group by groupId and map to RequirementGroups
                        Set<RequirementGroup> requirementGroups = criteriaQuestions.stream()
                                .collect(Collectors.groupingBy(DefaultQuestions::getGroupId))
                                .entrySet().stream()
                                // 4. Map each group entry to a RequirementGroup
                                .map(groupEntry -> buildRequirementGroup(groupEntry.getKey(), groupEntry.getValue()))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toCollection(LinkedHashSet::new)); // Preserve insertion order if possible

                        // Build TemplateCriteria
                        TemplateCriteria criteria = TemplateCriteria.builder()
                                .id(criteriaId)
                                .title(criterionTitle)
                                .description(null)
                                .source(null)
                                .relatesTo(null)
                                .relateItems(null)
                                .inheritanceNonOCDS(null)
                                .requirementGroups(requirementGroups)
                                .build();

                        // Build DataTemplate (wrapping the single criteria)
                        return DataTemplate.builder()
                                .id(firstQuestion.getId())
                                .templateName(firstQuestion.getTemplateName())
                                .parent(firstQuestion.getTemplateParent())
                                .mandatory(firstQuestion.getTemplateMandatory())
                                .criteria(List.of(criteria))
                                .build();
                    })
                    .filter(Objects::nonNull) // Filter out any unexpected nulls
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("Error mapping DefaultQuestions to DataTemplate. error {}", ex.getMessage());
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
                    log.error("Error parsing question dependency. error {}", ex.getMessage());
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
            log.error("Error building Requirement from DefaultQuestions. error {}", ex.getMessage());
            rollbar.warning("Error building Requirement from DefaultQuestions: " + ex.getMessage());
            return null;
        }
    }
}

