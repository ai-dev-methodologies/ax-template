package com.ax.template.authblueprint.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Per-user notification channel preferences.
 * <p>
 * Trace: NOTIF-PREF-001 (defaults), NOTIF-PREF-002 (partial update).
 * Keyed by userId (no surrogate key) — one row per user max.
 */
@AggregateRoot
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferences {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    /** Required by JPA. */
    protected NotificationPreferences() {}

    public NotificationPreferences(String userId, boolean inAppEnabled, boolean emailEnabled) {
        this.userId = userId;
        this.inAppEnabled = inAppEnabled;
        this.emailEnabled = emailEnabled;
    }

    /** Defaults per NOTIF-PREF-001 — all channels enabled. */
    public static NotificationPreferences defaultsFor(String userId) {
        return new NotificationPreferences(userId, true, true);
    }

    public String getUserId() { return userId; }
    public boolean isInAppEnabled() { return inAppEnabled; }
    public boolean isEmailEnabled() { return emailEnabled; }

    /** NOTIF-PREF-002 — partial update (only non-null fields applied). */
    public void apply(Boolean inAppEnabled, Boolean emailEnabled) {
        if (inAppEnabled != null) this.inAppEnabled = inAppEnabled;
        if (emailEnabled != null) this.emailEnabled = emailEnabled;
    }
}
