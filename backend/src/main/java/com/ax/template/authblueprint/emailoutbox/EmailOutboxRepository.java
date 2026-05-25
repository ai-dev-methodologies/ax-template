package com.ax.template.authblueprint.emailoutbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, UUID> {

    /**
     * EMAIL-SEND-001 + EMAIL-RETRY-002 — return rows that are due to be
     * sent now: PENDING (no nextAttemptAt set) OR RETRY with nextAttemptAt
     * &le; now. DLQ and SENT are terminal and never re-attempted.
     */
    @Query("""
        SELECT e FROM EmailOutbox e
        WHERE e.status = com.ax.template.authblueprint.emailoutbox.EmailOutboxStatus.PENDING
           OR (e.status = com.ax.template.authblueprint.emailoutbox.EmailOutboxStatus.RETRY
               AND e.nextAttemptAt <= :now)
        ORDER BY e.createdAt ASC
        """)
    List<EmailOutbox> findDueForSending(@Param("now") Instant now);

    Page<EmailOutbox> findByStatus(EmailOutboxStatus status, Pageable pageable);

    Page<EmailOutbox> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
