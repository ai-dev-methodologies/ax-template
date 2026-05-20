package com.ax.template.authblueprint.notification;

/**
 * Status of a notification entry.
 * <p>
 * Trace: NOTIF-LIST-002 (filter), NOTIF-READ-001 (state transition), NOTIF-SEND-001 (initial state).
 */
public enum NotificationStatus {
    UNREAD,
    READ
}
