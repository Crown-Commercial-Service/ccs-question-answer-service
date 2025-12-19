package uk.gov.ccs.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tika.utils.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uk.gov.ccs.constants.Constants;
import uk.gov.ccs.controllers.BaseController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

/**
 * Global error handler for the application - will handle any errors that we don't explicitly manage within other routes
 */
@ControllerAdvice
public class GlobalErrorHandler extends BaseController {
    /**
     * Error handler for 404 Not Found requests
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<Object> handleRouteNotFound(HttpServletRequest request, HttpServletResponse response)   {
        // This is a 404 Route Not Found error
        String uriPath = request.getRequestURI();
        log.error("Route not found: ", uriPath);
        rollbar.warning("Route not found: " + uriPath);
        return new ResponseEntity<>(createErrorResponseBody(HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "The requested resource or endpoint does not exist.",
                uriPath), HttpStatus.NOT_FOUND);
    }

    /**
     * Handles errors thrown from within an async method, these need to be checked separately
     */
    @ExceptionHandler(CompletionException.class)
    public ResponseEntity<Object> handleAsyncExceptions(CompletionException ex, HttpServletResponse response) {
        Throwable exceptionCause = ex.getCause();
        // Handle as a general exception
        log.error("Unexpected error occurred within async request, error: {}", ex.getMessage());
        rollbar.error(exceptionCause, "Unexpected error occurred within async request");

        return new ResponseEntity<>(createErrorResponseBody(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Unexpected error occurred within async request",
                null),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handles validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        rollbar.warning("Validation error: " + ex.getMessage());
        return new ResponseEntity<>(createErrorResponseBody(HttpStatus.BAD_REQUEST.value(),
                "Validation error", ex.getMessage(), null),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles JSON parsing/deserialization errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("Invalid request body. error: {}", ex.getMessage());
        rollbar.warning("Invalid request body: " + ex.getMessage());
        return new ResponseEntity<>(createErrorResponseBody(HttpStatus.BAD_REQUEST.value(),
                "Invalid request body", ex.getMessage(), null),
                HttpStatus.BAD_REQUEST);
    }

    /**
     * Any non-404 errors should be handled as part of other Controller Actions - so we shouldn't need anything else explicitly mapping
     * Just incase though, this is a generic error handler to catch anything else we don't explicitly handle
     */
    @ExceptionHandler(Exception.class)
    public String handleUnexpectedError(HttpServletResponse response, Exception ex)   {

        log.error("Unexpected error occurred. error: {}", ex.getMessage());
        // We don't know why the user got here, so we need to log this as an error to investigate
        rollbar.error(ex, "Unexpected error occurred");

        // Return a 500 response as we don't know what's happened
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Constants.responses_Error;
    }

    private Map<String, Object> createErrorResponseBody(int serverError,
                                                        String error,
                                                        String errorMessage,
                                                        String uriPath) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", serverError);
        body.put("error", error);
        body.put("message", errorMessage);

        if(!StringUtils.isBlank(uriPath)) {
            body.put("path", uriPath);
        }

        return body;
    }
}