package uk.gov.ccs.mapper;

import org.springframework.stereotype.Component;
import uk.gov.ccs.dts.qas.model.generated.Criterion;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.dts.qas.model.generated.QuestionGroup;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.model.agreements.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mapper to convert DataTemplate (from Agreements Service) to QuestionWrite (for Question Service)
 */
@Component
public class DataTemplateMapper extends BaseMapper {

    /**
     * Maps a list of DataTemplate objects to QuestionWrite format
     * 
     * @param dataTemplates List of DataTemplate from Agreements Service
     * @param agreementId The agreement ID
     * @param lotId The lot ID
     * @return QuestionWrite object containing the mapped data, or null if no criteria found
     */
    public QuestionWrite mapToQuestionWrite(List<DataTemplate> dataTemplates, String agreementId, String lotId) {

        if (dataTemplates == null || dataTemplates.isEmpty()) {
            log.warn("dataTemplate is null");
            return null;
        }

        try {
            // Flatten criteria from all templates and map them to Criterion objects
            List<Criterion> criteria = dataTemplates.stream()
                    .flatMap(template -> Optional.ofNullable(template.getCriteria()).orElse(Collections.emptyList()).stream())
                    .map(this::mapTemplateCriteriaToCriterion)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            // If no criteria found after mapping, return null
            if (criteria.isEmpty()) {
                log.warn("No criteria found");
                return null;
            }

            QuestionWrite questionWrite = new QuestionWrite();
            questionWrite.setAgreementId(agreementId);
            questionWrite.setLotId(lotId);
            questionWrite.setCriterion(criteria);

            return questionWrite;
        } catch (Exception ex) {
            log.error("Error mapping DataTemplate to QuestionWrite, error {}", ex.getMessage());
            rollbar.error(ex, "Error mapping DataTemplate to QuestionWrite");
            return null;
        }
    }

    /**
     * Maps TemplateCriteria to Criterion
     */
    private Criterion mapTemplateCriteriaToCriterion(TemplateCriteria templateCriteria) {
        try {
            Criterion criterion = new Criterion();

            // Map simple fields with null checks
            criterion.setCriteriaId(templateCriteria.getId());
            criterion.setTitle(templateCriteria.getTitle());

            // Map requirementGroups using streams
            List<QuestionGroup> questionGroups = Optional.ofNullable(templateCriteria.getRequirementGroups())
                    .orElse(Collections.emptySet())
                    .stream()
                    .map(this::mapRequirementGroupToQuestionGroup)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            criterion.setRequirementGroups(questionGroups);

            return criterion;
        } catch (Exception ex) {
            log.error("Error mapping TemplateCriteria to Criterion: {}", ex.getMessage());
            rollbar.warning("Error mapping TemplateCriteria to Criterion: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Maps RequirementGroup to QuestionGroup
     */
    private QuestionGroup mapRequirementGroupToQuestionGroup(RequirementGroup requirementGroup) {
        try {
            QuestionGroup questionGroup = new QuestionGroup();

            RequirementGroup.OCDS ocds = requirementGroup.getOcds();
            if (ocds != null) {
                // Map OCDS fields
                questionGroup.setGroupId(ocds.getId());
                questionGroup.setDescription(ocds.getDescription());

                // Map requirements using streams
                List<Question> questions = Optional.ofNullable(ocds.getRequirements())
                        .orElse(Collections.emptySet())
                        .stream()
                        .map(this::mapRequirementToQuestion)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                questionGroup.setRequirements(questions);
            }

            // Ensure groupId is set (required field) - using default if missing
            questionGroup.setGroupId(Optional.ofNullable(questionGroup.getGroupId())
                    .filter(id -> !id.trim().isEmpty())
                    .orElse("Group " + System.currentTimeMillis()));

            // Map nonOCDS fields and set defaults
            RequirementGroup.NonOCDS nonOCDS = requirementGroup.getNonOCDS();

            // 1. Set Task (Complex Fallback Logic Simplified)
            String defaultTaskFallback = Optional.ofNullable(questionGroup.getDescription())
                    .filter(d -> !d.trim().isEmpty())
                    .orElse(questionGroup.getGroupId());
            defaultTaskFallback = Optional.ofNullable(defaultTaskFallback)
                    .filter(t -> !t.trim().isEmpty())
                    .orElse("Default Task");

            String task = Optional.ofNullable(nonOCDS)
                    .map(RequirementGroup.NonOCDS::getTask)
                    .filter(t -> !t.trim().isEmpty())
                    .orElse(defaultTaskFallback);
            questionGroup.setTask(task);

            // 2. Set Order (Default to ZERO)
            questionGroup.setOrder(Optional.ofNullable(nonOCDS)
                    .map(RequirementGroup.NonOCDS::getOrder)
                    .map(BigDecimal::valueOf)
                    .orElse(BigDecimal.ZERO));

            // 3. Set Prompt
            questionGroup.setPrompt(Optional.ofNullable(nonOCDS).map(RequirementGroup.NonOCDS::getPrompt).orElse(null));

            // 4. Set Mandatory (Default to false)
            questionGroup.setMandatory(Optional.ofNullable(nonOCDS)
                    .map(RequirementGroup.NonOCDS::getMandatory)
                    .orElse(false));

            return questionGroup;
        } catch (Exception ex) {
            log.error("Error mapping RequirementGroup to QuestionGroup: {}", ex.getMessage());
            rollbar.warning("Error mapping RequirementGroup to QuestionGroup: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Maps Requirement to Question
     */
    private Question mapRequirementToQuestion(Requirement requirement) {
        try {
            Question question = new Question();
            question.setIsDefaultQuestion(true); // Always true for template data

            Requirement.OCDS ocds = requirement.getOcds();
            if (ocds != null) {
                // Map OCDS fields
                question.setQuestionId(ocds.getId());
                question.setTitle(ocds.getTitle());
                question.setDescription(ocds.getDescription());
                question.setDataType(ocds.getDataType());
            }

            // Ensure QuestionId is set (required field)
            question.setQuestionId(Optional.ofNullable(question.getQuestionId())
                    .filter(id -> !id.trim().isEmpty())
                    .orElse("Question " + System.currentTimeMillis()));

            // Ensure Title is set (required field)
            question.setTitle(Optional.ofNullable(question.getTitle())
                    .filter(title -> !title.trim().isEmpty())
                    .orElse(question.getQuestionId()));

            // Ensure DataType is set (required field)
            question.setDataType(Optional.ofNullable(question.getDataType())
                    .filter(type -> !type.trim().isEmpty())
                    .orElse("string"));

            // Map nonOCDS fields and set defaults
            Requirement.NonOCDS nonOCDS = requirement.getNonOCDS();

            // Set Order (Default to ZERO)
            question.setOrder(Optional.ofNullable(nonOCDS)
                    .map(Requirement.NonOCDS::getOrder)
                    .map(BigDecimal::valueOf)
                    .orElse(BigDecimal.ZERO));

            // Set Answered (Default to false)
            question.setAnswered(Optional.ofNullable(nonOCDS)
                    .map(Requirement.NonOCDS::getAnswered)
                    .orElse(false));

            // Set Mandatory (Default to false)
            question.setMandatory(Optional.ofNullable(nonOCDS)
                    .map(Requirement.NonOCDS::getMandatory)
                    .orElse(false));

            // Set MultiAnswer (Default to false)
            question.setMultiAnswer(Optional.ofNullable(nonOCDS)
                    .map(Requirement.NonOCDS::getMultiAnswer)
                    .orElse(false));

            // Set QuestionType (Default to "Text")
            question.setQuestionType(Optional.ofNullable(nonOCDS)
                    .map(Requirement.NonOCDS::getQuestionType)
                    .filter(type -> !type.trim().isEmpty())
                    .orElse("Text"));

            // Map Dependency (Default to empty map)
            question.setDependency(Optional.ofNullable(nonOCDS)
                    .map(Requirement.NonOCDS::getDependency)
                    .map(this::mapDependencyToMap)
                    .orElse(new HashMap<>()));

            return question;
        } catch (Exception ex) {
            log.error("Error mapping Requirement to Question: {}", ex.getMessage());
            rollbar.warning("Error mapping Requirement to Question: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Maps Dependency object to Map<String, Object> for JSON serialization
     */
    private Map<String, Object> mapDependencyToMap(Dependency dependency) {
        try {
            Map<String, Object> dependencyMap = new HashMap<>();

            // Map Conditional
            Optional.ofNullable(dependency.getConditional()).ifPresent(conditional -> {
                Map<String, Object> conditionalMap = new HashMap<>();
                conditionalMap.put("dependentOnID", conditional.getDependentOnID());
                conditionalMap.put("dependencyType", Optional.ofNullable(conditional.getDependencyType())
                        .map(DependencyType::getValue)
                        .orElse(null));
                conditionalMap.put("dependencyValue", conditional.getDependencyValue());
                dependencyMap.put("conditional", conditionalMap);
            });

            // Map Relationships
            Optional.ofNullable(dependency.getRelationships())
                    .filter(rels -> !rels.isEmpty())
                    .ifPresent(relationships -> {
                        List<Map<String, Object>> relationshipsList = relationships.stream()
                                .map(rel -> {
                                    Map<String, Object> relMap = new HashMap<>();
                                    relMap.put("dependentOnID", rel.getDependentOnID());
                                    relMap.put("relationshipType", rel.getRelationshipType());
                                    return relMap;
                                })
                                .collect(Collectors.toList());
                        dependencyMap.put("relationships", relationshipsList);
                    });

            return dependencyMap;
        } catch (Exception ex) {
            log.error("Error mapping Dependency to Map: {}", ex.getMessage());
            rollbar.warning("Error mapping Dependency to Map: " + ex.getMessage());
            return new HashMap<>();
        }
    }
}

