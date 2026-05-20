package com.ax.template.authblueprint.notification;

/**
 * Body for PATCH /api/notifications/preferences.
 * <p>
 * NOTIF-PREF-002 — partial update: only non-null fields are applied.
 */
public record NotificationPreferencesUpdateRequest(
    Boolean inAppEnabled,
    Boolean emailEnabled
) {}
