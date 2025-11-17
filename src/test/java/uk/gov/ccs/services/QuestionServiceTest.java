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
        List<Question> result = questionService.getQuestionsWithEventId(TEST_EVENT_ID);

        verify(questionRepo, times(1))
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID);

        assertEquals(2, result.size());
        // verify
        Question dto1 = result.get(0);
        verifyQuestion(entity1, dto1);
        assertTrue(dto1.getAnswered());
        assertTrue(dto1.getMandatory());
        assertFalse(dto1.getMultiAnswer());
        assertFalse(dto1.getIsLegacyQuestion());

        Question dto2 = result.get(1);
        verifyQuestion(entity2, dto2);
        assertFalse(dto2.getAnswered());
        assertTrue(dto2.getMandatory());
        assertTrue(dto2.getMultiAnswer());
        assertTrue(dto2.getIsLegacyQuestion());
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
        List<Question> result = questionService.getQuestionsWithEventId(TEST_EVENT_ID);

        verify(questionRepo, times(1))
                .findAllByEventIdOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(TEST_EVENT_ID);

        assertEquals(1, result.size());

        verifyQuestion(singleEntity, result.get(0));
        Question dto = result.get(0);
        assertTrue(dto.getAnswered());
        assertTrue(dto.getMandatory());
        assertFalse(dto.getMultiAnswer());
        assertFalse(dto.getIsLegacyQuestion());

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



    private void verifyQuestion(Questions singleEntity, Question dto) {
        assertEquals(singleEntity.getQuestionId(), dto.getQuestionId());
        assertEquals(singleEntity.getQuestionTitle(), dto.getTitle());
        assertEquals(singleEntity.getQuestionDataType(), dto.getDataType());
        assertEquals(BigDecimal.valueOf(singleEntity.getQuestionOrder()), dto.getOrder());
        assertEquals(singleEntity.getQuestionDataType(), dto.getDataType());
        assertEquals(singleEntity.getQuestionType(), dto.getQuestionType());
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