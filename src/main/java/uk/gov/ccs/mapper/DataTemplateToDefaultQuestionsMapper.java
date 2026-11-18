package uk.gov.ccs.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Component;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.model.agreements.Requirement;
import uk.gov.ccs.model.agreements.RequirementGroup;
import uk.gov.ccs.model.agreements.TemplateCriteria;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Mapper to convert hierarchical DataTemplate structure to flat DefaultQuestions entities
 * This is the reverse of DefaultQuestionsToDataTemplateMapper
 */
@Component
public class DataTemplateToDefaultQuestionsMapper extends BaseMapper {
    
    /**
     * Convert hierarchical DataTemplate list to flat DefaultQuestions entities
     * 
     * @param dataTemplates List of DataTemplate objects
     * @param agreementId The agreement ID
     * @param lotId The lot ID
     * @return List of DefaultQuestions entities ready for database insertion
     */
    public List<DefaultQuestions> mapToDefaultQuestions(
            List<DataTemplate> dataTemplates, String agreementId, String lotId,
            String eventType) {

        if (dataTemplates == null || dataTemplates.isEmpty()) {
            return Collections.emptyList();
        }

        final Timestamp now = new Timestamp(System.currentTimeMillis());

        try {
            return dataTemplates.stream()
                    // Flatten DataTemplate to TemplateCriteria, keeping reference to parent DataTemplate
                    .filter(dt -> dt.getCriteria() != null)
                    .flatMap(dt -> dt.getCriteria().stream()
                            .map(criteria -> new Object[] { dt, criteria }))

                    // Flatten TemplateCriteria to RequirementGroup
                    .filter(arr -> {
                        TemplateCriteria criteria = (TemplateCriteria) arr[1];
                        return criteria.getRequirementGroups() != null;
                    })
                    .flatMap(arr -> {
                        DataTemplate dt = (DataTemplate) arr[0];
                        TemplateCriteria criteria = (TemplateCriteria) arr[1];
                        return criteria.getRequirementGroups().stream()
                                .map(rg -> new Object[] { dt, criteria, rg });
                    })

                    // Flatten RequirementGroup to Requirement
                    .filter(arr -> {
                        RequirementGroup rg = (RequirementGroup) arr[2];
                        return rg.getOcds() != null && rg.getOcds().getRequirements() != null;
                    })
                    .flatMap(arr -> {
                        DataTemplate dt = (DataTemplate) arr[0];
                        TemplateCriteria criteria = (TemplateCriteria) arr[1];
                        RequirementGroup rg = (RequirementGroup) arr[2];
                        return rg.getOcds().getRequirements().stream()
                                .map(req -> new Object[] { dt, criteria, rg, req });
                    })

                    // Map the final quadruplet (Template, Criteria, Group, Requirement) to the final entity
                    .map(arr -> {
                        DataTemplate dt = (DataTemplate) arr[0];
                        TemplateCriteria criteria = (TemplateCriteria) arr[1];
                        RequirementGroup rg = (RequirementGroup) arr[2];
                        Requirement req = (Requirement) arr[3];
                        return buildDefaultQuestion(dt, criteria, rg, req, agreementId, lotId, eventType, now);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception ex) {
            log.error("Error mapping DataTemplate to DefaultQuestions, error{}", ex.getMessage());
            rollbar.error(ex, "Error mapping DataTemplate to DefaultQuestions");
            return Collections.emptyList();
        }
    }

    /**
     * Build default question
     */
    private DefaultQuestions buildDefaultQuestion(
            DataTemplate dt,
            TemplateCriteria criteria, RequirementGroup requirementGroup,
            Requirement requirement, String agreementId,
            String lotId, String eventType, Timestamp now) {

        try {

            Requirement.OCDS ocds = requirement.getOcds();
            Requirement.NonOCDS nonOCDS = requirement.getNonOCDS();
            RequirementGroup.OCDS groupOcds = requirementGroup.getOcds();
            RequirementGroup.NonOCDS groupNonOCDS = requirementGroup.getNonOCDS();

            // Build dependency JSON string if exists
            String dependencyJson = null;
            if (nonOCDS != null && nonOCDS.getDependency() != null) {
                try {
                    dependencyJson = objectMapper.writeValueAsString(nonOCDS.getDependency());
                } catch (JsonProcessingException e) {
                    log.error("Error serializing dependency to JSON. error {}", e.getMessage());
                    rollbar.warning("Error serializing dependency to JSON: " + e.getMessage());
                }
            }

            // Helper to safely get title with fallback
            String questionTitle = (ocds != null ? ocds.getTitle() : null);
            if (questionTitle == null || questionTitle.trim().isEmpty()) {
                // Use question ID as fallback if title is missing
                questionTitle = (ocds != null && ocds.getId() != null) ? ocds.getId() : "Untitled Question";
            }

            // Helper to safely get description with fallback
            String questionDescription = (ocds != null ? ocds.getDescription() : null);
            if (questionDescription == null || questionDescription.trim().isEmpty()) {
                questionDescription = questionTitle; // Use title as fallback for description
            }

            // Helper to safely get group_task with fallback
            String groupTask = (groupNonOCDS != null ? groupNonOCDS.getTask() : null);
            if (groupTask == null || groupTask.trim().isEmpty()) {
                // Fallback 1: Group OCDS Description
                if (groupOcds != null && groupOcds.getDescription() != null && !groupOcds.getDescription().trim().isEmpty()) {
                    groupTask = groupOcds.getDescription();
                    // Fallback 2: Group OCDS ID
                } else if (groupOcds != null && groupOcds.getId() != null) {
                    groupTask = groupOcds.getId();
                    // Fallback 3: Default string
                } else {
                    groupTask = "Default Task";
                }
            }

            List<Requirement.Option> options = Optional.of(requirement)
                    .map(Requirement::getNonOCDS)
                    .map(Requirement.NonOCDS::getOptions)
                    .orElse(Collections.emptyList());

            DefaultQuestions.DefaultQuestionsBuilder builder = DefaultQuestions.builder()
                    .agreementId(agreementId)
                    .lotId(lotId)
                    .criteriaId(criteria.getId())
                    .criterionTitle(criteria.getTitle())
                    .groupId(groupOcds != null ? groupOcds.getId() : null)
                    .groupDescription(groupOcds != null ? groupOcds.getDescription() : null)
                    .groupTask(groupTask)
                    .groupOrder(groupNonOCDS != null ? groupNonOCDS.getOrder() : null)
                    .groupPrompt(groupNonOCDS != null ? groupNonOCDS.getPrompt() : null)
                    .groupMandatory(groupNonOCDS != null ? groupNonOCDS.getMandatory() : null)
                    .questionId(ocds != null ? ocds.getId() : null)
                    .questionTitle(questionTitle)
                    .questionDescription(questionDescription)
                    .questionDataType(ocds != null ? ocds.getDataType() : null)
                    .questionOrder(nonOCDS != null ? nonOCDS.getOrder() : null)
                    // Default to false if Answered is not present
                    .questionAnswered(nonOCDS != null && nonOCDS.getAnswered() != null ? nonOCDS.getAnswered() : false)
                    .questionMandatory(nonOCDS != null ? nonOCDS.getMandatory() : null)
                    .questionDependency(dependencyJson)
                    .questionMultiAnswer(nonOCDS != null ? nonOCDS.getMultiAnswer() : null)
                    .questionType(nonOCDS != null ? nonOCDS.getQuestionType() : null)
                    .createdAt(now)
                    .updatedAt(now)
                    .templateId(dt.getId())
                    .templateName(dt.getTemplateName())
                    .templateMandatory(dt.getMandatory())
                    .templateParent(dt.getParent()) // parent only available in DOS6
                    .eventType(eventType)
                    .options(objectMapper.writeValueAsString(options));

            return builder.build();

        } catch (Exception ex) {
            log.error("Error building DefaultQuestion. error {}", ex.getMessage());
            rollbar.warning("Error building DefaultQuestion: " + ex.getMessage());
            return null;
        }
    }
}

