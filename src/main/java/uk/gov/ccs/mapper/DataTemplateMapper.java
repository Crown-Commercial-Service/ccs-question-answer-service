package uk.gov.ccs.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ccs.dts.qas.model.generated.Criterion;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.dts.qas.model.generated.QuestionGroup;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.model.agreements.Dependency;
import uk.gov.ccs.model.agreements.Relationships;
import uk.gov.ccs.model.agreements.Requirement;
import uk.gov.ccs.model.agreements.RequirementGroup;
import uk.gov.ccs.model.agreements.TemplateCriteria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper to convert DataTemplate (from Agreements Service) to QuestionWrite (for Question Service)
 */
@Component
public class DataTemplateMapper {

    @Autowired
    private Rollbar rollbar;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Maps a list of DataTemplate objects to QuestionWrite format
     * 
     * @param dataTemplates List of DataTemplate from Agreements Service
     * @param agreementId The agreement ID
     * @param lotId The lot ID
     * @return QuestionWrite object containing the mapped data, or null if no criteria found
     */
    public QuestionWrite mapToQuestionWrite(List<DataTemplate> dataTemplates, String agreementId, String lotId) {
        try {
            if (dataTemplates == null || dataTemplates.isEmpty()) {
                return null;
            }

            QuestionWrite questionWrite = new QuestionWrite();
            questionWrite.setAgreementId(agreementId);
            questionWrite.setLotId(lotId);
            questionWrite.setCriterion(new ArrayList<>());

            // Extract criteria from all DataTemplates
            for (DataTemplate template : dataTemplates) {
                if (template.getCriteria() != null) {
                    for (TemplateCriteria templateCriteria : template.getCriteria()) {
                        Criterion criterion = mapTemplateCriteriaToCriterion(templateCriteria);
                        if (criterion != null) {
                            questionWrite.getCriterion().add(criterion);
                        }
                    }
                }
            }

            // If no criteria found, return null
            if (questionWrite.getCriterion().isEmpty()) {
                return null;
            }

            return questionWrite;
        } catch (Exception ex) {
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
            
            if (templateCriteria.getId() != null) {
                criterion.setCriteriaId(templateCriteria.getId());
            }
            
            if (templateCriteria.getTitle() != null) {
                criterion.setTitle(templateCriteria.getTitle());
            }
            
            // Map requirementGroups
            if (templateCriteria.getRequirementGroups() != null) {
                List<QuestionGroup> questionGroups = new ArrayList<>();
                
                for (RequirementGroup requirementGroup : templateCriteria.getRequirementGroups()) {
                    QuestionGroup questionGroup = mapRequirementGroupToQuestionGroup(requirementGroup);
                    if (questionGroup != null) {
                        questionGroups.add(questionGroup);
                    }
                }
                
                criterion.setRequirementGroups(questionGroups);
            }
            
            return criterion;
        } catch (Exception ex) {
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
            
            // Map OCDS fields
            if (requirementGroup.getOcds() != null) {
                RequirementGroup.OCDS ocds = requirementGroup.getOcds();
                
                if (ocds.getId() != null) {
                    questionGroup.setGroupId(ocds.getId());
                }
                
                if (ocds.getDescription() != null) {
                    questionGroup.setDescription(ocds.getDescription());
                }
                
                // Map requirements
                if (ocds.getRequirements() != null) {
                    List<Question> questions = new ArrayList<>();
                    
                    for (Requirement requirement : ocds.getRequirements()) {
                        Question question = mapRequirementToQuestion(requirement);
                        if (question != null) {
                            questions.add(question);
                        }
                    }
                    
                    questionGroup.setRequirements(questions);
                }
            }
            
            // Ensure groupId is set (required field)
            if (questionGroup.getGroupId() == null || questionGroup.getGroupId().trim().isEmpty()) {
                questionGroup.setGroupId("Group " + System.currentTimeMillis());
            }
            
            // Map nonOCDS fields
            if (requirementGroup.getNonOCDS() != null) {
                RequirementGroup.NonOCDS nonOCDS = requirementGroup.getNonOCDS();
                
                // Set task - use description or groupId as fallback
                if (nonOCDS.getTask() != null && !nonOCDS.getTask().trim().isEmpty()) {
                    questionGroup.setTask(nonOCDS.getTask());
                } else {
                    String defaultTask = questionGroup.getDescription();
                    if (defaultTask == null || defaultTask.trim().isEmpty()) {
                        defaultTask = questionGroup.getGroupId();
                    }
                    if (defaultTask == null || defaultTask.trim().isEmpty()) {
                        defaultTask = "Default Task";
                    }
                    questionGroup.setTask(defaultTask);
                }
                
                // Set order - default to 0 if missing
                if (nonOCDS.getOrder() != null) {
                    questionGroup.setOrder(BigDecimal.valueOf(nonOCDS.getOrder()));
                } else {
                    questionGroup.setOrder(BigDecimal.ZERO);
                }
                
                // Set prompt
                if (nonOCDS.getPrompt() != null) {
                    questionGroup.setPrompt(nonOCDS.getPrompt());
                }
                
                // Set mandatory - default to false if missing
                if (nonOCDS.getMandatory() != null) {
                    questionGroup.setMandatory(nonOCDS.getMandatory());
                } else {
                    questionGroup.setMandatory(false);
                }
            } else {
                // If nonOCDS is missing, set defaults
                String defaultTask = questionGroup.getDescription();
                if (defaultTask == null || defaultTask.trim().isEmpty()) {
                    defaultTask = questionGroup.getGroupId();
                }
                if (defaultTask == null || defaultTask.trim().isEmpty()) {
                    defaultTask = "Default Task";
                }
                questionGroup.setTask(defaultTask);
                questionGroup.setOrder(BigDecimal.ZERO);
                questionGroup.setMandatory(false);
            }
            
            return questionGroup;
        } catch (Exception ex) {
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
            
            // Set isLegacyQuestion to true for template data
            question.setIsLegacyQuestion(true);
            
            // Map OCDS fields
            if (requirement.getOcds() != null) {
                Requirement.OCDS ocds = requirement.getOcds();
                
                if (ocds.getId() != null) {
                    question.setQuestionId(ocds.getId());
                }
                
                if (ocds.getTitle() != null && !ocds.getTitle().trim().isEmpty()) {
                    question.setTitle(ocds.getTitle());
                }
                
                if (ocds.getDescription() != null) {
                    question.setDescription(ocds.getDescription());
                }
                
                if (ocds.getDataType() != null) {
                    question.setDataType(ocds.getDataType());
                }
            }
            
            // Ensure questionId is set (required field)
            if (question.getQuestionId() == null || question.getQuestionId().trim().isEmpty()) {
                question.setQuestionId("Question " + System.currentTimeMillis());
            }
            
            // Ensure title is set (required field)
            if (question.getTitle() == null || question.getTitle().trim().isEmpty()) {
                String defaultTitle = question.getQuestionId();
                if (defaultTitle == null || defaultTitle.trim().isEmpty()) {
                    defaultTitle = "Default Question";
                }
                question.setTitle(defaultTitle);
            }
            
            // Ensure dataType is set (required field)
            if (question.getDataType() == null || question.getDataType().trim().isEmpty()) {
                question.setDataType("string");
            }
            
            // Map nonOCDS fields
            if (requirement.getNonOCDS() != null) {
                Requirement.NonOCDS nonOCDS = requirement.getNonOCDS();
                
                // Set order - default to 0 if missing
                if (nonOCDS.getOrder() != null) {
                    question.setOrder(BigDecimal.valueOf(nonOCDS.getOrder()));
                } else {
                    question.setOrder(BigDecimal.ZERO);
                }
                
                // Set answered - default to false
                if (nonOCDS.getAnswered() != null) {
                    question.setAnswered(nonOCDS.getAnswered());
                } else {
                    question.setAnswered(false);
                }
                
                // Set mandatory - default to false
                if (nonOCDS.getMandatory() != null) {
                    question.setMandatory(nonOCDS.getMandatory());
                } else {
                    question.setMandatory(false);
                }
                
                // Set multiAnswer - default to false
                if (nonOCDS.getMultiAnswer() != null) {
                    question.setMultiAnswer(nonOCDS.getMultiAnswer());
                } else {
                    question.setMultiAnswer(false);
                }
                
                // Set questionType - default to "Text"
                if (nonOCDS.getQuestionType() != null && !nonOCDS.getQuestionType().trim().isEmpty()) {
                    question.setQuestionType(nonOCDS.getQuestionType());
                } else {
                    question.setQuestionType("Text");
                }
                
                // Map dependency
                if (nonOCDS.getDependency() != null) {
                    question.setDependency(mapDependencyToMap(nonOCDS.getDependency()));
                } else {
                    question.setDependency(new HashMap<>());
                }
            } else {
                // If nonOCDS is missing, set defaults
                question.setOrder(BigDecimal.ZERO);
                question.setAnswered(false);
                question.setMandatory(false);
                question.setMultiAnswer(false);
                question.setQuestionType("Text");
                question.setDependency(new HashMap<>());
            }
            
            return question;
        } catch (Exception ex) {
            rollbar.warning("Error mapping Requirement to Question: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Maps Dependency object to Map<String, Object> for JSON serialization
     */
    private Map<String, Object> mapDependencyToMap(Dependency dependency) {
        try {
            if (dependency == null) {
                return new HashMap<>();
            }
            
            Map<String, Object> dependencyMap = new HashMap<>();
            
            if (dependency.getConditional() != null) {
                Map<String, Object> conditionalMap = new HashMap<>();
                conditionalMap.put("dependentOnID", dependency.getConditional().getDependentOnID());
                conditionalMap.put("dependencyType", dependency.getConditional().getDependencyType() != null 
                    ? dependency.getConditional().getDependencyType().getValue() : null);
                conditionalMap.put("dependencyValue", dependency.getConditional().getDependencyValue());
                dependencyMap.put("conditional", conditionalMap);
            }
            
            if (dependency.getRelationships() != null && !dependency.getRelationships().isEmpty()) {
                List<Map<String, Object>> relationshipsList = new ArrayList<>();
                for (Relationships rel : dependency.getRelationships()) {
                    Map<String, Object> relMap = new HashMap<>();
                    relMap.put("dependentOnID", rel.getDependentOnID());
                    relMap.put("relationshipType", rel.getRelationshipType());
                    relationshipsList.add(relMap);
                }
                dependencyMap.put("relationships", relationshipsList);
            }
            
            return dependencyMap;
        } catch (Exception ex) {
            rollbar.warning("Error mapping Dependency to Map: " + ex.getMessage());
            return new HashMap<>();
        }
    }
}

