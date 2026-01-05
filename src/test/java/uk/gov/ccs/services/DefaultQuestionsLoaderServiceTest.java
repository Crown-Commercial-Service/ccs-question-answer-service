package uk.gov.ccs.services;

import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.ccs.entity.DefaultQuestions;
import uk.gov.ccs.mapper.DataTemplateToDefaultQuestionsMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.DefaultQuestionsRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static uk.gov.ccs.services.DataLoaderTestMatcher.createDataTemplate;

@ExtendWith(MockitoExtension.class)
class DefaultQuestionsLoaderServiceTest {

    private static final String AGREEMENT_ID = "RM1043.9";
    private static final String LOT_ID = "1";
    private static final String EVENT_TYPE = "FC";

    @Mock
    private DefaultQuestionsRepository defaultQuestionsRepository;

    @Mock
    private DataTemplateToDefaultQuestionsMapper mapper;

    @Mock
    private Rollbar rollbar;

    @InjectMocks
    private DefaultQuestionsLoaderService service;

    @BeforeEach
    void setUp() {
        // Initialize Mockito annotations
        MockitoAnnotations.openMocks(this);
    }

    private DefaultQuestions createMockDefaultQuestion() {
        return mock(DefaultQuestions.class);
    }

    @Test
    void loadDefaultQuestionsFromBodySuccessfulLoadWithExistingQuestionsReturnsCorrectCount() throws Exception {
        // ARRANGE
        List<DataTemplate> mockTemplates = List.of(createDataTemplate(), createDataTemplate());
        List<DefaultQuestions> newQuestions = List.of(createMockDefaultQuestion(), createMockDefaultQuestion());
        List<DefaultQuestions> existingQuestions = List.of(createMockDefaultQuestion(), createMockDefaultQuestion(), createMockDefaultQuestion());

        // Configure Mocks
        when(mapper.mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE))
                .thenReturn(newQuestions);
        when(defaultQuestionsRepository
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID,
                        LOT_ID, EVENT_TYPE))
                .thenReturn(existingQuestions);

        // ACT
        int result = service.loadDefaultQuestionsFromBody(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // ASSERT
        assertEquals(2, result, "Should return the count of newly inserted questions.");

        // Verification of execution flow (Component Collaboration)
        // 1. Delete existing questions
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID,
                        LOT_ID, EVENT_TYPE);
        verify(defaultQuestionsRepository, times(1)).deleteAll(existingQuestions);
        verify(rollbar, times(1)).info(contains("Deleted 3 existing default questions"));

        // 2. Insert new questions
        verify(defaultQuestionsRepository, times(1)).saveAll(newQuestions);
        verify(rollbar, times(1)).info(contains("Successfully loaded 2 default questions"));

        // Ensure no error/warning logs were generated unexpectedly
        verify(rollbar, never()).error(any(), anyString());
        verify(rollbar, never()).warning(anyString());
    }

    @Test
    void loadDefaultQuestionsFromBodySuccessfulLoadNoExistingQuestionsReturnsCorrectCount() {
        // ARRANGE
        List<DataTemplate> mockTemplates = List.of(createDataTemplate());
        List<DefaultQuestions> newQuestions = List.of(createMockDefaultQuestion());

        // Configure Mocks
        when(mapper.mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE))
                .thenReturn(newQuestions);
        when(defaultQuestionsRepository
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID,
                        LOT_ID, EVENT_TYPE))
                .thenReturn(Collections.emptyList()); // No existing data

        // ACT
        int result = service.loadDefaultQuestionsFromBody(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // ASSERT
        assertEquals(1, result, "Should return the count of newly inserted questions.");

        // Verification of execution flow
        verify(mapper, times(1)).mapToDefaultQuestions(mockTemplates, AGREEMENT_ID,
                LOT_ID, EVENT_TYPE);

        // Delete step verification (deleteAll should not be called)
        verify(defaultQuestionsRepository, times(1))
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID,
                        LOT_ID, EVENT_TYPE);
        verify(rollbar, never()).info(contains("Deleted"));

        // Insert new questions
        verify(defaultQuestionsRepository, times(1)).saveAll(newQuestions);
        verify(rollbar, times(1)).info(contains("Successfully loaded 1 default questions"));
    }

    @Test
    void loadDefaultQuestionsFromBodyWithNullTemplatesReturnsZeroAndLogsWarning() {
        // ACT
        int result = service.loadDefaultQuestionsFromBody(null, AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // ASSERT
        assertEquals(0, result);
        verify(rollbar, times(1)).warning(contains("No data templates provided"));
        verifyNoInteractions(mapper);
        verifyNoInteractions(defaultQuestionsRepository);
    }

    @Test
    void loadDefaultQuestionsFromBodyWithEmptyTemplatesReturnsZeroAndLogsWarning() {
        // ACT
        int result = service.loadDefaultQuestionsFromBody(Collections.emptyList(), AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // ASSERT
        assertEquals(0, result);
        verify(rollbar, times(1)).warning(contains("No data templates provided"));
        verifyNoInteractions(mapper);
        verifyNoInteractions(defaultQuestionsRepository);
    }

    @Test
    void loadDefaultQuestionsFromBodyMapperReturnsEmptyListReturnsZeroAndLogsWarning() {
        // ARRANGE
        List<DataTemplate> mockTemplates = List.of(createDataTemplate());
        when(mapper.mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE))
                .thenReturn(Collections.emptyList());

        // ACT
        int result = service.loadDefaultQuestionsFromBody(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE);

        // ASSERT
        assertEquals(0, result);
        verify(mapper, times(1))
                .mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE);
        verify(rollbar, times(1)).warning(contains("No default questions found in provided data templates"));

        // Should stop before interacting with the repository
        verify(defaultQuestionsRepository, never())
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(anyString(),
                        anyString(), anyString());
    }

    @Test
    void loadDefaultQuestionsFromBodyMapperThrowsExceptionLogsErrorAndThrowsRuntimeException() {
        // ARRANGE
        List<DataTemplate> mockTemplates = List.of(createDataTemplate());
        RuntimeException mapperEx = new RuntimeException("Mapping Failed");
        when(mapper.mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE))
                .thenThrow(mapperEx);

        // ACT & ASSERT
        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> service.loadDefaultQuestionsFromBody(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE),
                "Should re-throw a RuntimeException");

        // Verify exception details
        assertEquals("Failed to load default questions from request body", thrown.getMessage());
        assertEquals(mapperEx, thrown.getCause());

        // Verify logging (Crucial for Experience Level Test)
        verify(rollbar, times(1)).error(eq(mapperEx),
                eq("Error loading default questions from request body for agreement: RM1043.9, lot: 1"));

        // Verify no repository actions occurred
        verifyNoMoreInteractions(defaultQuestionsRepository);
    }

    @Test
    void loadDefaultQuestionsFromBodyRepositoryFindThrowsExceptionLogsErrorAndThrowsRuntimeException() {
        // ARRANGE
        List<DataTemplate> mockTemplates = List.of(createDataTemplate());
        List<DefaultQuestions> newQuestions = List.of(createMockDefaultQuestion());
        RuntimeException repoEx = new RuntimeException("DB Query Failed");

        when(mapper.mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE))
                .thenReturn(newQuestions);
        when(defaultQuestionsRepository
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(AGREEMENT_ID,
                        LOT_ID, EVENT_TYPE))
                .thenThrow(repoEx);

        // ACT & ASSERT
        assertThrows(RuntimeException.class,
                () -> service.loadDefaultQuestionsFromBody(mockTemplates,
                        AGREEMENT_ID, LOT_ID, EVENT_TYPE),
                "Should re-throw a RuntimeException");

        // Verify logging
        verify(rollbar, times(1)).error(eq(repoEx),
                eq("Error loading default questions from request body for agreement: RM1043.9, lot: 1"));

        // Verify saveAll was not called
        verify(defaultQuestionsRepository, never()).saveAll(any());
    }

    @Test
    void loadDefaultQuestionsFromBodyRepositorySaveAllThrowsExceptionLogsErrorAndThrowsRuntimeException() {
        // ARRANGE
        List<DataTemplate> mockTemplates = List.of(createDataTemplate());
        List<DefaultQuestions> newQuestions = List.of(createMockDefaultQuestion());
        RuntimeException saveEx = new RuntimeException("DB Save Failed");

        when(mapper.mapToDefaultQuestions(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE))
                .thenReturn(newQuestions);
        when(defaultQuestionsRepository
                .findByAgreementIdAndLotIdAndEventTypeOrderByCriteriaIdAscGroupIdAscQuestionOrderAsc(anyString(),
                        anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(defaultQuestionsRepository.saveAll(newQuestions)).thenThrow(saveEx);

        // ACT & ASSERT
        assertThrows(RuntimeException.class,
                () -> service.loadDefaultQuestionsFromBody(mockTemplates, AGREEMENT_ID, LOT_ID, EVENT_TYPE),
                "Should re-throw a RuntimeException");

        // Verify logging
        verify(rollbar, times(1)).error(eq(saveEx),
                eq("Error loading default questions from request body for agreement: RM1043.9, lot: 1"));
    }
}