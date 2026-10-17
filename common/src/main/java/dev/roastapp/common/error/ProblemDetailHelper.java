package dev.roastapp.common.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

/**
 * Standardized error responses using RFC 7807 Problem Detail.
 * Every service uses this helper to ensure a consistent error format.
 */
public final class ProblemDetailHelper {

    private ProblemDetailHelper() {
        // utility class
    }

    public static ProblemDetail create(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now().toString());
        return problem;
    }

    public static ProblemDetail notFound(String resourceName, Object id) {
        return create(HttpStatus.NOT_FOUND, resourceName + " Not Found",
                resourceName + " with id '" + id + "' was not found.");
    }

    public static ProblemDetail badRequest(String detail) {
        return create(HttpStatus.BAD_REQUEST, "Bad Request", detail);
    }

    public static ProblemDetail conflict(String detail) {
        return create(HttpStatus.CONFLICT, "Conflict", detail);
    }

    public static ProblemDetail tooManyRequests(String detail, long retryAfterSeconds) {
        ProblemDetail problem = create(HttpStatus.TOO_MANY_REQUESTS, "Rate Limit Exceeded", detail);
        problem.setProperty("retryAfterSeconds", retryAfterSeconds);
        return problem;
    }

    public static ProblemDetail internal(String detail) {
        return create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", detail);
    }
}
