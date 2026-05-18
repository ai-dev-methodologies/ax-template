/**
 * @ax-template-meta
 * template_id: backend/data/SoftDeleteConfig
 * layer: backend-infrastructure
 * domain: data
 * anchors_rule: soft-delete-only-on-base-entity.md (PRACTICES-PERS-005)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Hibernate ORM 6.4 User Guide — @SQLDelete overrides the DELETE SQL at the JDBC level; the annotation must be placed on the concrete @Entity subclass, not on the @MappedSuperclass"
 *     url: "https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete"
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — @Modifying + @Query can issue bulk UPDATE/DELETE JPQL; for soft-delete restoration use a @Modifying query to set deleted_at = NULL"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html#jpa.modifying-queries"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   This config documents the soft-delete conventions for AI agents and new developers.
 *   SoftDeleteHelper provides utility methods for repositories that need to:
 *   - count all rows including soft-deleted (admin metrics)
 *   - restore a soft-deleted entity
 *   - hard-delete after retention period via a native query
 */
package com.example.app.data;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Utility component for soft-delete operations that fall outside the standard
 * ORM lifecycle managed by {@code @SQLDelete} and {@code @Where}.
 *
 * <h3>Standard path (covered by @SQLDelete + @Where)</h3>
 * <ul>
 *   <li>{@code repository.deleteById(id)} → Hibernate runs UPDATE ... SET deleted_at = NOW()</li>
 *   <li>{@code repository.findAll()} → Hibernate appends AND deleted_at IS NULL automatically</li>
 * </ul>
 *
 * <h3>Non-standard paths (covered by this class)</h3>
 * <ul>
 *   <li>Restore: set deleted_at = NULL on an accidentally-deleted entity</li>
 *   <li>Hard-delete: physically remove rows past the retention window (GDPR/DPA compliance)</li>
 *   <li>Admin count: count all rows including soft-deleted for metrics/dashboards</li>
 * </ul>
 *
 * <p>All restore and hard-delete operations use native SQL to bypass {@code @Where}
 * — JPQL queries on a {@code @Where}-annotated entity cannot reach deleted rows.
 */
@Component
public class SoftDeleteConfig {

    @PersistenceContext
    private EntityManager em;

    /**
     * Restores a soft-deleted row by setting {@code deleted_at = NULL}.
     *
     * <p>Use for admin-path "undelete" operations only. The caller is responsible
     * for verifying that restoration is safe (e.g. unique constraints won't conflict).
     *
     * @param tableName  exact database table name (e.g. "notifications")
     * @param id         primary key of the row to restore
     * @return number of rows updated (0 = row not found or was not deleted)
     */
    @Transactional
    public int restore(String tableName, UUID id) {
        return em.createNativeQuery(
                "UPDATE " + tableName + " SET deleted_at = NULL WHERE id = :id AND deleted_at IS NOT NULL")
                .setParameter("id", id)
                .executeUpdate();
    }

    /**
     * Hard-deletes rows that have been soft-deleted before the retention cutoff.
     *
     * <p>Intended for scheduled GDPR/DPA retention jobs. The {@code retentionCutoffSql}
     * parameter is a SQL timestamp expression (e.g. {@code "NOW() - INTERVAL '90 days'"}).
     *
     * @param tableName         exact database table name
     * @param retentionCutoffSql SQL expression for the cutoff (e.g. "NOW() - INTERVAL '90 days'")
     * @return number of rows hard-deleted
     */
    @Transactional
    public int hardDeleteExpired(String tableName, String retentionCutoffSql) {
        return em.createNativeQuery(
                "DELETE FROM " + tableName
                        + " WHERE deleted_at IS NOT NULL AND deleted_at < " + retentionCutoffSql)
                .executeUpdate();
    }

    /**
     * Counts all rows in a table including soft-deleted rows.
     *
     * <p>Use for admin metrics where total row count (active + deleted) is needed.
     * Standard JPQL {@code COUNT} queries exclude deleted rows via {@code @Where}.
     *
     * @param tableName exact database table name
     * @return total row count including soft-deleted
     */
    public long countAll(String tableName) {
        Number result = (Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM " + tableName)
                .getSingleResult();
        return result.longValue();
    }
}
