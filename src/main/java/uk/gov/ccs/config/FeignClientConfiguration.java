package uk.gov.ccs.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 *  This is the temporary Configuration object to print full log for Feign client.
 *
 *  <ul>
 *      <li>
 *          This is only for debug purposes, once everything work we should remove this class.
 *      </li>
 *  </ul>
 */

// TODO Remove this class once debug completed in dev.

@Configuration
public class FeignClientConfiguration {
    /**
     * Configures Feign to log the full request and response details.
     * Setting this to FULL will print the URL for every request.
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor feignRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                System.out.println("Feign OUTBOUND METHOD: " + template.method());
                System.out.println("Feign OUTBOUND URL: " + template.url());
            }
        };
    }
}
