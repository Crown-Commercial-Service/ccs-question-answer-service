package uk.gov.ccs.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.services.QuestionService;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * QuestionController to handle question related CRUD operations.
 *
 */

@RestController
@RequestMapping(path = "/question", produces = APPLICATION_JSON_VALUE)
public class QuestionController extends BaseController {

    @Autowired
    private QuestionService questionService;


    @GetMapping("/{eventID}")
    public ResponseEntity<List<Question>> getQuestions(@PathVariable("eventID") final String eventId) {

        return ResponseEntity
                .ok(questionService.getQuestionsWithEventId(eventId));
    }

}
