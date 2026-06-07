package com.ax.template.authblueprint.announcement;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * announcement-l0 service — sole orchestrator. Visibility is DERIVED from the injected
 * {@link Clock} (ANN-WINDOW-001); state is advanced only through {@link AnnouncementStateMachine}
 * (ANN-LIFECYCLE-001); the author is the caller (ANN-AUTHZ-001, set by the controller from
 * Authentication.getName()); {@link AnnouncementMetrics} records each transition (ANN-OBSERVABILITY-001).
 */
@Service
public class AnnouncementService {

    private final AnnouncementRepository repo;
    private final AnnouncementStateMachine stateMachine;
    private final AnnouncementMetrics metrics;
    private final Clock clock;

    public AnnouncementService(AnnouncementRepository repo, AnnouncementStateMachine stateMachine,
                               AnnouncementMetrics metrics, Clock clock) {
        this.repo = repo;
        this.stateMachine = stateMachine;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public Announcement create(String author, String title, String body, Instant startsAt, Instant endsAt) {
        if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
            metrics.record("created", "rejected");
            throw AnnouncementException.invalidWindow();   // ANN-VALIDATION-001 (400)
        }
        Announcement a = new Announcement(UUID.randomUUID(), title, body, startsAt, endsAt,
            author, Instant.now(clock));
        Announcement saved = repo.save(a);
        metrics.record("created", "ok");
        return saved;
    }

    @Transactional
    public Announcement publish(UUID id) {
        Announcement a = repo.findById(id).orElseThrow(AnnouncementException::notFound);
        try {
            stateMachine.publish(a);                       // ANN-LIFECYCLE-001
        } catch (AnnouncementException e) {
            metrics.record("published", "rejected");
            throw e;
        }
        metrics.record("published", "ok");
        return a;
    }

    @Transactional
    public Announcement archive(UUID id) {
        Announcement a = repo.findById(id).orElseThrow(AnnouncementException::notFound);
        try {
            stateMachine.archive(a);
        } catch (AnnouncementException e) {
            metrics.record("archived", "rejected");
            throw e;
        }
        metrics.record("archived", "ok");
        return a;
    }

    /** ANN-LIST-001 — only currently-active (PUBLISHED + within window at the injected now). */
    @Transactional(readOnly = true)
    public List<Announcement> listActive() {
        Instant now = Instant.now(clock);
        return repo.findByStateOrderByStartsAtDesc(AnnouncementState.PUBLISHED).stream()
            .filter(a -> a.isActiveAt(now))
            .toList();
    }

    /** ANN-LIST-001 — admin: all announcements regardless of state/window. */
    @Transactional(readOnly = true)
    public List<Announcement> listAll() {
        return repo.findAllByOrderByStartsAtDesc();
    }

    @Transactional(readOnly = true)
    public Announcement get(UUID id) {
        return repo.findById(id).orElseThrow(AnnouncementException::notFound);
    }
}
