/**
 * @ax-template-meta
 * template_id: backend/notification/NotificationPreferences
 * layer: backend-domain
 * domain: notification
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 *   - source_type: external
 *     citation: "OWASP ASVS V4.2.1 — Access control verifies user owns the resource"
 *     url: "https://owasp.org/www-project-application-security-verification-standard/"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   NotificationPreferences is keyed by userId (one row per user).
 *   Lazy-init: row is created only on first PATCH, not on first GET.
 *   userId is NEVER exposed in the URL path — always resolved from JWT principal.
 */
package com.example.app.notification;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;

/**
 * Per-user notification channel preferences.
 *
 * <p>Isolation: {@code userId} is resolved from the JWT principal, never from
 * a URL path parameter. Cross-user access is prevented by design (NOTIF-AUTHZ-003).
 *
 * <p>Lazy-init policy: this row is created only on the first PATCH. A GET with no
 * existing row returns the default values without inserting a row.
 *
 * <p>Extends {@code BaseEntity} for: id (UUID), createdAt, updatedAt, deleted.
 */
@Entity
@SQLDelete(sql = "UPDATE notification_preferences SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "notification_preferences")
public class NotificationPreferences extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    /** When true, in-app notification delivery is active for this user. */
    @Column(name = "in_app_enabled", nullable = false)
    private boolean inAppEnabled = true;

    /** When true, email delivery is active for this user (subject to emailEnabled in preferences). */
    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled = true;

    protected NotificationPreferences() {
        // JPA
    }

    /**
     * Factory constructor — creates a new preferences row with supplied values.
     *
     * <p>Use {@code NotificationDto.PreferencesResponse.defaults(userId)} for the GET
     * fallback when no row exists (no row is inserted by the GET path).
     */
    public static NotificationPreferences create(UUID userId, boolean inAppEnabled, boolean emailEnabled) {
        var prefs = new NotificationPreferences();
        prefs.userId = userId;
        prefs.inAppEnabled = inAppEnabled;
        prefs.emailEnabled = emailEnabled;
        return prefs;
    }

    /**
     * Applies a partial update from the request DTO.
     * Null fields are ignored (PATCH semantics).
     */
    public void applyPartialUpdate(NotificationDto.UpdatePreferencesRequest req) {
        if (req.inAppEnabled() != null) {
            this.inAppEnabled = req.inAppEnabled();
        }
        if (req.emailEnabled() != null) {
            this.emailEnabled = req.emailEnabled();
        }
    }

    public UUID getUserId()          { return userId; }
    public boolean isInAppEnabled()  { return inAppEnabled; }
    public boolean isEmailEnabled()  { return emailEnabled; }
    public Instant getUpdatedAt()    { return super.getUpdatedAt(); }
}
