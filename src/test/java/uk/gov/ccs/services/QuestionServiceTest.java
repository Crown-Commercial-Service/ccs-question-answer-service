package uk.gov.ccs.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ccs.dts.qas.model.generated.Question;
import uk.gov.ccs.entity.Questions;
import uk.gov.ccs.repo.QuestionRepository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    private static final String TEST_EVENT_ID = "ocds-pfhb7i-24266";
    @Mock
    private QuestionRepository questionRepo;

    @InjectMocks
    private QuestionService questionService;

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
        assertFalse(entity1Result.getIsLegacyQuestion());

        Questions entity2Result = result.get(1);
        assertEquals(entity2.getQuestionId(), entity2Result.getQuestionId());
        assertEquals(entity2.getQuestionTitle(), entity2Result.getQuestionTitle());
        assertFalse(entity2Result.getQuestionAnswered());
        assertTrue(entity2Result.getQuestionMandatory());
        assertTrue(entity2Result.getQuestionMultiAnswer());
        assertTrue(entity2Result.getIsLegacyQuestion());
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
        assertFalse(entityResult.getIsLegacyQuestion());

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





    private Questions givenQuestion1() {
        Questions q = new Questions();
        q.setQuestionId("12345");
        q.setQuestionTitle("This is the first question");
        q.setQuestionDataType("Text");
        q.setQuestionOrder(1);
        q.setQuestionAnswered(true);
        q.setQuestionMandatory(true);
        q.setQuestionMultiAnswer(false);
        q.setIsLegacyQuestion(false);
        q.setQuestionType("Lot1");
        return q;
    }

    private Questions givenQuestion2() {
        Questions q = new Questions();
        q.setQuestionId("56789");
        q.setQuestionTitle("This is the first question");
        q.setQuestionDataType("Blob");
        q.setQuestionOrder(2);
        q.setQuestionAnswered(false);
        q.setQuestionMandatory(true);
        q.setQuestionMultiAnswer(true);
        q.setIsLegacyQuestion(true);
        q.setQuestionType("Lot2");
        return q;
    }
}