package uk.gov.ccs.BLL;

import com.rollbar.notifier.Rollbar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cache.CacheManager;
import uk.gov.ccs.clients.AgreementsClient;
import uk.gov.ccs.mapper.DataTemplateMapper;
import uk.gov.ccs.model.agreements.DataTemplate;
import uk.gov.ccs.repo.QuestionRepository;
import uk.gov.ccs.services.QuestionService;
import uk.gov.ccs.services.TemplateDataService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ccs.BLL.TestMatcher.givenQuestion1;

@SpringBootTest(classes = CacheTestConfiguration.class)
class QuestionLogicClientTest {

    private final String TEST_EVENT_ID = "EVENT-TEST-1";

    @MockBean
    private QuestionRepository questionRepository;

    @MockBean
    private Rollbar rollbar;

    @MockBean
    private AgreementsClient agreementsClient;

    @MockBean
    private DataTemplateMapper dataTemplateMapper;

    @MockBean
    private TemplateDataService templateDataService;

    @Autowired
    private QuestionLogicClient client;

    // Spy on the service to verify how many times its method is actually called
    @SpyBean
    private QuestionService questionService;

    // Inject the CacheManager to manually clear the cache before each test
    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Clear the cache before each test run
        if (cacheManager.getCache("qAndACache") != null) {
            cacheManager.getCache("qAndACache").clear();
        }
        if (cacheManager.getCache("dataTemplatesCache") != null) {
            cacheManager.getCache("dataTemplatesCache").clear();
        }

        // Setup the spy to return a list when called
        when(questionService.getQuestionsWithEventId(anyString())).thenReturn(
                List.of(givenQuestion1())
        );
    }

    /**
     * Test Case 1: Verifies the @Cacheable annotation.
     * The service method should only be called once, even if the client method is called multiple times
     * with the same key.
     */
    @Test
    void getQuestionsWithEventIdShouldBeCalledOnceWhenCalledMultipleTimes() {
        // 1. First call: Cache Miss, service method called once
        client.getQuestionsWithEventId(TEST_EVENT_ID);

        // 2. Subsequent calls: Cache Hit, service method NOT called
        client.getQuestionsWithEventId(TEST_EVENT_ID);
        client.getQuestionsWithEventId(TEST_EVENT_ID);

        // Assert: Service call count must be 1
        verify(questionService, times(1)).getQuestionsWithEventId(TEST_EVENT_ID);
    }

    @Test
    void deleteQuestionShouldEvictCacheForcingSecondServiceCall() {
        // 1. Initial Load: Service is called (Cache Miss -> Cache Populated)
        client.getQuestionsWithEventId(TEST_EVENT_ID);

        // 2. Verify cache is populated (Cache Hit -> Service not called)
        client.getQuestionsWithEventId(TEST_EVENT_ID);

        // Verification point: Service has been called once so far
        verify(questionService, times(1)).getQuestionsWithEventId(TEST_EVENT_ID);

        // 3. Eviction: Call the delete method which has @CacheEvict
        client.deleteQuestion(TEST_EVENT_ID, "QID-999");

        // 4. Reload: Call getQuestions again (Cache Miss -> Service is called again)
        client.getQuestionsWithEventId(TEST_EVENT_ID);

        // Assert: Service call count must be 2
        verify(questionService, times(2)).getQuestionsWithEventId(TEST_EVENT_ID);
    }

    /**
     * Test Case: Verifies the @Cacheable annotation for getEventDataTemplates.
     * The service method should only be called once, even if the client method is called multiple times
     * with the same key.
     */
    @Test
    void getEventDataTemplatesShouldBeCalledOnceWhenCalledMultipleTimes() {
        // Arrange
        String agreementId = "RM1043.8";
        String lotId = "1";
        String eventType = "FC";
        List<DataTemplate> mockTemplates = List.of(DataTemplate.builder().build());
        
        when(templateDataService.getEventDataTemplates(agreementId, lotId, eventType))
                .thenReturn(mockTemplates);

        // 1. First call: Cache Miss, service method called once
        List<DataTemplate> result1 = client.getEventDataTemplates(agreementId, lotId, eventType);

        // 2. Subsequent calls: Cache Hit, service method NOT called
        List<DataTemplate> result2 = client.getEventDataTemplates(agreementId, lotId, eventType);
        List<DataTemplate> result3 = client.getEventDataTemplates(agreementId, lotId, eventType);

        // Assert: Service call count must be 1
        verify(templateDataService, times(1)).getEventDataTemplates(agreementId, lotId, eventType);
        assertEquals(mockTemplates, result1);
        assertEquals(mockTemplates, result2);
        assertEquals(mockTemplates, result3);
    }

    @Test
    void getEventDataTemplatesShouldReturnDifferentResultsForDifferentKeys() {
        // Arrange
        List<DataTemplate> templates1 = List.of(DataTemplate.builder().build());
        List<DataTemplate> templates2 = List.of(DataTemplate.builder().build());
        
        when(templateDataService.getEventDataTemplates("RM1043.8", "1", "FC"))
                .thenReturn(templates1);
        when(templateDataService.getEventDataTemplates("RM1043.8", "2", "FC"))
                .thenReturn(templates2);

        // Act - different lot IDs should result in different cache keys
        List<DataTemplate> result1 = client.getEventDataTemplates("RM1043.8", "1", "FC");
        List<DataTemplate> result2 = client.getEventDataTemplates("RM1043.8", "2", "FC");

        // Assert: Both should be called once (different cache keys)
        verify(templateDataService, times(1)).getEventDataTemplates("RM1043.8", "1", "FC");
        verify(templateDataService, times(1)).getEventDataTemplates("RM1043.8", "2", "FC");
        assertEquals(templates1, result1);
        assertEquals(templates2, result2);
    }

    @Test
    void getEventDataTemplatesShouldHandleException() {
        // Arrange
        when(templateDataService.getEventDataTemplates(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Service error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            client.getEventDataTemplates("RM1043.8", "1", "FC");
        });
        
        verify(rollbar, times(1)).error(any(Exception.class), anyString());
    }

    @Test
    void getEventDataTemplatesShouldReturnEmptyListWhenServiceReturnsEmpty() {
        // Arrange
        when(templateDataService.getEventDataTemplates("RM1043.8", "1", "FC"))
                .thenReturn(Collections.emptyList());

        // Act
        List<DataTemplate> result = client.getEventDataTemplates("RM1043.8", "1", "FC");

        // Assert
        assertTrue(result.isEmpty());
        verify(templateDataService, times(1)).getEventDataTemplates("RM1043.8", "1", "FC");
    }

}