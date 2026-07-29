/**
 * @ax-template-meta
 * template_id: backend/notification/NotificationPreferencesRepository
 * layer: backend
 * domain: notification
 * anchors_rule: testing-archunit-repository-shape.md
 * domain_mode: full_trio
 * backend_operation_id: getNotificationPreferences
 * evidence:
 *   - source_type: internal
 *     rationale: "Notification domain — preferences repository. Extends BaseRepository for JPA shape compliance (PRACTICES-TEST-004). findByUserId supports lazy-init pattern: returns empty Optional when no row exists, letting the service fall back to defaults."
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Derived query methods"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.query-methods.query-creation"
 * provenance_class: internal_design
 * imports_from: [backend-cross-cutting]
 * imports_forbidden: [L1, L2, L3, L4]
 */
package com.example.app.notification;

import com.example.app.repositories.BaseRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for per-user notification preferences.
 *
 * <p>Extends {@link BaseRepository} so the ArchUnit shape check passes
 * (PRACTICES-TEST-004: *Repository must extend JpaRepository).
 *
 * <p>Lazy-init pattern (NOTIF-PREF-001):
 * {@link #findByUserId(UUID)} returns {@code Optional.empty()} when no row exists.
 * The service layer creates a row only on the first PATCH (upsert on first write).
 * GET always returns defaults when the row is absent — no row created on read.
 *
 * <p>Operation bindings:
 * <ul>
 *   <li>GET  /api/notifications/preferences → {@code findByUserId(callerUserId)}
 *   <li>PATCH /api/notifications/preferences → {@code findByUserId(callerUserId)} + {@code save()}
 * </ul>
 *
 * <p>Fork instructions:
 * <ol>
 *   <li>Replace {@code com.example.app} with your base package.
 *   <li>Add derived queries for additional preference fields as channels are extended.
 *   <li>Do NOT add a {@code findByUserId(UUID)} returning non-Optional — the lazy-init
 *       pattern depends on empty Optional indicating "no preferences set yet."
 * </ol>
 */
public interface NotificationPreferencesRepository
        extends BaseRepository<NotificationPreferences, UUID> {

    /**
     * Finds preferences by the owning user's ID.
     *
     * <p>Returns {@code Optional.empty()} when no preferences row exists for the user.
     * The service interprets this as "use defaults" (NOTIF-PREF-001 lazy-init).
     *
     * @param userId the owning user's ID (never the notification's recipient field directly)
     * @return the preferences row, or empty if none exists yet
     */
    Optional<NotificationPreferences> findByUserId(UUID userId);
}
