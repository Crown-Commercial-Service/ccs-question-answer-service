package uk.gov.ccs.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import uk.gov.ccs.BLL.QuestionLogicClient;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.dts.qas.model.generated.QuestionWriteResponse;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.exceptions.ResourceNotFoundException;
import uk.gov.ccs.model.agreements.DataTemplate;

import java.net.URI;
import java.util.List;

import static uk.gov.ccs.constants.Constants.responses_Success;

/**
 * QuestionController to handle question related CRUD operations.
 */
@RestController
@RequestMapping(path = "/questions", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionController extends BaseController {

    @Autowired
    private QuestionLogicClient questionLogicClient;

    /**
     * Retrieve questions for an eventId grouped into criteria and question group.
     * @param eventId The ID of the event the question belongs to.
     * @return {@link List<Questions>}, standard HTTP response entity (200).
     */
    @GetMapping("/{eventID}")
    public ResponseEntity<List<Questions>> getQuestions(@PathVariable("eventID") final String eventId) {

        log.debug("GET /questions - Retrieving questions with eventId={}", eventId);
        try {
            return ResponseEntity
                    .ok(questionLogicClient.getQuestionsWithEventId(eventId));
        } catch (Exception ex) {
            throw new ResourceNotFoundException("No question details found for this eventId " + eventId);
        }
    }

    /**
     * Return template data.
     * @param agreementId - todo
     * @param lotId - todo
     * @param eventType - todo
     * @param apiKey - todo
     * @return {@link List<DataTemplate>}
     */
    @GetMapping("/{agreement-id}/lots/{lot-id}/event-types/{event-type}/data-templates")
    List<DataTemplate> getEventDataTemplates(@PathVariable("agreement-id") String agreementId, @PathVariable("lot-id") String lotId, @PathVariable("event-type") String eventType, @RequestHeader("x-api-key") String apiKey) {
        //TODO Raja is working on this bit to map data
        log.debug("GET - /questions/{agreement-id}/lots/{lot-id}/event-types/{event-type}/data-templates - Retrieving questions with");
        return null;
    }

    /**
     * POST endpoint to create questions.
     * 
     * Accepts Event ID, Agreement ID, Lot ID, and optional question data.
     * - If question data passed, insert/update directly into DB against Event ID
     * - If no question data passed, fetch "template" data for Agreement / Lot, and store under Event ID
     * - If Agreement and Lot ID have no template data, and no question data passed, no data changes are made
     * 
     * Usage: POST http://localhost:4000/questions?eventType=FC
     * 
     * @param questionWrite The question write payload containing eventId, agreementId, lotId, and optional criterion data
     * @param eventType The event type (e.g., "FC" for Further Competition) - required when no question data is provided
     * @return QuestionWriteResponse with OK status on success, 204 No Content if no template data exists, 400 Bad Request on validation error
     */
    @PostMapping
    public ResponseEntity<QuestionWriteResponse> createQuestions(
            @RequestBody(required = false) QuestionWrite questionWrite,
            @RequestParam(required = false) String eventType) {
        
        log.debug("POST /questions - Request received. questionWrite: {}, eventType: {}",
                questionWrite != null ? "present" : "null", eventType);
        
        // Check if request body is present
        if (questionWrite == null) {
            log.warn("POST /questions - Request body is required");
            return ResponseEntity.badRequest().build();
        }
        
        log.debug("POST /questions - Creating questions for eventId: {}, agreementId: {}, lotId: {}, eventType: {}",
                questionWrite.getEventId(), questionWrite.getAgreementId(), questionWrite.getLotId(), eventType);

        try {
            // Validate required path parameters (eventId, agreementId, lotId)
            if (questionWrite.getEventId() == null || questionWrite.getEventId().trim().isEmpty()) {
                log.warn("POST /questions - Event ID is required");
                return ResponseEntity.badRequest().build();
            }
            if (questionWrite.getAgreementId() == null || questionWrite.getAgreementId().trim().isEmpty()) {
                log.warn("POST /questions - Agreement ID is required");
                return ResponseEntity.badRequest().build();
            }
            if (questionWrite.getLotId() == null || questionWrite.getLotId().trim().isEmpty()) {
                log.warn("POST /questions - Lot ID is required");
                return ResponseEntity.badRequest().build();
            }

            // Process the request through the business logic layer
            QuestionWriteResponse response = questionLogicClient.createOrUpdateQuestions(questionWrite, eventType);

            // If response is null, it means no template data exists and no changes were made
            if (response == null) {
                log.debug("POST /questions - No template data found for agreement: {}, lot: {}, eventType: {}. No changes made.",
                        questionWrite.getAgreementId(), questionWrite.getLotId(), eventType);
                return ResponseEntity.noContent().build();
            }

            log.debug("POST /questions - Successfully created/updated questions for eventId: {}", questionWrite.getEventId());
            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .queryParam("eventType", eventType)
                    .buildAndExpand(eventType)
                    .toUri();
            return ResponseEntity
                    .created(location)
                    .body(response);

        } catch (IllegalArgumentException ex) {
            log.warn("POST /questions - Validation error: {}", ex.getMessage());
            // Return error message in response body
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("POST /questions - Error creating questions", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles DELETE requests to remove a specific question resource for a given event.
     * Returns HTTP 200.
     *
     * @param eventId The ID of the event the question belongs to.
     * @param questionId The unique ID of the question to delete.
     * @return A standard HTTP response entity (200).
     */
    @DeleteMapping("/{eventId}/{questionId}")
    public ResponseEntity<String> deleteQuestion(
            @PathVariable String eventId,
            @PathVariable String questionId) {

        long count = questionLogicClient.deleteQuestion(eventId, questionId);
        if (count == 0) {
            rollbar.error("DELETE /questions failed, no match found for the eventId and questionId."
                    + "eventId=" + eventId + " questionId= " + questionId);
            return ResponseEntity.notFound().build();
        }

        log.debug("DELETE /questions - Successfully deleted the question eventId: {}, " +
                "questionId: {}", eventId, questionId);

        return ResponseEntity.ok(responses_Success);
    }
}
