package uk.gov.ccs.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.repo.QuestionRepository;
import com.rollbar.notifier.Rollbar;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static uk.gov.ccs.BLL.TestMatcher.givenQuestion1;
import static uk.gov.ccs.BLL.TestMatcher.givenQuestion2;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    private static final String TEST_EVENT_ID = "ocds-pfhb7i-24266";
    @Mock
    private QuestionRepository questionRepo;

    @InjectMocks
    private QuestionService questionService;

    @Mock
    private Rollbar rollbar;

    @Test
    void getQuestionsWithEventId_shouldReturnMappedQuestionWhenItExist() {

        // Arrange
        Questions entity1 = givenQuestion1();
        Questions entity2 = givenQuestion2();

        List<Questions> mockEntities = List.of(entity1, entity2);

        // Configure the mock repository to return the list of entities
        when(questionRepo.findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID))
                .thenReturn(mockEntities);

        // Act
        List<Questions> result = questionService.getQuestionsWithEventId(TEST_EVENT_ID);

        verify(questionRepo, times(1))
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID);

        assertEquals(2, result.size());
        // verify
        Questions entity1Result = result.get(0);
        assertEquals(entity1.getQuestionId(), entity1Result.getQuestionId());
        assertEquals(entity1.getQuestionTitle(), entity1Result.getQuestionTitle());
        assertTrue(entity1Result.getQuestionAnswered());
        assertTrue(entity1Result.getQuestionMandatory());
        assertFalse(entity1Result.getQuestionMultiAnswer());
        assertFalse(entity1Result.getIsDefaultQuestion());

        Questions entity2Result = result.get(1);
        assertEquals(entity2.getQuestionId(), entity2Result.getQuestionId());
        assertEquals(entity2.getQuestionTitle(), entity2Result.getQuestionTitle());
        assertFalse(entity2Result.getQuestionAnswered());
        assertTrue(entity2Result.getQuestionMandatory());
        assertTrue(entity2Result.getQuestionMultiAnswer());
        assertTrue(entity2Result.getIsDefaultQuestion());
    }

    @Test
    void getQuestionsWithEventId_shouldReturnEmptyList_whenNoQuestionExist() {
        // Arrange
        when(questionRepo.findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(anyString()))
                .thenReturn(Collections.emptyList());

        // Act
        List<?> result = questionService.getQuestionsWithEventId(TEST_EVENT_ID);

        // Assert
        verify(questionRepo, times(1))
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID);
        assertTrue(result.isEmpty());
    }

    @Test
    void getQuestionsWithEventId_shouldHandleSingleQuestion() {
        // Arrange
        Questions singleEntity = givenQuestion1();

        List<Questions> mockEntities = List.of(singleEntity);

        // Configure the mock repository to return the single entity
        when(questionRepo.findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID))
                .thenReturn(mockEntities);

        // Act
        List<Questions> result = questionService.getQuestionsWithEventId(TEST_EVENT_ID);

        verify(questionRepo, times(1))
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID);

        assertEquals(1, result.size());

        Questions entityResult = result.get(0);
        assertEquals(singleEntity.getQuestionId(), entityResult.getQuestionId());
        assertEquals(singleEntity.getQuestionTitle(), entityResult.getQuestionTitle());
        assertTrue(entityResult.getQuestionAnswered());
        assertTrue(entityResult.getQuestionMandatory());
        assertFalse(entityResult.getQuestionMultiAnswer());
        assertFalse(entityResult.getIsDefaultQuestion());

    }

    @Test
    void getQuestionsWithEventId_shouldHandleEmptyEventIdString() {
        // Arrange
        final String emptyEventId = "";

        when(questionRepo.findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(emptyEventId))
                .thenReturn(Collections.emptyList());

        // Act
        List<?> result = questionService.getQuestionsWithEventId(emptyEventId);

        // Assert
        verify(questionRepo, times(1))
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(emptyEventId);

        // verify
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteQuestionShouldReturnOneWhenQuestionDeletedSuccessfully() {
        // Arrange
        final String eventId = TEST_EVENT_ID;
        final String questionId = "QID123";
        final long expectedDeletedCount = 1L;

        // Mock the repository to return 1 (success)
        when(questionRepo.deleteByEventIdAndQuestionId(eventId, questionId)).thenReturn(expectedDeletedCount);

        // Act
        long deletedCount = questionService.deleteQuestion(eventId, questionId);

        // Assert
        assertEquals(expectedDeletedCount, deletedCount, "Should return the count of deleted entities (1).");
        // Verify Repository was called
        verify(questionRepo, times(1)).deleteByEventIdAndQuestionId(eventId, questionId);
        // Verify Rollbar was NOT called on success
        verify(rollbar, times(0)).error(any(Throwable.class), anyString());
    }

    @Test
    void deleteQuestionShouldReturnZeroWhenQuestionNotFound() {
        // Arrange
        final String eventId = TEST_EVENT_ID;
        final String questionId = "QID_MISSING";
        final long expectedDeletedCount = 0L;

        // Mock the repository to return 0 (not found/deleted)
        when(questionRepo.deleteByEventIdAndQuestionId(eventId, questionId)).thenReturn(expectedDeletedCount);

        // Act
        long deletedCount = questionService.deleteQuestion(eventId, questionId);

        // Assert
        assertEquals(expectedDeletedCount, deletedCount, "Should return 0 when no entity is deleted.");
        // Verify Repository was called
        verify(questionRepo, times(1)).deleteByEventIdAndQuestionId(eventId, questionId);
        // Verify Rollbar was NOT called
        verify(rollbar, times(0)).error(any(Throwable.class), anyString());
    }

    @Test
    void deleteQuestionShouldLogToRollbarAndRethrowWhenRepositoryThrowsException() {
        // Arrange
        final String eventId = TEST_EVENT_ID;
        final String questionId = "QID_FAIL";
        // Use a standard JPA-related exception (or any RuntimeException)
        final RuntimeException expectedException = new RuntimeException("Simulated database constraint violation");

        // Mock the repository call to throw the exception
        when(questionRepo.deleteByEventIdAndQuestionId(eventId, questionId)).thenThrow(expectedException);

        // Use ArgumentCaptors for advanced verification of the Rollbar logging
        ArgumentCaptor<Throwable> exceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);

        // Act & Assert
        // 1. Verify the original exception is re-thrown by asserting the type
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            questionService.deleteQuestion(eventId, questionId);
        });

        // 2. Assert the thrown exception instance is the one we expected
        assertEquals(expectedException, thrown, "The re-thrown exception should be the original exception.");

        // 3. Verify Rollbar logging occurred exactly once
        verify(rollbar, times(1)).error(exceptionCaptor.capture(), messageCaptor.capture());

        // 4. Assert captured Rollbar arguments
        assertEquals(expectedException, exceptionCaptor.getValue(), "Rollbar must log the actual exception instance.");

        // Assert that the captured message contains the contextual information
        String capturedMessage = messageCaptor.getValue();
        assertTrue(capturedMessage.startsWith("Error occured while deleting question"),
                "Rollbar message should start with the standard prefix.");
        assertTrue(capturedMessage.contains(eventId), "Rollbar message should contain the eventId.");
        assertTrue(capturedMessage.contains(questionId), "Rollbar message should contain the questionId.");
    }


}