package com.ax.template.authblueprint.announcement;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    /** Admin list — all announcements, newest window first (ANN-LIST-001). */
    List<Announcement> findAllByOrderByStartsAtDesc(Pageable pageable);

    /** Candidate set for the active list — PUBLISHED only; the half-open window is applied
     *  in the service against the injected clock (ANN-WINDOW-001 / ANN-LIST-001). */
    List<Announcement> findByStateOrderByStartsAtDesc(AnnouncementState state);
}
