package com.ax.template.authblueprint.notification;

import org.springframework.stereotype.Component;

/**
 * Default in-app channel — the persistence of the {@link Notification} itself
 * is the in-app "delivery". This adapter is a no-op, but it exists so the
 * dispatcher invokes channels uniformly and so it can be replaced with a
 * push-style fan-out (SSE, WebSocket) without changing the service contract.
 */
@Component
public class InAppNotificationChannel implements NotificationChannel {

    @Override
    public String id() { return "in-app"; }

    @Override
    public void deliver(Notification notification) {
        // No-op: persistence == delivery for the in-app channel.
    }

    @Override
    public boolean enabledFor(NotificationPreferences prefs) {
        return prefs == null || prefs.isInAppEnabled();
    }
}
