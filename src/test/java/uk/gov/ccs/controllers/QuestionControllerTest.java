package uk.gov.ccs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.ccs.BLL.QuestionLogicClient;
import uk.gov.ccs.config.SecurityConfig;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.dts.qas.model.generated.QuestionWriteResponse;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.services.QuestionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static uk.gov.ccs.constants.Constants.responses_Success;

@WebMvcTest(QuestionController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "config.security.api-key=abdc1234")
@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    private static final String BASE_URL = "/questions";
    private static final String TEST_EVENT_ID = "TEST_12344_OCD";
    private static final String TEST_QUESTION_ID = "question 2";
    private static final String TEST_AGREEMENT_ID = "AGR_1234";
    private static final String TEST_LOT_ID = "LOT_5678";
    private static final String TEST_EVENT_TYPE = "FC";
    private final String LOAD_DEFAULTS_URL = BASE_URL + "/{agreement-id}/lots/{lot-id}/{event-type}/load-default-questions";

    @Autowired
    private MockMvc mockMvc;

    @Value("${config.security.api-key}")
    private String apiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private Rollbar rollbar;

    @Mock
    private QuestionLogicClient questionLogicClient;

    @Mock
    private QuestionService questionService;

    @Test
    void getQuestionsShouldReturnListOfQuestionsWhenDataIsFound() throws Exception {
        // Arrange
        Questions question1 = givenQuestion();
        Questions question2 = givenQuestion();

        // Mock the service call to return our list of entities
        when(questionLogicClient.getQuestionsWithEventId(eq(TEST_EVENT_ID)))
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
    void getQuestionsShouldReturnEmptyListWhenNoQuestionsAreFound() throws Exception {
        // Arrange
        final String emptyEventId = "EVENT_NO_QUESTIONS";

        // Mock the service call to return an empty list
        when(questionLogicClient.getQuestionsWithEventId(eq(emptyEventId)))
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
    void getQuestionsShouldCallServiceWithCorrectEventId() throws Exception {
        // Arrange
        final String expectedEventId = "ABC-123";

        // Mock the service to return anything (we only care about the invocation here)
        when(questionLogicClient.getQuestionsWithEventId(eq(expectedEventId)))
                .thenReturn(Collections.emptyList());

        // Act
        mockMvc.perform(get(BASE_URL + "/{eventID}", expectedEventId)
                        .with(user("test")) // FIX: Simulate an authenticated user
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert
        verify(questionLogicClient, times(1))
                .getQuestionsWithEventId(eq(expectedEventId));
    }

    @Test
    void getQuestionsShouldAllowAuthenticatedRequests() throws Exception {
        // Arrange
        final String eventId = "EVENT_UNAUTH";
        
        // Mock the service call to return an empty list
        when(questionLogicClient.getQuestionsWithEventId(eq(eventId)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        // Perform request WITHOUT the .with(user("...")) security post-processor
        // SecurityConfig permits all requests, so unauthenticated requests should work
        mockMvc.perform(get(BASE_URL + "/{eventID}", eventId)
                        .header("x-api-key", apiKey)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Assert that the service was called (Security allows the request)
        verify(questionLogicClient, times(1))
                .getQuestionsWithEventId(eq(eventId));
    }

    @Test
    void getQuestionsShouldNotAllowUnauthenticatedRequests() throws Exception {
        // Arrange
        final String eventId = "EVENT_UNAUTH";
        
        // Mock the service call to return an empty list
        when(questionLogicClient.getQuestionsWithEventId(eq(eventId)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        // Perform request WITHOUT the .with(user("...")) security post-processor
        // SecurityConfig permits all requests, so unauthenticated requests should work
        mockMvc.perform(get(BASE_URL + "/{eventID}", eventId)
                        .accept(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getQuestionsShouldBeAuthorizedWithSpecificRole() throws Exception {
        // Arrange
        final String eventId = "EVENT_AUTH";

        // Mock the service call to return an empty list
        when(questionLogicClient.getQuestionsWithEventId(eq(eventId)))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        // Simulate a user named "adminUser" with roles "ADMIN" and "VIEWER"
        mockMvc.perform(get(BASE_URL + "/{eventID}", eventId)
                        .with(user("adminUser").roles("ADMIN", "VIEWER")) // Advanced use of user() post-processor
                        .accept(APPLICATION_JSON))
                .andExpect(status().isOk()) // Should succeed if the user is authorized (200 OK)
                .andExpect(content().json("[]"));

        // Assert
        verify(questionLogicClient, times(1))
                .getQuestionsWithEventId(eq(eventId));
    }

    private Questions givenQuestion() {
        Questions q = new Questions();
        q.setId(1);
        q.setEventId(TEST_EVENT_ID);
        q.setCriteriaId("criteria1");
        q.setCriterionTitle("criterion tile");
        q.setGroupId("This is test");
        q.setGroupDescription("Test group description");
        q.setGroupTask("group task");
        q.setGroupOrder(1);
        q.setGroupPrompt("Group promt");
        q.setGroupMandatory(false);
        q.setQuestionId("12345");
        q.setQuestionTitle("What is your role");
        q.setQuestionDescription("Details about your role. e.g. BA");
        q.setQuestionDataType("text");
        q.setQuestionOrder(1);
        q.setQuestionAnswered(false);
        q.setQuestionMandatory(false);
        q.setQuestionDependency(null);
        q.setQuestionMultiAnswer(false);
        q.setQuestionType("Technical");
        q.setIsDefaultQuestion(false);
        return q;
    }

    // Helper methods for POST tests

    private QuestionWrite givenQuestionWrite() {
        QuestionWrite questionWrite = new QuestionWrite();
        questionWrite.setEventId("test-event-123");
        questionWrite.setAgreementId("RM1043.8");
        questionWrite.setLotId("1");
        questionWrite.setCriterion(new ArrayList<>());
        return questionWrite;
    }

    private QuestionWriteResponse givenQuestionWriteResponse() {
        QuestionWriteResponse response = new QuestionWriteResponse();
        response.setId(1L);
        response.setEventId("test-event-123");
        response.setAgreementId("RM1043.8");
        response.setLotId("1");
        return response;
    }

    /**
     * Mock DTO for the DataTemplate in the request body used by the new POST endpoint.
     */
    record MockDataTemplate(String templateName, String version) {}

    // POST endpoint tests

    @Test
    void createQuestionsShouldReturn200_whenValidRequestAndResponseExists() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        QuestionWriteResponse response = givenQuestionWriteResponse();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
    }

    @Test
    void createQuestionsShouldReturn200_whenValidRequestWithEventType() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        QuestionWriteResponse response = givenQuestionWriteResponse();
        String eventType = "FC";
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), eq(eventType)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .param("eventType", eventType)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), eq(eventType));
    }

    @Test
    void createQuestionsShouldReturn204_whenNoTemplateDataExists() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isAccepted());

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
    }

    @Test
    void createQuestionsShouldReturn400_whenRequestBodyIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenEventIdIsNull() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        questionWrite.setEventId(null);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenEventIdIsEmpty() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        questionWrite.setEventId("   ");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenAgreementIdIsNull() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        questionWrite.setAgreementId(null);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenAgreementIdIsEmpty() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        questionWrite.setAgreementId("   ");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenLotIdIsNull() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        questionWrite.setLotId(null);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenLotIdIsEmpty() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        questionWrite.setLotId("   ");

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestionsShouldReturn400_whenIllegalArgumentExceptionThrown() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenThrow(new IllegalArgumentException("Validation error: Event type is required"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
    }

    @Test
    void createQuestionsShouldReturn500_whenUnexpectedExceptionThrown() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isInternalServerError());

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
    }

    @Test
    void createQuestionsShouldAllowAuthenticatedRequests() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        QuestionWriteResponse response = givenQuestionWriteResponse();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .header("x-api-key", apiKey)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
    }

    @Test
    void createQuestionsShouldNotAllowUnauthenticatedRequests() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        QuestionWriteResponse response = givenQuestionWriteResponse();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteQuestionShouldReturn200OKAndSuccessBodyWhenQuestionIsDeleted() throws Exception {
        // Arrange
        when(questionLogicClient.deleteQuestion(eq(TEST_EVENT_ID), eq(TEST_QUESTION_ID))).thenReturn(1L);

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/{eventId}/{questionId}", TEST_EVENT_ID, TEST_QUESTION_ID)
                        .with(user("authorizedUser").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(responses_Success));

        // Verify the logic client was called
        verify(questionLogicClient, times(1))
                .deleteQuestion(eq(TEST_EVENT_ID), eq(TEST_QUESTION_ID));

        // Verify Rollbar was NOT called on success
        verify(rollbar, times(0)).error(anyString());
    }

    @Test
    void deleteQuestionShouldReturn404NotFoundAndLogRollbarErrorWhenQuestionNotFound() throws Exception {
        // Arrange
        final String nonExistentQuestionId = "QID-MISSING";

        // Mock the logic client to return 0 (no entities deleted)
        when(questionLogicClient.deleteQuestion(eq(TEST_EVENT_ID), eq(nonExistentQuestionId))).thenReturn(0L);

        // Expected error message pattern for Rollbar assertion
        final String expectedLogMessage = "DELETE /questions failed, no match found for the eventId and questionId."
                + "eventId=" + TEST_EVENT_ID + " questionId= " + nonExistentQuestionId;

        // Act & Assert
        mockMvc.perform(delete(BASE_URL + "/{eventId}/{questionId}", TEST_EVENT_ID, nonExistentQuestionId)
                        .with(user("authorizedUser").roles("ADMIN"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()) // Assert 404 Not Found
                .andExpect(content().string("")); // Assert empty body for 404

        // Verify the logic client was called
        verify(questionLogicClient, times(1))
                .deleteQuestion(eq(TEST_EVENT_ID), eq(nonExistentQuestionId));

        // Verify Rollbar was called with the correct message
        verify(rollbar, times(1))
                .error(argThat((String message) -> message.contains(expectedLogMessage)));
    }

    @Test
    void loadDefaultQuestionsShouldReturn201AndCountWhenSuccessful() throws Exception {
        // Arrange
        final int successfullyLoadedCount = 5;
        List<MockDataTemplate> mockTemplates = List.of(new MockDataTemplate("Template1", "V1"));

        when(questionLogicClient.loadDefaultQuestions(
                any(),
                eq(TEST_AGREEMENT_ID),
                eq(TEST_LOT_ID),
                eq(TEST_EVENT_TYPE)))
                .thenReturn(successfullyLoadedCount);

        // Act & Assert
        mockMvc.perform(post(LOAD_DEFAULTS_URL, TEST_AGREEMENT_ID, TEST_LOT_ID, TEST_EVENT_TYPE)
                        .with(user("authorizedUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockTemplates)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Successfully loaded and created " + successfullyLoadedCount + " default questions"));

        // Verify the logic client was called
        verify(questionLogicClient, times(1))
                .loadDefaultQuestions(any(), eq(TEST_AGREEMENT_ID), eq(TEST_LOT_ID), eq(TEST_EVENT_TYPE));
    }

    @Test
    void loadDefaultQuestionsShouldReturn400BadRequestWhenEmptyBody() throws Exception {
        // Arrange
        List<MockDataTemplate> emptyTemplates = Collections.emptyList();

        // Act & Assert
        mockMvc.perform(post(LOAD_DEFAULTS_URL, TEST_AGREEMENT_ID, TEST_LOT_ID, TEST_EVENT_TYPE)
                        .with(user("authorizedUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyTemplates)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Request body must contain a non-empty array of DataTemplate objects"));

        // Verify the logic client was NOT called
        verify(questionLogicClient, times(0))
                .loadDefaultQuestions(any(), anyString(), anyString(), anyString());
        // Verify Rollbar was NOT called
        verify(rollbar, times(0)).error(any(Throwable.class), anyString());
    }

    @Test
    void loadDefaultQuestionsShouldReturn404AndZeroCountWhenNoQuestionsFound() throws Exception {
        // Arrange
        final int zeroCount = 0;
        List<MockDataTemplate> mockTemplates = List.of(new MockDataTemplate("Template2", "V1"));

        when(questionLogicClient.loadDefaultQuestions(
                any(),
                eq(TEST_AGREEMENT_ID),
                eq(TEST_LOT_ID),
                eq(TEST_EVENT_TYPE)))
                .thenReturn(zeroCount);

        // Act & Assert
        mockMvc.perform(post(LOAD_DEFAULTS_URL, TEST_AGREEMENT_ID, TEST_LOT_ID, TEST_EVENT_TYPE)
                        .with(user("authorizedUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockTemplates)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No default questions were found or loaded for agreementId: " +
                        TEST_AGREEMENT_ID + ", lotId: " + TEST_LOT_ID + "."));

        // Verify the logic client was called
        verify(questionLogicClient, times(1))
                .loadDefaultQuestions(any(), eq(TEST_AGREEMENT_ID), eq(TEST_LOT_ID), eq(TEST_EVENT_TYPE));
    }

    @Test
    void loadDefaultQuestionsShouldReturn500AndLogRollbarErrorWhenServiceThrowsException() throws Exception {
        // Arrange
        final String expectedErrorMessage = "Database connection failed";
        List<MockDataTemplate> mockTemplates = List.of(new MockDataTemplate("Template3", "V1"));

        // Mock the logic client to throw an exception
        when(questionLogicClient.loadDefaultQuestions(
                any(),
                eq(TEST_AGREEMENT_ID),
                eq(TEST_LOT_ID),
                eq(TEST_EVENT_TYPE)))
                .thenThrow(new RuntimeException(expectedErrorMessage)); // Throw a runtime exception

        // Act & Assert
        mockMvc.perform(post(LOAD_DEFAULTS_URL, TEST_AGREEMENT_ID, TEST_LOT_ID, TEST_EVENT_TYPE)
                        .with(user("authorizedUser").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mockTemplates)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error loading default questions: " + expectedErrorMessage));

        // Verify Rollbar logging occurred exactly once
        verify(rollbar, times(1)).error(any(Throwable.class), anyString());

        // Assert captured Rollbar message contains the context
        verify(rollbar, times(1))
                .error(any(Throwable.class), argThat(
                        (String message) -> message.contains(TEST_AGREEMENT_ID) && message.contains(TEST_LOT_ID)
                ));
    }

    @Test
    void whenRequestingMissingResourceThenReturnsGlobal404Json() throws Exception {

        mockMvc.perform(get("/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void whenRequestingMissingStaticFile_thenReturnsGlobal404Json() throws Exception {

        mockMvc.perform(get("/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/"));
    }
}