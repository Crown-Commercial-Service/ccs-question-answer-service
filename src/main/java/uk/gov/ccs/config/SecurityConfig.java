package uk.gov.ccs.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uk.gov.ccs.constants.Constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpStatus;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;


/**
 * Spring Security configuration for the API application.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${config.security.api-key}")
    private String apiKey;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Stateless API, no CSRF or sessions.
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Don’t show login pages or use HTTP Basic.
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)

            // Route access.
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/health/**", "/info", "/unauthorised", "/error")
                    .permitAll()
                .anyRequest().authenticated() // Replace ".authenticated()" with ".hasAuthority(Constants.DEFAULT_ROLE)" to check for Roles AND api Key. For now it is only api Key.
            )

            // Custom 401 for unauthenticated access to protected endpoints.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write(Constants.responses_Unauthorised);
                })
            )

            // API key filter. Checks for API Key attached with any calling request, and if provided authenticates the caller.
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

                    String header = request.getHeader(Constants.API_KEY);

                    if (StringUtils.hasText(header) && header.equals(apiKey)) {
                        var auth = new UsernamePasswordAuthenticationToken(
                            Constants.API_KEY,
                            null,
                            List.of(new SimpleGrantedAuthority(Constants.DEFAULT_ROLE))
                        );
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }

                    // We never send 401 from here; we just let Spring's authorisation rules decide later.
                    filterChain.doFilter(request, response);
                }
            }, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}