package uk.gov.ccs.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *  Base service class to hold common functionalities.
 */
public abstract class BaseService {

    protected static final Logger log = LoggerFactory.getLogger(BaseService.class);
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected Rollbar rollbar;

    /**
     * Format lot ID (remove "Lot " prefix if present)
     */
    protected String formatLotId(String lotId) {
        if (lotId == null) {
            return null;
        }
        return lotId.replace("Lot ", "").trim();
    }
}
