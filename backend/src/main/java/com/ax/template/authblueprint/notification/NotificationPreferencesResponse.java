package com.ax.template.authblueprint.notification;

public record NotificationPreferencesResponse(
    boolean inAppEnabled,
    boolean emailEnabled
) {
    public static NotificationPreferencesResponse from(NotificationPreferences p) {
        return new NotificationPreferencesResponse(p.isInAppEnabled(), p.isEmailEnabled());
    }
}
