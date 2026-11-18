package uk.gov.ccs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Core application class - application entry point
 */
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class })
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