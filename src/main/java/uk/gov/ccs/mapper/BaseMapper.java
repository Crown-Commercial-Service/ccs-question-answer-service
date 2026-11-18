package uk.gov.ccs.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rollbar.notifier.Rollbar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.ccs.model.agreements.Dependency;

import java.util.Map;

/**
 * Base mapper class to hold common functionalities.
 */
public abstract class BaseMapper {

    protected static final Logger log = LoggerFactory.getLogger(BaseMapper.class);

    @Autowired
    protected Rollbar rollbar;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Map dependency JSON to Dependency object
     */
    protected Dependency mapDependency(Map<String, Object> dependencyMap) {
        try {
            // Convert Map to Dependency using ObjectMapper
            return objectMapper.convertValue(dependencyMap, Dependency.class);
        } catch (Exception ex) {
            log.error("Error mapping dependency. error {}", ex.getMessage());
            rollbar.warning("Error mapping dependency: " + ex.getMessage());
            return null;
        }
    }
}
