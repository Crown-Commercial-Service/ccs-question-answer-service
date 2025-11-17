package uk.gov.ccs.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ccs.BLL.QuestionLogicClient;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.services.QuestionService;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.dts.qas.model.generated.QuestionWriteResponse;

import java.util.List;
import java.util.Optional;

/**
 * QuestionController to handle question related CRUD operations.
 */
@RestController
@RequestMapping(path = "/questions", produces = MediaType.APPLICATION_JSON_VALUE)
public class QuestionController extends BaseController {

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionLogicClient questionLogicClient;

    @GetMapping("/{eventID}")
    public ResponseEntity<List<Question>> getQuestions(@PathVariable("eventID") final String eventId) {

        return ResponseEntity
                .ok(questionService.getQuestionsWithEventId(eventId));
    }


    /**
     * GET endpoint to retrieve all questions
     * 
     * Usage: GET http://localhost:4000/questions
     */
    @GetMapping
    public ResponseEntity<List<Questions>> getAllQuestions() {
        log.info("GET /questions - Retrieving all questions");
        try {
            List<Questions> questions = questionService.getAllQuestions();
            return ResponseEntity.ok(questions);
        } catch (Exception ex) {
            log.error("Error retrieving questions", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



//    @PostMapping
//    public ResponseEntity<QuestionWriteResponse> createQuestions()  {
//
//        log.error("Error retrieving question with ID: {}");
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//
//
//    }

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
        
        log.info("POST /questions - Request received. questionWrite: {}, eventType: {}", 
                questionWrite != null ? "present" : "null", eventType);
        
        // Check if request body is present
        if (questionWrite == null) {
            log.warn("POST /questions - Request body is required");
            return ResponseEntity.badRequest().build();
        }
        
        log.info("POST /questions - Creating questions for eventId: {}, agreementId: {}, lotId: {}, eventType: {}",
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
                log.info("POST /questions - No template data found for agreement: {}, lot: {}, eventType: {}. No changes made.",
                        questionWrite.getAgreementId(), questionWrite.getLotId(), eventType);
                return ResponseEntity.noContent().build();
            }

            log.info("POST /questions - Successfully created/updated questions for eventId: {}", questionWrite.getEventId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {
            log.warn("POST /questions - Validation error: {}", ex.getMessage());
            // Return error message in response body
            return ResponseEntity.badRequest().build();
        } catch (Exception ex) {
            log.error("POST /questions - Error creating questions", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
