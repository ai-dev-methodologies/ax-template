package com.ax.template.authblueprint.notification;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a notification lookup fails for the caller — either because the
 * row does not exist OR because it belongs to a different user.
 * <p>
 * Trace: NOTIF-AUTHZ-002 — cross-user IDOR returns 404, not 403, to avoid
 * leaking the existence of another user's row.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Notification not found: " + id);
    }
}
