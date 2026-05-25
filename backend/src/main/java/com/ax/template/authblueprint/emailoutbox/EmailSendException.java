package com.ax.template.authblueprint.emailoutbox;

/**
 * Thrown by {@link EmailSenderService#send} on either transient or
 * permanent send failure. The catalog policy treats all failures
 * uniformly: increment retryCount, exponential backoff until
 * MAX_RETRIES, then DLQ. Operators triage DLQ via the admin surface.
 */
public class EmailSendException extends Exception {
    public EmailSendException(String message) { super(message); }
    public EmailSendException(String message, Throwable cause) { super(message, cause); }
}
