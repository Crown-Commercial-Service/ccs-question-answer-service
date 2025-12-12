package uk.gov.ccs.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapper to convert hierarchical DataTemplate structure to flat DefaultQuestions entities
 * This is the reverse of DefaultQuestionsToDataTemplateMapper
 */
@Component
public class DataTemplateToDefaultQuestionsMapper {
    
    @Autowired
    private Rollbar rollbar;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert hierarchical DataTemplate list to flat DefaultQuestions entities
     * 
     * @param dataTemplates List of DataTemplate objects
     * @param agreementId The agreement ID
     * @param lotId The lot ID
     * @return List of DefaultQuestions entities ready for database insertion
     */
    public List<DefaultQuestions> mapToDefaultQuestions(
            List<DataTemplate> dataTemplates, String agreementId, String lotId) {
        
        List<DefaultQuestions> defaultQuestions = new ArrayList<>();
        
        if (dataTemplates == null || dataTemplates.isEmpty()) {
            return defaultQuestions;
        }
        
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        try {
            for (DataTemplate dataTemplate : dataTemplates) {
                if (dataTemplate.getCriteria() != null) {
                    for (TemplateCriteria criteria : dataTemplate.getCriteria()) {
                        List<DefaultQuestions> criteriaQuestions = mapCriteriaToDefaultQuestions(
                            criteria, agreementId, lotId, now);
                        defaultQuestions.addAll(criteriaQuestions);
                    }
                }
            }
        } catch (Exception ex) {
            rollbar.error(ex, "Error mapping DataTemplate to DefaultQuestions");
        }
        
        return defaultQuestions;
    }
    
    /**
     * Map TemplateCriteria to list of DefaultQuestions
     */
    private List<DefaultQuestions> mapCriteriaToDefaultQuestions(
            TemplateCriteria criteria, String agreementId, String lotId, Timestamp now) {
        
        List<DefaultQuestions> questions = new ArrayList<>();
        
        if (criteria.getRequirementGroups() == null) {
            return questions;
        }
        
        for (RequirementGroup requirementGroup : criteria.getRequirementGroups()) {
            List<DefaultQuestions> groupQuestions = mapRequirementGroupToDefaultQuestions(
                criteria, requirementGroup, agreementId, lotId, now);
            questions.addAll(groupQuestions);
        }
        
        return questions;
    }
    
    /**
     * Map RequirementGroup to list of DefaultQuestions
     */
    private List<DefaultQuestions> mapRequirementGroupToDefaultQuestions(
            TemplateCriteria criteria, RequirementGroup requirementGroup,
            String agreementId, String lotId, Timestamp now) {
        
        List<DefaultQuestions> questions = new ArrayList<>();
        
        if (requirementGroup.getOcds() == null || 
            requirementGroup.getOcds().getRequirements() == null) {
            return questions;
        }
        
        RequirementGroup.OCDS ocds = requirementGroup.getOcds();
        RequirementGroup.NonOCDS nonOCDS = requirementGroup.getNonOCDS();
        
        for (Requirement requirement : ocds.getRequirements()) {
            DefaultQuestions defaultQuestion = buildDefaultQuestion(
                criteria, requirementGroup, requirement, agreementId, lotId, now);
            if (defaultQuestion != null) {
                questions.add(defaultQuestion);
            }
        }
        
        return questions;
    }
    
    /**
     * Build a single DefaultQuestions entity from the hierarchical structure
     */
    private DefaultQuestions buildDefaultQuestion(
            TemplateCriteria criteria, RequirementGroup requirementGroup,
            Requirement requirement, String agreementId, String lotId, Timestamp now) {
        
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
                    rollbar.warning("Error serializing dependency to JSON: " + e.getMessage());
                }
            }
            
            DefaultQuestions.DefaultQuestionsBuilder builder = DefaultQuestions.builder()
                .agreementId(agreementId)
                .lotId(lotId)
                .criteriaId(criteria.getId())
                .criterionTitle(criteria.getTitle())
                .groupId(groupOcds != null ? groupOcds.getId() : null)
                .groupDescription(groupOcds != null ? groupOcds.getDescription() : null)
                .groupTask(groupNonOCDS != null ? groupNonOCDS.getTask() : null)
                .groupOrder(groupNonOCDS != null ? groupNonOCDS.getOrder() : null)
                .groupPrompt(groupNonOCDS != null ? groupNonOCDS.getPrompt() : null)
                .groupMandatory(groupNonOCDS != null ? groupNonOCDS.getMandatory() : null)
                .questionId(ocds != null ? ocds.getId() : null)
                .questionTitle(ocds != null ? ocds.getTitle() : null)
                .questionDescription(ocds != null ? ocds.getDescription() : null)
                .questionDataType(ocds != null ? ocds.getDataType() : null)
                .questionOrder(nonOCDS != null ? nonOCDS.getOrder() : null)
                .questionAnswered(nonOCDS != null && nonOCDS.getAnswered() != null ? nonOCDS.getAnswered() : false)
                .questionMandatory(nonOCDS != null ? nonOCDS.getMandatory() : null)
                .questionDependency(dependencyJson)
                .questionMultiAnswer(nonOCDS != null ? nonOCDS.getMultiAnswer() : null)
                .questionType(nonOCDS != null ? nonOCDS.getQuestionType() : null)
                .createdAt(now)
                .updatedAt(now);
            
            return builder.build();
            
        } catch (Exception ex) {
            rollbar.warning("Error building DefaultQuestion: " + ex.getMessage());
            return null;
        }
    }
}

