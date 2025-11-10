package uk.gov.ccs.services;

import com.rollbar.notifier.Rollbar;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to read in and process sample data and produce models representing its contents
 */
@Service
public class SampleService {
    @Autowired
    Rollbar rollbar;

    /**
     * Sample data fetch from service (this would normally be from DB)
     */
    public String getSampleStringFromService() {
        try {
            return "testValue";
        } catch (Exception ex) {
            rollbar.error(ex, "Error returning sample value");
        }

        return null;
    }
}