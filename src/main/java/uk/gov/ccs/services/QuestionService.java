package uk.gov.ccs.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.repo.QuestionRepository;
import uk.gov.ccs.dts.qas.model.generated.Criterion;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.dts.qas.model.generated.QuestionGroup;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.dts.qas.model.generated.QuestionWriteResponse;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service to handle question-related business logic
 */
@Service
public class QuestionService {
    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private Rollbar rollbar;

    @Autowired
    private AgreementServiceClient agreementServiceClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get all questions
     */
    public List<Questions> getAllQuestions() {
        try {
            return questionRepository.findAll();
        } catch (Exception ex) {
            rollbar.error(ex, "Error fetching all questions");
            throw ex;
        }
    }

    /**
     * Get a question by ID
     */
    public Optional<Questions> getQuestionById(Long id) {
        try {
            return questionRepository.findById(id);
        } catch (Exception ex) {
            rollbar.error(ex, "Error fetching question with ID: " + id);
            throw ex;
        }
    }

    /**
     * Save questions from the provided payload directly to the database.
     * 
     * Business rules:
     * - If question data passed, DB should be updated directly with that data (be it edits or new questions)
     * - If question data passed but incomplete (e.g. missing questions that are in the DB), 
     *   those questions should be unaffected by the operation
     * - If question data passed which is the same as existing data, data should be reflected (no unnecessary updates)
     * - If question data passed which is invalid (e.g. required fields having no or null values), 
     *   no updates should be made and endpoint should return error message
     */
    @Transactional
    public QuestionWriteResponse saveQuestionsFromPayload(QuestionWrite questionWrite) {
        try {
            // Validate all required fields before making any changes
            validateQuestionData(questionWrite);

            // Get existing questions for this event ID (to preserve ones not in payload)
            List<Questions> existingQuestions = questionRepository.findByEventId(questionWrite.getEventId());
            
            // Create a map of existing questions by questionId for quick lookup
            java.util.Map<String, Questions> existingQuestionsMap = new java.util.HashMap<>();
            for (Questions existing : existingQuestions) {
                existingQuestionsMap.put(existing.getQuestionId(), existing);
            }

            List<Questions> savedQuestions = new ArrayList<>();
            Timestamp now = new Timestamp(System.currentTimeMillis());
            java.util.Set<String> processedQuestionIds = new java.util.HashSet<>();

            // Iterate through each criterion
            for (Criterion criterion : questionWrite.getCriterion()) {
                // Iterate through each question group in the criterion
                for (QuestionGroup group : criterion.getRequirementGroups()) {
                    // Iterate through each question in the group
                    for (Question question : group.getRequirements()) {
                        // Check if this question already exists
                        Questions existingQuestion = existingQuestionsMap.get(question.getQuestionId());
                        
                        Questions questionEntity;
                        if (existingQuestion != null) {
                            // Update existing question - preserve ID and createdAt
                            questionEntity = updateQuestionEntity(
                                existingQuestion,
                                questionWrite.getEventId(),
                                questionWrite.getAgreementId(),
                                questionWrite.getLotId(),
                                criterion,
                                group,
                                question,
                                now
                            );
                        } else {
                            // Create new question
                            questionEntity = mapToQuestionEntity(
                                questionWrite.getEventId(),
                                questionWrite.getAgreementId(),
                                questionWrite.getLotId(),
                                criterion,
                                group,
                                question,
                                now
                            );
                        }
                        
                        // Only save if data has changed (to avoid unnecessary updates)
                        if (existingQuestion == null || hasDataChanged(existingQuestion, questionEntity)) {
                            savedQuestions.add(questionRepository.save(questionEntity));
                        } else {
                            savedQuestions.add(existingQuestion);
                        }
                        
                        processedQuestionIds.add(question.getQuestionId());
                    }
                }
            }

            // Note: Questions not in the payload remain unchanged (existingQuestions not in processedQuestionIds)
            // This satisfies: "If question data passed but incomplete, those questions should be unaffected"

            // Return response
            QuestionWriteResponse response = new QuestionWriteResponse();
            response.setEventId(questionWrite.getEventId());
            response.setAgreementId(questionWrite.getAgreementId());
            response.setLotId(questionWrite.getLotId());
            if (!savedQuestions.isEmpty()) {
                response.setId(savedQuestions.get(0).getId().longValue());
            }

            return response;
        } catch (IllegalArgumentException ex) {
            // Re-throw validation errors
            throw ex;
        } catch (Exception ex) {
            rollbar.error(ex, "Error saving questions from payload for eventId: " + questionWrite.getEventId());
            throw ex;
        }
    }

    /**
     * Validates that all required fields are present and not null/empty
     */
    private void validateQuestionData(QuestionWrite questionWrite) {
        if (questionWrite.getCriterion() == null || questionWrite.getCriterion().isEmpty()) {
            return; // Empty criterion list is allowed (will fetch from template)
        }

        for (Criterion criterion : questionWrite.getCriterion()) {
            // Validate criterion required fields
            if (criterion.getCriteriaId() == null || criterion.getCriteriaId().trim().isEmpty()) {
                throw new IllegalArgumentException("Criterion criteriaId is required and cannot be null or empty");
            }
            if (criterion.getTitle() == null || criterion.getTitle().trim().isEmpty()) {
                throw new IllegalArgumentException("Criterion title is required and cannot be null or empty");
            }

            if (criterion.getRequirementGroups() == null || criterion.getRequirementGroups().isEmpty()) {
                continue; // Empty groups are allowed
            }

            for (QuestionGroup group : criterion.getRequirementGroups()) {
                // Validate group required fields
                if (group.getGroupId() == null || group.getGroupId().trim().isEmpty()) {
                    throw new IllegalArgumentException("QuestionGroup groupId is required and cannot be null or empty");
                }
                if (group.getTask() == null || group.getTask().trim().isEmpty()) {
                    throw new IllegalArgumentException("QuestionGroup task is required and cannot be null or empty");
                }
                if (group.getOrder() == null) {
                    throw new IllegalArgumentException("QuestionGroup order is required and cannot be null");
                }
                if (group.getMandatory() == null) {
                    throw new IllegalArgumentException("QuestionGroup mandatory is required and cannot be null");
                }

                if (group.getRequirements() == null || group.getRequirements().isEmpty()) {
                    continue; // Empty questions are allowed
                }

                for (Question question : group.getRequirements()) {
                    // Validate question required fields
                    if (question.getQuestionId() == null || question.getQuestionId().trim().isEmpty()) {
                        throw new IllegalArgumentException("Question questionId is required and cannot be null or empty");
                    }
                    if (question.getTitle() == null || question.getTitle().trim().isEmpty()) {
                        throw new IllegalArgumentException("Question title is required and cannot be null or empty");
                    }
                    if (question.getDataType() == null || question.getDataType().trim().isEmpty()) {
                        throw new IllegalArgumentException("Question dataType is required and cannot be null or empty");
                    }
                    if (question.getOrder() == null) {
                        throw new IllegalArgumentException("Question order is required and cannot be null");
                    }
                    if (question.getAnswered() == null) {
                        throw new IllegalArgumentException("Question answered is required and cannot be null");
                    }
                    if (question.getMandatory() == null) {
                        throw new IllegalArgumentException("Question mandatory is required and cannot be null");
                    }
                    if (question.getMultiAnswer() == null) {
                        throw new IllegalArgumentException("Question multiAnswer is required and cannot be null");
                    }
                    if (question.getQuestionType() == null || question.getQuestionType().trim().isEmpty()) {
                        throw new IllegalArgumentException("Question questionType is required and cannot be null or empty");
                    }
                    if (question.getIsLegacyQuestion() == null) {
                        throw new IllegalArgumentException("Question isLegacyQuestion is required and cannot be null");
                    }
                }
            }
        }
    }

    /**
     * Updates an existing question entity with new data
     */
    private Questions updateQuestionEntity(
            Questions existing,
            String eventId, String agreementId, String lotId,
            Criterion criterion, QuestionGroup group, Question question,
            Timestamp now) {
        
        // Update all fields except ID and createdAt
        existing.setEventId(eventId);
        existing.setAgreementId(agreementId);
        existing.setLotId(lotId);
        existing.setCriteriaId(criterion.getCriteriaId());
        existing.setCriterionTitle(criterion.getTitle());
        existing.setGroupId(group.getGroupId());
        existing.setGroupDescription(group.getDescription());
        existing.setGroupTask(group.getTask());
        existing.setGroupOrder(group.getOrder() != null ? group.getOrder().intValue() : null);
        existing.setGroupPrompt(group.getPrompt());
        existing.setGroupMandatory(group.getMandatory());
        existing.setQuestionId(question.getQuestionId());
        existing.setQuestionTitle(question.getTitle());
        existing.setQuestionDescription(question.getDescription());
        existing.setQuestionDataType(question.getDataType());
        existing.setQuestionOrder(question.getOrder() != null ? question.getOrder().intValue() : null);
        existing.setQuestionAnswered(question.getAnswered() != null ? question.getAnswered() : false);
        existing.setQuestionMandatory(question.getMandatory());
        existing.setQuestionMultiAnswer(question.getMultiAnswer());
        existing.setQuestionType(question.getQuestionType());
        existing.setIsLegacyQuestion(question.getIsLegacyQuestion() != null ? question.getIsLegacyQuestion() : false);
        existing.setUpdatedAt(now);

        // Handle dependency JSONB field
        if (question.getDependency() != null && !question.getDependency().isEmpty()) {
            try {
                existing.setQuestionDependency(objectMapper.writeValueAsString(question.getDependency()));
            } catch (JsonProcessingException e) {
                rollbar.warning("Error serializing question dependency to JSON: " + e.getMessage());
            }
        } else {
            existing.setQuestionDependency(null);
        }

        return existing;
    }

    /**
     * Checks if the question data has changed compared to existing data
     */
    private boolean hasDataChanged(Questions existing, Questions updated) {
        return !java.util.Objects.equals(existing.getEventId(), updated.getEventId()) ||
               !java.util.Objects.equals(existing.getAgreementId(), updated.getAgreementId()) ||
               !java.util.Objects.equals(existing.getLotId(), updated.getLotId()) ||
               !java.util.Objects.equals(existing.getCriteriaId(), updated.getCriteriaId()) ||
               !java.util.Objects.equals(existing.getCriterionTitle(), updated.getCriterionTitle()) ||
               !java.util.Objects.equals(existing.getGroupId(), updated.getGroupId()) ||
               !java.util.Objects.equals(existing.getGroupDescription(), updated.getGroupDescription()) ||
               !java.util.Objects.equals(existing.getGroupTask(), updated.getGroupTask()) ||
               !java.util.Objects.equals(existing.getGroupOrder(), updated.getGroupOrder()) ||
               !java.util.Objects.equals(existing.getGroupPrompt(), updated.getGroupPrompt()) ||
               !java.util.Objects.equals(existing.getGroupMandatory(), updated.getGroupMandatory()) ||
               !java.util.Objects.equals(existing.getQuestionId(), updated.getQuestionId()) ||
               !java.util.Objects.equals(existing.getQuestionTitle(), updated.getQuestionTitle()) ||
               !java.util.Objects.equals(existing.getQuestionDescription(), updated.getQuestionDescription()) ||
               !java.util.Objects.equals(existing.getQuestionDataType(), updated.getQuestionDataType()) ||
               !java.util.Objects.equals(existing.getQuestionOrder(), updated.getQuestionOrder()) ||
               !java.util.Objects.equals(existing.getQuestionAnswered(), updated.getQuestionAnswered()) ||
               !java.util.Objects.equals(existing.getQuestionMandatory(), updated.getQuestionMandatory()) ||
               !java.util.Objects.equals(existing.getQuestionDependency(), updated.getQuestionDependency()) ||
               !java.util.Objects.equals(existing.getQuestionMultiAnswer(), updated.getQuestionMultiAnswer()) ||
               !java.util.Objects.equals(existing.getQuestionType(), updated.getQuestionType()) ||
               !java.util.Objects.equals(existing.getIsLegacyQuestion(), updated.getIsLegacyQuestion());
    }

    /**
     * Fetch template data from agreement service and save as questions.
     * 
     * Business rules:
     * - If Agreement and Lot ID have template data, and no question data passed, 
     *   template data should be stored against the event
     * - If Agreement and Lot ID have no template data, and no question data passed, 
     *   no data changes should be made
     * 
     * @param eventId The event ID
     * @param agreementId The agreement ID
     * @param lotId The lot ID
     * @param eventType The event type (e.g., "FC" for Further Competition)
     * @return QuestionWriteResponse if template data was found and saved, null if no template data exists
     */
    @Transactional
    public QuestionWriteResponse saveQuestionsFromTemplate(
            String eventId, String agreementId, String lotId, String eventType) {
        try {
            // Fetch template data from agreement service
            QuestionWrite templateData = agreementServiceClient.fetchTemplateData(agreementId, lotId, eventType);
            
            // If no template data exists, return null (no data changes should be made)
            if (templateData == null || templateData.getCriterion() == null || templateData.getCriterion().isEmpty()) {
                return null;
            }

            // Set the event ID from the request
            templateData.setEventId(eventId);
            templateData.setAgreementId(agreementId);
            templateData.setLotId(lotId);

            // Save the template data as questions
            return saveQuestionsFromPayload(templateData);
        } catch (Exception ex) {
            rollbar.error(ex, "Error saving questions from template for eventId: " + eventId);
            throw ex;
        }
    }

    /**
     * Maps API model (Question) to entity (Questions)
     */
    private Questions mapToQuestionEntity(
            String eventId, String agreementId, String lotId,
            Criterion criterion, QuestionGroup group, Question question,
            Timestamp now) {
        
        Questions entity = Questions.builder()
            .eventId(eventId)
            .agreementId(agreementId)
            .lotId(lotId)
            .criteriaId(criterion.getCriteriaId())
            .criterionTitle(criterion.getTitle())
            .groupId(group.getGroupId())
            .groupDescription(group.getDescription())
            .groupTask(group.getTask())
            .groupOrder(group.getOrder() != null ? group.getOrder().intValue() : null)
            .groupPrompt(group.getPrompt())
            .groupMandatory(group.getMandatory())
            .questionId(question.getQuestionId())
            .questionTitle(question.getTitle())
            .questionDescription(question.getDescription())
            .questionDataType(question.getDataType())
            .questionOrder(question.getOrder() != null ? question.getOrder().intValue() : null)
            .questionAnswered(question.getAnswered() != null ? question.getAnswered() : false)
            .questionMandatory(question.getMandatory())
            .questionMultiAnswer(question.getMultiAnswer())
            .questionType(question.getQuestionType())
            .isLegacyQuestion(question.getIsLegacyQuestion() != null ? question.getIsLegacyQuestion() : false)
            .createdAt(now)
            .updatedAt(now)
            .build();

        // Handle dependency JSONB field
        if (question.getDependency() != null && !question.getDependency().isEmpty()) {
            try {
                entity.setQuestionDependency(objectMapper.writeValueAsString(question.getDependency()));
            } catch (JsonProcessingException e) {
                rollbar.warning("Error serializing question dependency to JSON: " + e.getMessage());
            }
        }

        return entity;
    }
    
    /**
     * Returns list of question details based on eventId
     * @param eventId
     * @return {@link List<Question>}
     *
     */
    // Not sure if we are caching yet
    //@Cacheable(value = "qAndACache", key = "#root.methodName")
    public List<Question> getQuestionsWithEventId(final String eventId) {

        return questionRepository
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(eventId)
                .stream()
                .map(this::mapQuestionEntityWithApiResponse)
                .collect(Collectors.toList());
    }

    private Question mapQuestionEntityWithApiResponse(Questions q) {

        return new Question(q.getQuestionId(),
                q.getQuestionTitle(),
                q.getQuestionDataType(),
                BigDecimal.valueOf(q.getQuestionOrder()),
                q.getQuestionAnswered(),
                q.getQuestionMandatory(),
                q.getQuestionMultiAnswer(),
                q.getQuestionType(),
                q.getIsLegacyQuestion());
    }
}

