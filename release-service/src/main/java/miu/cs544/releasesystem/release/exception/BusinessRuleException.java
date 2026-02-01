package miu.cs544.releasesystem.release.exception;

/**
 * Thrown when a business rule prevents an operation (e.g. developer already has an IN_PROCESS task).
 * Results in HTTP 409 Conflict rather than 500.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
