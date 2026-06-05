package com.ax.template.authblueprint.softdelete;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link SoftDeleteAccount}. Default-excluding reads use the explicit
 * {@code ...AndDeletedAtIsNull} finders (SOFTDELETE-QUERY-001); the unfiltered finders serve the
 * restore / admin / purge paths that MUST see tombstoned rows.
 */
public interface SoftDeleteAccountRepository extends JpaRepository<SoftDeleteAccount, UUID> {

    // ── default-excluding (normal callers) ──
    Optional<SoftDeleteAccount> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, String ownerId);

    List<SoftDeleteAccount> findByOwnerIdAndDeletedAtIsNull(String ownerId);

    boolean existsByOwnerIdAndEmailAndDeletedAtIsNull(String ownerId, String email);

    // ── unfiltered (restore / admin / purge — must see tombstoned rows) ──
    Optional<SoftDeleteAccount> findByIdAndOwnerId(UUID id, String ownerId);

    List<SoftDeleteAccount> findByOwnerId(String ownerId);

    List<SoftDeleteAccount> findByDeletedAtIsNotNullAndDeletedAtBefore(Instant cutoff);
}
