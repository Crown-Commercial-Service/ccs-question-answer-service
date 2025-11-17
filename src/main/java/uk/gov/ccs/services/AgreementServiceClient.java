package uk.gov.ccs.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import uk.gov.ccs.dts.qas.model.generated.Criterion;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.dts.qas.model.generated.QuestionGroup;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client to interact with the Agreement Service to fetch template data
 */
@Service
public class AgreementServiceClient {
    
    @Autowired
    private Rollbar rollbar;

    @Value("${external-services.agreements-service.base-path}")
    private String agreementServiceBasePath;

    @Value("${external-services.agreements-service.api-key:}")
    private String agreementServiceApiKey;

    @Value("${external-services.agreements-service.data-templates-path}")
    private String dataTemplatesPath;

    @Autowired
    private ObjectMapper objectMapper;

    private RestClient restClient;

    /**
     * Initialize RestClient with API key header if configured
     */
    private RestClient getRestClient() {
        if (restClient == null) {
            RestClient.Builder builder = RestClient.builder();
            
            // Add API key header if configured
            if (agreementServiceApiKey != null && !agreementServiceApiKey.trim().isEmpty()) {
                builder = builder.defaultHeader("x-api-key", agreementServiceApiKey);
            }
            
            restClient = builder.build();
        }
        return restClient;
    }

    /**
     * Fetches template data from the agreement service for a given agreement, lot, and event type.
     * Uses the configured path pattern from application.yml
     * 
     * @param agreementId The agreement ID
     * @param lotId The lot ID
     * @param eventType The event type (e.g., "FC" for Further Competition)
     * @return QuestionWrite object containing the template data, or null if not found
     */
    public QuestionWrite fetchTemplateData(String agreementId, String lotId, String eventType) {
        try {
            // Build URL using the configured path pattern
            String path = dataTemplatesPath
                    .replace("{agreement-id}", agreementId)
                    .replace("{lot-id}", lotId)
                    .replace("{event-type}", eventType);
            
            String url = agreementServiceBasePath + path;
            
            ResponseEntity<List> response = getRestClient().get()
                    .uri(url)
                    .retrieve()
                    .toEntity(List.class);

            HttpStatusCode statusCode = response.getStatusCode();
            
            // If 404 or empty response, no template data exists
            if (statusCode.value() == 404 || response.getBody() == null || response.getBody().isEmpty()) {
                return null;
            }

            // If successful, parse the response
            if (statusCode.is2xxSuccessful() && response.getBody() != null) {
                return mapToQuestionWrite(agreementId, lotId, response.getBody());
            }
            
            return null;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
            // 404 means no template data exists - this is expected and not an error
            return null;
        } catch (Exception ex) {
            rollbar.warning("Error fetching template data from agreement service: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Maps the agreement service response (List of ProcurementDataTemplate) to QuestionWrite format.
     * Extracts all required fields from the nested template JSON structure.
     */
    @SuppressWarnings("unchecked")
    private QuestionWrite mapToQuestionWrite(String agreementId, String lotId, List<Map<String, Object>> dataTemplates) {
        try {
            QuestionWrite questionWrite = new QuestionWrite();
            questionWrite.setAgreementId(agreementId);
            questionWrite.setLotId(lotId);
            questionWrite.setCriterion(new ArrayList<>());

            // Each ProcurementDataTemplate has a "criteria" field that contains the question structure
            for (Map<String, Object> template : dataTemplates) {
                Object criteriaObj = template.get("criteria");
                
                if (criteriaObj != null) {
                    if (criteriaObj instanceof List) {
                        // If criteria is a list, parse each criterion
                        List<Map<String, Object>> criteriaList = (List<Map<String, Object>>) criteriaObj;
                        for (Map<String, Object> criteriaMap : criteriaList) {
                            Criterion criterion = parseCriterion(criteriaMap);
                            if (criterion != null) {
                                questionWrite.getCriterion().add(criterion);
                            }
                        }
                    } else if (criteriaObj instanceof Map) {
                        // If criteria is a single object, parse it
                        Map<String, Object> criteriaMap = (Map<String, Object>) criteriaObj;
                        Criterion criterion = parseCriterion(criteriaMap);
                        if (criterion != null) {
                            questionWrite.getCriterion().add(criterion);
                        }
                    }
                }
            }

            // If no criteria found, return null to indicate no template data
            if (questionWrite.getCriterion().isEmpty()) {
                return null;
            }

            return questionWrite;
        } catch (Exception ex) {
            rollbar.error(ex, "Error mapping template data to QuestionWrite format");
            return null;
        }
    }

    /**
     * Parse a single criterion map into a Criterion object.
     * Extracts: criteriaId, criterionTitle, and requirementGroups.
     */
    @SuppressWarnings("unchecked")
    private Criterion parseCriterion(Map<String, Object> criteriaMap) {
        try {
            Criterion criterion = new Criterion();
            
            // Extract criteriaId and title
            Object criteriaIdObj = criteriaMap.get("id");
            if (criteriaIdObj != null) {
                criterion.setCriteriaId(String.valueOf(criteriaIdObj));
            }
            
            Object titleObj = criteriaMap.get("title");
            if (titleObj != null) {
                criterion.setTitle(String.valueOf(titleObj));
            }
            
            // Parse requirementGroups
            Object requirementGroupsObj = criteriaMap.get("requirementGroups");
            if (requirementGroupsObj instanceof List) {
                List<Map<String, Object>> requirementGroupsList = (List<Map<String, Object>>) requirementGroupsObj;
                List<QuestionGroup> questionGroups = new ArrayList<>();
                
                for (Map<String, Object> groupMap : requirementGroupsList) {
                    QuestionGroup questionGroup = parseQuestionGroup(groupMap);
                    if (questionGroup != null) {
                        questionGroups.add(questionGroup);
                    }
                }
                
                criterion.setRequirementGroups(questionGroups);
            }
            
            return criterion;
        } catch (Exception ex) {
            rollbar.warning("Error parsing criterion: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Parse a requirementGroup map into a QuestionGroup object.
     * Extracts: groupId (from OCDS.id), description (from OCDS.description), 
     * task (from nonOCDS.task), order (from nonOCDS.order), 
     * prompt (from nonOCDS.prompt), mandatory (from nonOCDS.mandatory), 
     * and requirements (from OCDS.requirements).
     */
    @SuppressWarnings("unchecked")
    private QuestionGroup parseQuestionGroup(Map<String, Object> groupMap) {
        try {
            QuestionGroup questionGroup = new QuestionGroup();
            
            // Extract OCDS fields
            Object ocdsObj = groupMap.get("OCDS");
            if (ocdsObj instanceof Map) {
                Map<String, Object> ocdsMap = (Map<String, Object>) ocdsObj;
                
                // Extract groupId from OCDS.id
                Object groupIdObj = ocdsMap.get("id");
                if (groupIdObj != null) {
                    questionGroup.setGroupId(String.valueOf(groupIdObj));
                }
                
                // Extract description from OCDS.description
                Object descriptionObj = ocdsMap.get("description");
                if (descriptionObj != null) {
                    questionGroup.setDescription(String.valueOf(descriptionObj));
                }
                
                // Parse requirements from OCDS.requirements
                Object requirementsObj = ocdsMap.get("requirements");
                if (requirementsObj instanceof List) {
                    List<Map<String, Object>> requirementsList = (List<Map<String, Object>>) requirementsObj;
                    List<Question> questions = new ArrayList<>();
                    
                    for (Map<String, Object> requirementMap : requirementsList) {
                        Question question = parseQuestion(requirementMap);
                        if (question != null) {
                            questions.add(question);
                        }
                    }
                    
                    questionGroup.setRequirements(questions);
                }
            }
            
            // Ensure groupId is set (required field) - use a default if missing
            if (questionGroup.getGroupId() == null || questionGroup.getGroupId().trim().isEmpty()) {
                questionGroup.setGroupId("Group " + System.currentTimeMillis()); // Fallback ID
            }
            
            // Extract nonOCDS fields
            Object nonOCDSObj = groupMap.get("nonOCDS");
            if (nonOCDSObj instanceof Map) {
                Map<String, Object> nonOCDSMap = (Map<String, Object>) nonOCDSObj;
                
                // Extract task - if null or empty, use description or groupId as fallback
                Object taskObj = nonOCDSMap.get("task");
                if (taskObj != null && !String.valueOf(taskObj).trim().isEmpty()) {
                    questionGroup.setTask(String.valueOf(taskObj));
                } else {
                    // Provide default: use description if available, otherwise use groupId
                    String defaultTask = questionGroup.getDescription();
                    if (defaultTask == null || defaultTask.trim().isEmpty()) {
                        defaultTask = questionGroup.getGroupId();
                    }
                    if (defaultTask == null || defaultTask.trim().isEmpty()) {
                        defaultTask = "Default Task"; // Final fallback
                    }
                    questionGroup.setTask(defaultTask);
                }
                
                // Extract order - default to 0 if missing
                Object orderObj = nonOCDSMap.get("order");
                if (orderObj != null) {
                    if (orderObj instanceof Number) {
                        questionGroup.setOrder(java.math.BigDecimal.valueOf(((Number) orderObj).doubleValue()));
                    } else {
                        try {
                            questionGroup.setOrder(new java.math.BigDecimal(String.valueOf(orderObj)));
                        } catch (Exception e) {
                            questionGroup.setOrder(java.math.BigDecimal.ZERO); // Default to 0
                        }
                    }
                } else {
                    questionGroup.setOrder(java.math.BigDecimal.ZERO); // Default to 0 if missing
                }
                
                // Extract prompt
                Object promptObj = nonOCDSMap.get("prompt");
                if (promptObj != null) {
                    questionGroup.setPrompt(String.valueOf(promptObj));
                }
                
                // Extract mandatory - default to false if missing
                Object mandatoryObj = nonOCDSMap.get("mandatory");
                if (mandatoryObj != null) {
                    if (mandatoryObj instanceof Boolean) {
                        questionGroup.setMandatory((Boolean) mandatoryObj);
                    } else {
                        questionGroup.setMandatory(Boolean.parseBoolean(String.valueOf(mandatoryObj)));
                    }
                } else {
                    questionGroup.setMandatory(false); // Default to false if missing
                }
            } else {
                // If nonOCDS is missing entirely, set required fields with defaults
                String defaultTask = questionGroup.getDescription();
                if (defaultTask == null || defaultTask.trim().isEmpty()) {
                    defaultTask = questionGroup.getGroupId();
                }
                if (defaultTask == null || defaultTask.trim().isEmpty()) {
                    defaultTask = "Default Task";
                }
                questionGroup.setTask(defaultTask);
                questionGroup.setOrder(java.math.BigDecimal.ZERO);
                questionGroup.setMandatory(false);
            }
            
            return questionGroup;
        } catch (Exception ex) {
            rollbar.warning("Error parsing question group: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Parse a requirement (question) map into a Question object.
     * Extracts: questionId (from OCDS.id), title (from OCDS.title), 
     * description (from OCDS.description), dataType (from OCDS.dataType),
     * order (from nonOCDS.order), answered (from nonOCDS.answered),
     * mandatory (from nonOCDS.mandatory), multiAnswer (from nonOCDS.multiAnswer),
     * questionType (from nonOCDS.questionType), dependency (from nonOCDS.dependency),
     * and isLegacyQuestion (set to true for template data).
     */
    @SuppressWarnings("unchecked")
    private Question parseQuestion(Map<String, Object> requirementMap) {
        try {
            Question question = new Question();
            
            // Set isLegacyQuestion to true for template data
            question.setIsLegacyQuestion(true);
            
            // Extract OCDS fields
            Object ocdsObj = requirementMap.get("OCDS");
            if (ocdsObj instanceof Map) {
                Map<String, Object> ocdsMap = (Map<String, Object>) ocdsObj;
                
                // Extract questionId from OCDS.id
                Object questionIdObj = ocdsMap.get("id");
                if (questionIdObj != null) {
                    question.setQuestionId(String.valueOf(questionIdObj));
                }
                
                // Extract title from OCDS.title
                Object titleObj = ocdsMap.get("title");
                if (titleObj != null && !String.valueOf(titleObj).trim().isEmpty()) {
                    question.setTitle(String.valueOf(titleObj));
                }
                
                // Extract description from OCDS.description
                Object descriptionObj = ocdsMap.get("description");
                if (descriptionObj != null) {
                    question.setDescription(String.valueOf(descriptionObj));
                }
                
                // Extract dataType from OCDS.dataType
                Object dataTypeObj = ocdsMap.get("dataType");
                if (dataTypeObj != null) {
                    question.setDataType(String.valueOf(dataTypeObj));
                }
            }
            
            // Ensure questionId is set (required field) - use a default if missing
            if (question.getQuestionId() == null || question.getQuestionId().trim().isEmpty()) {
                question.setQuestionId("Question " + System.currentTimeMillis()); // Fallback ID
            }
            
            // Ensure title is set (required field) - use questionId or default if missing
            if (question.getTitle() == null || question.getTitle().trim().isEmpty()) {
                String defaultTitle = question.getQuestionId();
                if (defaultTitle == null || defaultTitle.trim().isEmpty()) {
                    defaultTitle = "Default Question";
                }
                question.setTitle(defaultTitle);
            }
            
            // Ensure dataType is set (required field) - default to "string" if missing
            if (question.getDataType() == null || question.getDataType().trim().isEmpty()) {
                question.setDataType("string"); // Default data type
            }
            
            // Extract nonOCDS fields
            Object nonOCDSObj = requirementMap.get("nonOCDS");
            if (nonOCDSObj instanceof Map) {
                Map<String, Object> nonOCDSMap = (Map<String, Object>) nonOCDSObj;
                
                // Extract order - default to 0 if missing
                Object orderObj = nonOCDSMap.get("order");
                if (orderObj != null) {
                    if (orderObj instanceof Number) {
                        question.setOrder(java.math.BigDecimal.valueOf(((Number) orderObj).doubleValue()));
                    } else {
                        try {
                            question.setOrder(new java.math.BigDecimal(String.valueOf(orderObj)));
                        } catch (Exception e) {
                            question.setOrder(java.math.BigDecimal.ZERO); // Default to 0
                        }
                    }
                } else {
                    question.setOrder(java.math.BigDecimal.ZERO); // Default to 0 if missing
                }
                
                // Extract answered
                Object answeredObj = nonOCDSMap.get("answered");
                if (answeredObj != null) {
                    if (answeredObj instanceof Boolean) {
                        question.setAnswered((Boolean) answeredObj);
                    } else {
                        question.setAnswered(Boolean.parseBoolean(String.valueOf(answeredObj)));
                    }
                } else {
                    question.setAnswered(false); // Default to false
                }
                
                // Extract mandatory - default to false if missing
                Object mandatoryObj = nonOCDSMap.get("mandatory");
                if (mandatoryObj != null) {
                    if (mandatoryObj instanceof Boolean) {
                        question.setMandatory((Boolean) mandatoryObj);
                    } else {
                        question.setMandatory(Boolean.parseBoolean(String.valueOf(mandatoryObj)));
                    }
                } else {
                    question.setMandatory(false); // Default to false if missing
                }
                
                // Extract multiAnswer
                Object multiAnswerObj = nonOCDSMap.get("multiAnswer");
                if (multiAnswerObj != null) {
                    if (multiAnswerObj instanceof Boolean) {
                        question.setMultiAnswer((Boolean) multiAnswerObj);
                    } else {
                        question.setMultiAnswer(Boolean.parseBoolean(String.valueOf(multiAnswerObj)));
                    }
                } else {
                    question.setMultiAnswer(false); // Default to false
                }
                
                // Extract questionType - default to "Text" if missing
                Object questionTypeObj = nonOCDSMap.get("questionType");
                if (questionTypeObj != null && !String.valueOf(questionTypeObj).trim().isEmpty()) {
                    question.setQuestionType(String.valueOf(questionTypeObj));
                } else {
                    question.setQuestionType("Text"); // Default question type
                }
                
                // Extract dependency (as Map<String, Object>)
                Object dependencyObj = nonOCDSMap.get("dependency");
                if (dependencyObj != null) {
                    if (dependencyObj instanceof Map) {
                        question.setDependency((Map<String, Object>) dependencyObj);
                    } else {
                        // Try to convert to Map
                        try {
                            String json = objectMapper.writeValueAsString(dependencyObj);
                            Map<String, Object> dependencyMap = objectMapper.readValue(json, Map.class);
                            question.setDependency(dependencyMap);
                        } catch (Exception e) {
                            // If conversion fails, create empty map
                            question.setDependency(new java.util.HashMap<>());
                        }
                    }
                } else {
                    question.setDependency(new java.util.HashMap<>());
                }
            } else {
                // If nonOCDS is missing entirely, set required fields with defaults
                if (question.getOrder() == null) {
                    question.setOrder(java.math.BigDecimal.ZERO);
                }
                if (question.getAnswered() == null) {
                    question.setAnswered(false);
                }
                if (question.getMandatory() == null) {
                    question.setMandatory(false);
                }
                if (question.getMultiAnswer() == null) {
                    question.setMultiAnswer(false);
                }
                if (question.getQuestionType() == null || question.getQuestionType().trim().isEmpty()) {
                    question.setQuestionType("Text");
                }
                if (question.getDependency() == null) {
                    question.setDependency(new java.util.HashMap<>());
                }
            }
            
            // Final validation: ensure all required fields are set
            if (question.getOrder() == null) {
                question.setOrder(java.math.BigDecimal.ZERO);
            }
            if (question.getAnswered() == null) {
                question.setAnswered(false);
            }
            if (question.getMandatory() == null) {
                question.setMandatory(false);
            }
            if (question.getMultiAnswer() == null) {
                question.setMultiAnswer(false);
            }
            if (question.getQuestionType() == null || question.getQuestionType().trim().isEmpty()) {
                question.setQuestionType("Text");
            }
            if (question.getDependency() == null) {
                question.setDependency(new java.util.HashMap<>());
            }
            
            return question;
        } catch (Exception ex) {
            rollbar.warning("Error parsing question: " + ex.getMessage());
            return null;
        }
    }
}


