package uk.gov.ccs.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.ccs.BLL.QuestionLogicClient;
import uk.gov.ccs.config.SecurityConfig;
import uk.gov.ccs.dts.qas.model.generated.QuestionWrite;
import uk.gov.ccs.dts.qas.model.generated.QuestionWriteResponse;
import uk.gov.ccs.entity.Questions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuestionController.class)
@Import(SecurityConfig.class)
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
    private QuestionLogicClient questionLogicClient;

    @Test
    void getQuestions_shouldReturnListOfQuestions_whenDataIsFound() throws Exception {
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
    void getQuestions_shouldReturnEmptyList_whenNoQuestionsAreFound() throws Exception {
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
    void getQuestions_shouldCallServiceWithCorrectEventId() throws Exception {
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
    void getQuestions_shouldAllowUnauthenticatedRequests() throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        // Assert that the service was called (Security allows the request)
        verify(questionLogicClient, times(1))
                .getQuestionsWithEventId(eq(eventId));
    }

    @Test
    void getQuestions_shouldBeAuthorizedWithSpecificRole() throws Exception {
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
        q.setAgreementId("RM1043.8");
        q.setLotId("1");
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
        q.setIsLegacyQuestion(false);
        return q;
    }

    // POST endpoint tests

    @Test
    void createQuestions_shouldReturn200_whenValidRequestAndResponseExists() throws Exception {
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
    void createQuestions_shouldReturn200_whenValidRequestWithEventType() throws Exception {
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
    void createQuestions_shouldReturn204_whenNoTemplateDataExists() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isNoContent());

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
    }

    @Test
    void createQuestions_shouldReturn400_whenRequestBodyIsNull() throws Exception {
        // Act & Assert
        mockMvc.perform(post(BASE_URL)
                        .with(user("test"))
                        .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(questionLogicClient, never())
                .createOrUpdateQuestions(any(), any());
    }

    @Test
    void createQuestions_shouldReturn400_whenEventIdIsNull() throws Exception {
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
    void createQuestions_shouldReturn400_whenEventIdIsEmpty() throws Exception {
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
    void createQuestions_shouldReturn400_whenAgreementIdIsNull() throws Exception {
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
    void createQuestions_shouldReturn400_whenAgreementIdIsEmpty() throws Exception {
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
    void createQuestions_shouldReturn400_whenLotIdIsNull() throws Exception {
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
    void createQuestions_shouldReturn400_whenLotIdIsEmpty() throws Exception {
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
    void createQuestions_shouldReturn400_whenIllegalArgumentExceptionThrown() throws Exception {
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
    void createQuestions_shouldReturn500_whenUnexpectedExceptionThrown() throws Exception {
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
    void createQuestions_shouldAllowUnauthenticatedRequests() throws Exception {
        // Arrange
        QuestionWrite questionWrite = givenQuestionWrite();
        QuestionWriteResponse response = givenQuestionWriteResponse();
        
        when(questionLogicClient.createOrUpdateQuestions(eq(questionWrite), isNull()))
                .thenReturn(response);

        // Act & Assert
        // SecurityConfig permits all requests, so unauthenticated requests should work
        mockMvc.perform(post(BASE_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(questionWrite)))
                .andExpect(status().isCreated())
                .andExpect(content().json(objectMapper.writeValueAsString(response)));

        verify(questionLogicClient, times(1))
                .createOrUpdateQuestions(eq(questionWrite), isNull());
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
}