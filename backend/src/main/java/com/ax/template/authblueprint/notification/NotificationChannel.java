package com.ax.template.authblueprint.notification;

/**
 * Channel adapter contract. NOTIF-SEND-002 — every channel is invoked in a
 * try/catch so a single channel failure does not abort delivery to peers and
 * does not fail the originating HTTP request.
 */
public interface NotificationChannel {

    /** Stable identifier (e.g. "in-app", "email"). */
    String id();

    /**
     * Attempt delivery. Implementations may throw — the dispatcher will catch
     * and log without surfacing to the caller (NOTIF-SEND-002).
     */
    void deliver(Notification notification);

    /**
     * Whether this channel is enabled for the given user's preferences. The
     * dispatcher consults this before invoking {@link #deliver(Notification)}.
     */
    boolean enabledFor(NotificationPreferences prefs);
}
