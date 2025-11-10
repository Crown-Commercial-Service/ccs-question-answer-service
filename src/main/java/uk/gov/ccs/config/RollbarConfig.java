package uk.gov.ccs.config;

import com.rollbar.notifier.Rollbar;
import com.rollbar.notifier.config.Config;
import com.rollbar.spring.webmvc.RollbarSpringConfigBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rollbar logging configuration for the application, setup once for the app
 */
@Configuration
public class RollbarConfig {
    @Value("${rollbar.accessToken}")
    private String rollbarAccessToken;

    @Value("${rollbar.env}")
    private String rollbarEnv;

    /**
     * Register a Rollbar instance for the app to use
     */
    @Bean
    public Rollbar setupRollbar() {
        return new Rollbar(getRollbarConfig());
    }

    /**
     * Build a Config item comprised of the Rollbar configuration details
     */
    private Config getRollbarConfig() {
        return RollbarSpringConfigBuilder.withAccessToken(rollbarAccessToken)
                .environment(rollbarEnv)
                .build();
    }
}