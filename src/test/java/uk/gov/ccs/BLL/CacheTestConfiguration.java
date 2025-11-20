package uk.gov.ccs.BLL;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ccs.services.QuestionService;

@Configuration
@EnableCaching
class CacheTestConfiguration {

    // Provide a real bean for the QuestionService that can be spied on
    @Bean
    public QuestionService questionService() {
        return new QuestionService();
    }

    // Provide the QuestionLogicClient which uses caching
    @Bean
    public QuestionLogicClient questionLogicClient() {
        return new QuestionLogicClient();
    }

    @Bean
    public CacheManager cacheManager() {
        // Use a simple, in-memory cache manager (ConcurrentMapCacheManager)
        // Ensure the cache name is initialized to match the annotation value
        return new ConcurrentMapCacheManager("qAndACache");
    }
}
