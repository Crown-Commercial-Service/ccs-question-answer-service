package uk.gov.ccs.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import java.util.concurrent.CompletionException;

/**
 * Global error handler for the application - will handle any errors that we don't explicitly manage within other routes
 */
@ControllerAdvice
public class GlobalErrorHandler extends BaseController {
    /**
     * Error handler for 404 Not Found requests
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public String handleRouteNotFound(HttpServletRequest request, HttpServletResponse response)   {
        // This is a 404 Route Not Found error
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        rollbar.warning("Route not found: " + request.getRequestURI());

        return Constants.responses_NotFound;
    }

    /**
     * Handles errors thrown from within an async method, these need to be checked separately
     */
    @ExceptionHandler(CompletionException.class)
    public String handleAsyncExceptions(CompletionException ex, HttpServletResponse response) {
        Throwable exceptionCause = ex.getCause();

        // Handle as a general exception
        rollbar.error(exceptionCause, "Unexpected error occurred within async request");
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        return Constants.responses_Error;
    }

    /**
     * Handles validation errors from @Valid annotation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
        rollbar.warning("Validation error: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Validation error: " + ex.getMessage());
    }

    /**
     * Handles JSON parsing/deserialization errors
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleMessageNotReadableException(HttpMessageNotReadableException ex) {
        rollbar.warning("Invalid request body: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Invalid request body: " + ex.getMostSpecificCause().getMessage());
    }

    /**
     * Any non-404 errors should be handled as part of other Controller Actions - so we shouldn't need anything else explicitly mapping
     * Just incase though, this is a generic error handler to catch anything else we don't explicitly handle
     */
    @ExceptionHandler(Exception.class)
    public String handleUnexpectedError(HttpServletResponse response, Exception ex)   {
        // We don't know why the user got here, so we need to log this as an error to investigate
        rollbar.error(ex, "Unexpected error occurred");

        // Return a 500 response as we don't know what's happened
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Constants.responses_Error;
    }
}