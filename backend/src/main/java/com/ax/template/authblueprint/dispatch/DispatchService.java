package com.ax.template.authblueprint.dispatch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * dispatch sole orchestrator — realizes exclusive-assignment-l0 + timed-offer-l0. All time is read
 * from the injected {@link Clock} (OFFER-TOCTOU-003); the contended claims are atomic conditional
 * UPDATEs in the repositories (EXCL-CLAIM-001); non-contended status edges go through the state
 * machines. {@link #expireOneOffer} is {@code REQUIRES_NEW} so the cross-bean {@link DispatchSweeper}
 * gets a per-row transaction whose @Version-guarded write LOSES to a committed live accept.
 */
@Service
public class DispatchService {

    private final ProviderRepository providerRepo;
    private final ServiceRequestRepository requestRepo;
    private final OfferRepository offerRepo;
    private final ProviderStateMachine providerSm;
    private final ServiceRequestStateMachine requestSm;
    private final OfferStateMachine offerSm;
    private final DispatchMetrics metrics;
    private final Clock clock;

    private final long offerTtlSeconds;
    private final long stalenessWindowSeconds;
    private final int maxCascadeDepth;

    public DispatchService(ProviderRepository providerRepo, ServiceRequestRepository requestRepo,
                           OfferRepository offerRepo, ProviderStateMachine providerSm,
                           ServiceRequestStateMachine requestSm, OfferStateMachine offerSm,
                           DispatchMetrics metrics, Clock clock,
                           @Value("${dispatch.offer-ttl-seconds:120}") long offerTtlSeconds,
                           @Value("${dispatch.staleness-window-seconds:60}") long stalenessWindowSeconds,
                           @Value("${dispatch.max-cascade-depth:5}") int maxCascadeDepth) {
        this.providerRepo = providerRepo;
        this.requestRepo = requestRepo;
        this.offerRepo = offerRepo;
        this.providerSm = providerSm;
        this.requestSm = requestSm;
        this.offerSm = offerSm;
        this.metrics = metrics;
        this.clock = clock;
        this.offerTtlSeconds = offerTtlSeconds;
        this.stalenessWindowSeconds = stalenessWindowSeconds;
        this.maxCascadeDepth = maxCascadeDepth;
    }

    // ── supply side ───────────────────────────────────────────────────────────
    @Transactional
    public Provider registerProvider(String handle) {
        Instant now = Instant.now(clock);
        return providerRepo.save(new Provider(UUID.randomUUID(), handle, ProviderStatus.AVAILABLE, now, now));
    }

    @Transactional
    public Provider heartbeat(UUID providerId) {
        Provider p = providerRepo.findById(providerId).orElseThrow(DispatchException::notFound);
        p.setLastHeartbeatAt(Instant.now(clock));
        return p;
    }

    // ── demand side ───────────────────────────────────────────────────────────
    @Transactional
    public ServiceRequest createRequest(String createdBy, String description) {
        Instant now = Instant.now(clock);
        return requestRepo.save(new ServiceRequest(UUID.randomUUID(), description, createdBy, now));
    }

    @Transactional
    public ServiceRequest cancelRequest(UUID id) {
        ServiceRequest r = requestRepo.findById(id).orElseThrow(DispatchException::notFound);
        requestSm.cancel(r);   // throws invalidTransition (409) if ASSIGNED/terminal
        metrics.record("cancel", "ok");
        return r;
    }

    // ── dispatcher: offer a request to a chosen provider ────────────────────────
    @Transactional
    public Offer offer(UUID requestId, UUID providerId) {
        // OFFER-FSM-001 — lock the request row so concurrent offers for it serialize (the
        // at-most-one-PENDING check below is then race-free). The V034 partial unique index is the
        // independent DB backstop for Flyway fork-receivers; the reference runs on create-drop, so
        // application-side serialization is what enforces the invariant here.
        ServiceRequest r = requestRepo.findByIdForUpdate(requestId).orElseThrow(DispatchException::notFound);
        if (offerRepo.findFirstByRequestIdAndStatus(requestId, OfferStatus.PENDING).isPresent()) {
            metrics.record("offer", "rejected");
            throw DispatchException.duplicatePendingOffer();
        }
        if (r.getStatus() == ServiceRequestStatus.PENDING) {
            requestSm.offer(r);   // PENDING -> OFFERED
        } else if (r.getStatus() != ServiceRequestStatus.OFFERED) {
            metrics.record("offer", "rejected");
            throw DispatchException.invalidTransition(r.getStatus().name(), ServiceRequestStatus.OFFERED.name());
        }
        Offer o = createOfferTo(r.getId(), providerId);
        metrics.record("offer", "ok");
        return o;
    }

    // ── provider acts on its offer ──────────────────────────────────────────────
    @Transactional
    public Offer acceptOffer(UUID offerId) {
        Offer o = offerRepo.findById(offerId).orElseThrow(DispatchException::notFound);
        Instant now = Instant.now(clock);
        // OFFER-TOCTOU-003 — re-check PENDING + deadline INSIDE the accept transaction.
        if (o.getStatus() != OfferStatus.PENDING) {
            metrics.record("accept", "rejected");
            throw DispatchException.invalidTransition(o.getStatus().name(), OfferStatus.ACCEPTED.name());
        }
        if (!now.isBefore(o.getExpiresAt())) {
            metrics.record("accept", "expired");
            throw DispatchException.offerExpired();
        }
        UUID requestId = o.getRequestId();
        UUID providerId = o.getProviderId();
        // EXCL-PAIR-002 — deterministic order: claim the REQUEST first, then the PROVIDER.
        // EXCL-CLAIM-001 — each is one atomic status-guarded conditional UPDATE (affected-rows==1 wins).
        int reqClaimed = requestRepo.claim(requestId, providerId,
            ServiceRequestStatus.OFFERED, ServiceRequestStatus.ASSIGNED);
        if (reqClaimed == 0) {
            metrics.record("accept", "job_taken");
            throw DispatchException.jobAlreadyTaken();               // EXCL-409-004
        }
        int provClaimed = providerRepo.claim(providerId,
            ProviderStatus.AVAILABLE, ProviderStatus.ASSIGNED);
        if (provClaimed == 0) {
            metrics.record("accept", "driver_busy");
            throw DispatchException.driverAlreadyBusy();             // rolls back the request claim (same tx)
        }
        Offer fresh = offerRepo.findById(offerId).orElseThrow(DispatchException::notFound);  // PC was cleared by the claims
        offerSm.accept(fresh);
        metrics.record("accept", "ok");
        return fresh;
    }

    @Transactional
    public Offer declineOffer(UUID offerId) {
        UUID requestId = offerRepo.findById(offerId).orElseThrow(DispatchException::notFound).getRequestId();
        // Lock the REQUEST first (consistent lock order with acceptOffer: request before offer) so a
        // decline and a concurrent accept on the same request cannot deadlock.
        ServiceRequest r = requestRepo.findByIdForUpdate(requestId).orElseThrow(DispatchException::notFound);
        Offer o = offerRepo.findById(offerId).orElseThrow(DispatchException::notFound);   // re-read under the lock
        if (o.getStatus() != OfferStatus.PENDING) {
            metrics.record("decline", "rejected");
            throw DispatchException.invalidTransition(o.getStatus().name(), OfferStatus.DECLINED.name());
        }
        offerSm.decline(o);
        offerRepo.flush();              // demotion UPDATE hits the DB BEFORE the next-offer INSERT
        advanceToNextCandidate(r, o);   // OFFER-ATOMIC-002 — terminate + re-offer in ONE transaction
        metrics.record("decline", "ok");
        return o;
    }

    // ── AVAIL-SWEEP-001 — per-row sweep handler (its OWN transaction, cross-bean from sweeper) ──
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expireOneOffer(UUID offerId) {
        Offer probe = offerRepo.findById(offerId).orElse(null);
        if (probe == null) return;                                      // idempotent: already gone
        if (probe.getStatus() != OfferStatus.PENDING) return;           // a live accept/decline won — skip
        if (Instant.now(clock).isBefore(probe.getExpiresAt())) return;  // re-check: not actually due
        // Lock the REQUEST first (consistent order with acceptOffer) to avoid an offer<->request
        // deadlock with a concurrent accept; a live accept holding the request lock makes this block,
        // then the re-read below sees the now-non-PENDING offer and the sweep cleanly LOSES (skips).
        ServiceRequest r = requestRepo.findByIdForUpdate(probe.getRequestId()).orElse(null);
        if (r == null) return;
        Offer o = offerRepo.findById(offerId).orElse(null);             // re-read under the request lock
        if (o == null || o.getStatus() != OfferStatus.PENDING
            || Instant.now(clock).isBefore(o.getExpiresAt())) return;   // a live accept committed in the gap
        offerSm.expire(o);              // @Version-guarded write — defense-in-depth backstop
        offerRepo.flush();             // EXPIRED UPDATE hits the DB BEFORE the next-offer INSERT
        advanceToNextCandidate(r, o);  // re-offer next, same tx
        metrics.record("expire", "ok");
    }

    // ── read side ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ServiceRequest getRequest(UUID id) {
        return requestRepo.findById(id).orElseThrow(DispatchException::notFound);
    }

    @Transactional(readOnly = true)
    public Provider getProvider(UUID id) {
        return providerRepo.findById(id).orElseThrow(DispatchException::notFound);
    }

    @Transactional(readOnly = true)
    public Offer getOffer(UUID id) {
        return offerRepo.findById(id).orElseThrow(DispatchException::notFound);
    }

    /** The current outstanding PENDING offer for a request (lets a caller follow the cascade). */
    @Transactional(readOnly = true)
    public Offer currentPendingOffer(UUID requestId) {
        return offerRepo.findFirstByRequestIdAndStatus(requestId, OfferStatus.PENDING)
            .orElseThrow(DispatchException::notFound);
    }

    /** AVAIL-SWEEP-001 — bounded batch of due PENDING offer ids (the sweeper loops over these). */
    @Transactional(readOnly = true)
    public List<UUID> dueOfferIds(int batchSize) {
        return offerRepo.findDueOfferIds(OfferStatus.PENDING, Instant.now(clock), PageRequest.of(0, batchSize));
    }

    // ── internals ─────────────────────────────────────────────────────────────
    private Offer createOfferTo(UUID requestId, UUID providerId) {
        Provider p = providerRepo.findById(providerId).orElseThrow(DispatchException::notFound);
        Instant now = Instant.now(clock);
        // AVAIL-FRESH-002 — offerable only while AVAILABLE AND heartbeat-fresh.
        if (p.getStatus() != ProviderStatus.AVAILABLE
            || !p.isFresh(now, Duration.ofSeconds(stalenessWindowSeconds))) {
            metrics.record("offer", "not_eligible");
            throw DispatchException.providerNotEligible();
        }
        int ordinal = (int) offerRepo.countByRequestId(requestId) + 1;   // OFFER-CASCADE-004 monotonic
        return offerRepo.save(new Offer(UUID.randomUUID(), requestId, providerId,
            now.plusSeconds(offerTtlSeconds), ordinal, now));
    }

    /**
     * OFFER-ATOMIC-002 / OFFER-CASCADE-004 — advance to the next ranked candidate, or exhaust.
     * The request {@code r} is ALREADY locked (FOR UPDATE) by the caller and the prior offer is
     * already demoted-and-flushed, so the partial unique index sees at most one PENDING and a
     * concurrent manual offer() for the same request is serialized behind the lock.
     */
    private void advanceToNextCandidate(ServiceRequest r, Offer terminated) {
        if (r.getStatus() != ServiceRequestStatus.OFFERED) return;  // already resolved
        if (terminated.getOrdinal() >= maxCascadeDepth) {
            requestSm.markUnfulfilled(r);
            return;
        }
        Instant now = Instant.now(clock);
        List<UUID> tried = offerRepo.findProviderIdsByRequestId(r.getId(), PageRequest.of(0, 100));
        List<UUID> eligible = providerRepo.findEligibleIds(ProviderStatus.AVAILABLE,
            now.minus(Duration.ofSeconds(stalenessWindowSeconds)), PageRequest.of(0, 50));
        UUID next = eligible.stream().filter(id -> !tried.contains(id)).findFirst().orElse(null);
        if (next == null) {
            requestSm.markUnfulfilled(r);   // no candidate left
            return;
        }
        int ordinal = terminated.getOrdinal() + 1;   // strictly monotonic; no COUNT read-then-write race
        offerRepo.save(new Offer(UUID.randomUUID(), r.getId(), next,
            now.plusSeconds(offerTtlSeconds), ordinal, now));
    }
}
