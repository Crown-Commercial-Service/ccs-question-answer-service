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
import uk.gov.ccs.repo.QuestionRepository;
import uk.gov.ccs.services.QuestionService;

import java.util.List;

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

}