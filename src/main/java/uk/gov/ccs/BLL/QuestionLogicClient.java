package uk.gov.ccs.BLL;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.dts.qas.model.generated.QuestionWriteResponse;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.services.QuestionService;
import uk.gov.ccs.services.TemplateDataService;

import java.util.List;

/**
 * Business Logic Layer for question-related operations.
 * Serves as an intermediary between Controllers and Services.
 */
@Component
public class QuestionLogicClient {
    @Autowired
    private QuestionService questionService;

    @Autowired
    private TemplateDataService templateDataService;

    @Autowired
    private Rollbar rollbar;

    /**
     * Creates or updates questions based on the provided QuestionWrite payload.
     * 
     * Business rules:
     * - If question data passed, DB should be updated directly with that data
     * - If no question data passed, fetch template data for Agreement/Lot, and store under Event ID
     * - If Agreement and Lot ID have no template data, and no question data passed, no data changes should be made
     * 
     * @param questionWrite The question write payload containing eventId, agreementId, lotId, and optional criterion data
     * @param eventType The event type (e.g., "FC" for Further Competition) - required when fetching templates
     * @return QuestionWriteResponse with the created/updated question information, or null if no template data exists
     */
    @CacheEvict(value = "questions", allEntries = true)
    public QuestionWriteResponse createOrUpdateQuestions(QuestionWrite questionWrite, String eventType) {
        try {
            // Validate required fields
            if (questionWrite.getEventId() == null || questionWrite.getEventId().trim().isEmpty()) {
                throw new IllegalArgumentException("Event ID is required");
            }
            if (questionWrite.getAgreementId() == null || questionWrite.getAgreementId().trim().isEmpty()) {
                throw new IllegalArgumentException("Agreement ID is required");
            }
            if (questionWrite.getLotId() == null || questionWrite.getLotId().trim().isEmpty()) {
                throw new IllegalArgumentException("Lot ID is required");
            }

            // Check if question data (criterion) is provided
            boolean hasQuestionData = questionWrite.getCriterion() != null && !questionWrite.getCriterion().isEmpty();

            if (hasQuestionData) {
                // If question data passed, insert/update directly into DB against Event ID
                return questionService.saveQuestionsFromPayload(questionWrite);
            } else {
                // If no question data passed, fetch template data for Agreement/Lot, and store under Event ID
                // If no template data exists, returns null (no data changes made)
                if (eventType == null || eventType.trim().isEmpty()) {
                    throw new IllegalArgumentException("Event type is required when fetching template data");
                }
                return questionService.saveQuestionsFromTemplate(
                    questionWrite.getEventId(),
                    questionWrite.getAgreementId(),
                    questionWrite.getLotId(),
                    eventType
                );
            }
        } catch (IllegalArgumentException ex) {
            // Re-throw validation errors
            throw ex;
        } catch (Exception ex) {
            rollbar.error(ex, "Error creating/updating questions for eventId: " + questionWrite.getEventId());
            throw ex;
        }
    }

    /**
     * Retrieve list of questions for an eventId grouped into criteria and question group.
     * @param eventId to retrieve questions for that eventId
     * @return list of questions {@link List<Questions>}
     */
    @Cacheable(value = "qAndACache", key = "#root.methodName + '-' + #eventId")
    public List<Questions> getQuestionsWithEventId(final String eventId) {
        return questionService.getQuestionsWithEventId(eventId);
    }

    /**
     * Delete question/questions match with the eventId and questionId.
     * @param eventId to match
     * @param questionId to match
     * @return 1 is delete successful or 0 if delete fail for non matching eventId
     * or questionId {@link Long}
     */
    @CacheEvict(value = "qAndACache", key = "'getQuestionsWithEventId-' + #eventId")
    public long deleteQuestion(String eventId, String questionId) {
        return questionService.deleteQuestion(eventId, questionId);
    }

    /**
     * Get DataTemplates for agreement, lot, and event type from questions table.
     * Returns DataTemplate in the same format as agreements-service.
     * 
     * @param agreementId Agreement ID (e.g., "RM1043.8")
     * @param lotId Lot ID (e.g., "1")
     * @param eventType Event type (e.g., "FC")
     * @return List of DataTemplate objects (same format as agreements-service)
     */
    @Cacheable(value = "dataTemplatesCache", key = "#agreementId + '-' + #lotId + '-' + #eventType")
    public List<DataTemplate> getEventDataTemplates(
            String agreementId, String lotId, String eventType) {
        try {
            return templateDataService.getEventDataTemplates(agreementId, lotId, eventType);
        } catch (Exception ex) {
            rollbar.error(ex, "Error getting event data templates for agreement: " + agreementId + 
                ", lot: " + lotId + ", eventType: " + eventType);
            throw ex;
        }
    }
}


