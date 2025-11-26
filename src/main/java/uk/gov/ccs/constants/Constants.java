package uk.gov.ccs.constants;

/**
 * Class designed to hold all constants for the application - to avoid needing to repeat values constantly throughout code
 */
public final class Constants {
    // Security Config Constants
    public static String API_KEY = "x-api-key";
    public static String DEFAULT_ROLE = "API_CLIENT";
    public static String responses_Unauthorised = "{\"error\":\"Missing or invalid API key\"}";

    // Route response constants
    public static String responses_Success = "OK";
    public static String responses_NotFound = "Route not found";
    public static String responses_Error = "Unexpected error occurred";
}