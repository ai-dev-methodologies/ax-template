package com.ax.template.authblueprint.dispatch;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {

    /**
     * EXCL-CLAIM-001 — the exclusive claim as ONE atomic status-guarded conditional UPDATE.
     * affected-rows == 1 ⇒ this caller won the provider; affected-rows == 0 ⇒ the provider was no
     * longer AVAILABLE (lost the race → DRIVER_ALREADY_BUSY). The version bump is explicit because a
     * bulk JPQL UPDATE does not auto-manage {@code @Version}. clearAutomatically evicts the stale
     * managed copy so a subsequent findById re-reads the committed row.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Provider p SET p.status = :assigned, p.version = p.version + 1 "
        + "WHERE p.id = :id AND p.status = :available")
    int claim(@Param("id") UUID id,
              @Param("available") ProviderStatus available,
              @Param("assigned") ProviderStatus assigned);

    /** AVAIL-FRESH-002 — offerable = AVAILABLE AND heartbeat within the staleness window. Bounded. */
    @Query("SELECT p.id FROM Provider p WHERE p.status = :available AND p.lastHeartbeatAt > :threshold "
        + "ORDER BY p.lastHeartbeatAt DESC")
    List<UUID> findEligibleIds(@Param("available") ProviderStatus available,
                               @Param("threshold") Instant threshold,
                               Pageable pageable);
}
