package uk.gov.ccs.BLL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.gov.ccs.services.SampleService;

/**
 * Processing Layer for sample based data
 */
@Component
public class SampleLogicClient {
    @Autowired
    SampleService sampleService;

    /**
     * Returns a sample string from the service
     */
    @Cacheable(value = "qAndACache", key = "#root.methodName")
    public String getSampleString() {
        return sampleService.getSampleStringFromService();
    }
}