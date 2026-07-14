package com.ax.template.authblueprint.geofence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * NO delete method is declared anywhere in this domain — a confirmed transition is permanent
 * audit evidence. {@link GeofenceTransition} rows are members (HG-AGG-REPO): they own no
 * repository — reads are root-JPQL here, writes go through {@code common/MemberWriter}.
 */
public interface GeofenceTrackerRepository extends JpaRepository<GeofenceTracker, UUID> {

    Optional<GeofenceTracker> findBySubjectIdAndZoneId(String subjectId, String zoneId);

    // ── through-root member reads (HG-AGG-REPO — GeofenceTransition owns no repository) ──

    @Query("SELECT t FROM GeofenceTransition t WHERE t.trackerId = :trackerId ORDER BY t.confirmedAt ASC")
    List<GeofenceTransition> findTransitionsByTrackerId(@Param("trackerId") UUID trackerId, Pageable pageable);
}
