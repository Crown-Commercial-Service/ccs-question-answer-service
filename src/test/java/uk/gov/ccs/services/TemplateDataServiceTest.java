package uk.gov.ccs.services;

import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.mapper.DefaultQuestionsToDataTemplateMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.DefaultQuestionsRepository;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateDataServiceTest {

    static final String AGREEMENT_ID = "RM1043.9";
    static final String LOT_NUMBER = "1";
    static final String EVENT_TYPE = "FC";

    @Mock
    private DefaultQuestionsRepository defaultQuestionsRepository;

    @Mock
    private DefaultQuestionsToDataTemplateMapper mapper;

    @Mock
    private Rollbar rollbar;

    @InjectMocks
    private TemplateDataService templateDataService;

    private DefaultQuestions createDefaultQuestion(String agreementId, String lotId) {
        return DefaultQuestions.builder()
                .id(1)
                .agreementId(agreementId)
                .lotId(lotId)
                .criteriaId("CRITERIA-1")
                .criterionTitle("Test Criterion")
                .groupId("GROUP-1")
                .groupDescription("Test Group")
                .groupTask("Test Task")
                .groupOrder(1)
                .groupPrompt("Test Prompt")
                .groupMandatory(true)
                .questionId("Q-1")
                .questionTitle("Test Question")
                .questionDescription("Test Description")
                .questionDataType("Text")
                .questionOrder(1)
                .questionAnswered(false)
                .questionMandatory(true)
                .questionDependency(null)
                .questionMultiAnswer(false)
                .questionType("Text")
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .updatedAt(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    @Test
    void getEventDataTemplates_shouldReturnEmptyList_whenNoQuestionsFound() {
        // Arrange
        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        // Act
        List<DataTemplate> result = templateDataService
                .getEventDataTemplates(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);

        // Assert
        assertTrue(result.isEmpty());
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                        AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);
        verify(mapper, never()).mapToDataTemplate(any());
    }

    @Test
    void getEventDataTemplates_shouldReturnMappedDataTemplates_whenQuestionsFound() {
        // Arrange
        DefaultQuestions question = createDefaultQuestion(AGREEMENT_ID, LOT_NUMBER);
        List<DefaultQuestions> questions = List.of(question);
        List<DataTemplate> expectedTemplates = List.of(DataTemplate.builder().build());

        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE))
                .thenReturn(questions);
        when(mapper.mapToDataTemplate(questions)).thenReturn(expectedTemplates);

        // Act
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);

        // Assert
        assertEquals(expectedTemplates, result);
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);
        verify(mapper, times(1)).mapToDataTemplate(questions);
    }

    @Test
    void getEventDataTemplates_shouldFormatLotId_whenLotIdHasPrefix() {
        // Arrange
        DefaultQuestions question = createDefaultQuestion("RM1043.8", "1");
        List<DefaultQuestions> questions = List.of(question);
        List<DataTemplate> expectedTemplates = List.of(DataTemplate.builder().build());

        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE))
                .thenReturn(questions);
        when(mapper.mapToDataTemplate(questions)).thenReturn(expectedTemplates);

        // Act - lotId with "Lot " prefix
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);

        // Assert - should remove "Lot " prefix
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID,
                        LOT_NUMBER, EVENT_TYPE);
    }

    @Test
    void getEventDataTemplates_shouldFormatLotId_whenLotIdHasWhitespace() {
        // Arrange
        DefaultQuestions question = createDefaultQuestion(AGREEMENT_ID, LOT_NUMBER);
        List<DefaultQuestions> questions = List.of(question);
        List<DataTemplate> expectedTemplates = List.of(DataTemplate.builder().build());

        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE))
                .thenReturn(questions);
        when(mapper.mapToDataTemplate(questions)).thenReturn(expectedTemplates);

        // Act - lotId with whitespace
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, " " +LOT_NUMBER + " ", EVENT_TYPE);

        // Assert - should trim whitespace
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                        AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);
    }

    @Test
    void getEventDataTemplates_shouldReturnEmptyList_whenRepositoryReturnsNull() {
        // Arrange
        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                anyString(), anyString(), anyString()))
                .thenReturn(null);

        // Act
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);

        // Assert
        assertTrue(result.isEmpty());
        verify(mapper, never()).mapToDataTemplate(any());
    }

    @Test
    void getEventDataTemplates_shouldHandleException_andReturnEmptyList() {
        // Arrange
        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);

        // Assert
        assertTrue(result.isEmpty());
        verify(rollbar, times(1)).error(any(Exception.class), anyString());
    }

    @Test
    void getEventDataTemplates_shouldIgnoreEventType() {
        // Arrange
        DefaultQuestions question = createDefaultQuestion(AGREEMENT_ID, LOT_NUMBER);
        List<DefaultQuestions> questions = List.of(question);
        List<DataTemplate> expectedTemplates = List.of(DataTemplate.builder().build());

        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE))
                .thenReturn(questions);
        when(mapper.mapToDataTemplate(questions)).thenReturn(expectedTemplates);

        // Act - eventType is passed but not used in query
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);

        // Assert - eventType doesn't affect the query
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                        AGREEMENT_ID, LOT_NUMBER, EVENT_TYPE);
        assertEquals(expectedTemplates, result);
    }

    @Test
    void getEventDataTemplates_shouldHandleNullLotId() {
        // Arrange
        when(defaultQuestionsRepository.findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(
                AGREEMENT_ID, null, EVENT_TYPE))
                .thenReturn(Collections.emptyList());

        // Act
        List<DataTemplate> result = templateDataService.getEventDataTemplates(AGREEMENT_ID, null, EVENT_TYPE);

        // Assert
        assertTrue(result.isEmpty());
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID, null, EVENT_TYPE);
    }
}

