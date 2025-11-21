package uk.gov.ccs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Core application class - application entry point
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class })
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableFeignClients
public class Main {
    /**
     * Spring Boot initialisation point
     */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}