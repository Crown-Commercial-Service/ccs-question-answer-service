package uk.gov.ccs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.ccs.services.QuestionService;
import uk.gov.ccs.BLL.QuestionLogicClient;
import uk.gov.ccs.dts.qas.model.generated.Question;
import com.rollbar.notifier.Rollbar;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(QuestionController.class)
@ActiveProfiles("test")
class QuestionControllerTest {

    private static final String BASE_URL = "/questions";
    private static final String TEST_EVENT_ID = "TEST_12344_OCD";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Rollbar rollbar;

    @MockBean
    private QuestionService questionService;

    @MockBean
    private QuestionLogicClient questionLogicClient;

    @Test
    void getQuestions_shouldReturnListOfQuestions_whenDataIsFound() throws Exception {
        // Arrange
        Question question1 = givenQuestion();
        Question question2 = givenQuestion();

        // Mock the service call to return our list of DTOs
        when(questionService.getQuestionsWithEventId(eq(TEST_EVENT_ID)))
                .thenReturn(List.of(question1, question2));

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{eventID}", TEST_EVENT_ID)
                        .with(user("test"))
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(List.of(question1, question2))));
    }

    @Test
    void getQuestions_shouldReturnEmptyList_whenNoQuestionsAreFound() throws Exception {
        // Arrange
        final String emptyEventId = "EVENT_NO_QUESTIONS";

        // Mock the service call to return an empty list
        when(questionService.getQuestionsWithEventId(eq(emptyEventId)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(get(BASE_URL + "/{eventID}", emptyEventId)
                        .with(user("test"))
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void getQuestions_shouldCallServiceWithCorrectEventId() throws Exception {
        // Arrange
        final String expectedEventId = "ABC-123";

        // Mock the service to return anything (we only care about the invocation here)
        when(questionService.getQuestionsWithEventId(eq(expectedEventId)))
                .thenReturn(Collections.emptyList());

        // Act
        mockMvc.perform(get(BASE_URL + "/{eventID}", expectedEventId)
                        .with(user("test")) // FIX: Simulate an authenticated user
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert
        verify(questionService, times(1))
                .getQuestionsWithEventId(eq(expectedEventId));
    }

    @Test
    void getQuestions_shouldReturn401Unauthorized_whenUnauthenticated() throws Exception {
        // Arrange
        final String eventId = "EVENT_SEC_FAIL";

        // Act & Assert
        // Perform request WITHOUT the .with(user("...")) security post-processor
        mockMvc.perform(get(BASE_URL + "/{eventID}", eventId)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Assert that the service was never called (Security should block the request early)
        verify(questionService, times(0))
                .getQuestionsWithEventId(anyString());
    }

    @Test
    void getQuestions_shouldBeAuthorizedWithSpecificRole() throws Exception {
        // Arrange
        final String eventId = "EVENT_AUTH";

        // Mock the service call to return an empty list
        when(questionService.getQuestionsWithEventId(eq(eventId)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        // Simulate a user named "adminUser" with roles "ADMIN" and "VIEWER"
        mockMvc.perform(get(BASE_URL + "/{eventID}", eventId)
                        .with(user("adminUser").roles("ADMIN", "VIEWER")) // Advanced use of user() post-processor
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk()) // Should succeed if the user is authorized (200 OK)
                .andExpect(content().json("[]"));

        // Assert
        verify(questionService, times(1))
                .getQuestionsWithEventId(eq(eventId));
    }

    private Question givenQuestion() {
        return new Question(
                "12345",                           // questionId
                "This is the first question",      // title
                "Text",                            // dataType
                BigDecimal.valueOf(1),             // order
                true,                              // answered
                true,                              // mandatory
                false,                             // multiAnswer
                "Lot1",                            // questionType
                false                              // isLegacyQuestion
        );
    }
}