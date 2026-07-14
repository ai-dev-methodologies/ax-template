package com.ax.template.authblueprint.inputplausibility;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * PLAUSIBILITY-DATE-RANGE/FUTURE-001 sole orchestrator. A DATE-typed self-reported asserted
 * fact (e.g. a claimed loss/event date) is validated against the channel's configured
 * [reference - maxLookback, reference + maxLookahead] window, where the reference instant is
 * ALWAYS the injected {@link Clock} — never wall-clock. A passing submission is appended as a
 * {@link DatePlausibilityReading} marked SELF_REPORTED_UNVERIFIED (same provenance contract as
 * the numeric channel); a failing submission is recorded as an immutable
 * {@link DateRejectedAttempt} BEFORE the 422 is returned — never silently dropped.
 */
@Service
public class DatePlausibilityService {

    private final DatePlausibilityChannelRepository channels;
    private final MemberWriter members;
    private final DateRejectedAttemptRecorder rejections;
    private final PlausibilityMetrics metrics;
    private final Clock clock;

    public DatePlausibilityService(DatePlausibilityChannelRepository channels, MemberWriter members,
                                   DateRejectedAttemptRecorder rejections, PlausibilityMetrics metrics, Clock clock) {
        this.channels = channels;
        this.members = members;
        this.rejections = rejections;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public DatePlausibilityChannel define(String subjectRef, long maxLookbackSeconds, long maxLookaheadSeconds) {
        if (subjectRef == null || subjectRef.isBlank()) {
            metrics.record("dateDefine", "invalid");
            throw PlausibilityException.invalidChannel("subjectRef must not be blank");
        }
        if (maxLookbackSeconds < 0 || maxLookaheadSeconds < 0) {
            metrics.record("dateDefine", "invalid");
            throw PlausibilityException.invalidChannel("maxLookbackSeconds/maxLookaheadSeconds must be >= 0");
        }
        DatePlausibilityChannel c = new DatePlausibilityChannel(UUID.randomUUID(), subjectRef,
            maxLookbackSeconds, maxLookaheadSeconds, Instant.now(clock));
        DatePlausibilityChannel saved = channels.save(c);
        metrics.record("dateDefine", "ok");
        return saved;
    }

    /**
     * PLAUSIBILITY-DATE-RANGE-001 / PLAUSIBILITY-DATE-FUTURE-001 — plausibility-gate one
     * DATE-typed submission against the reference instant (the injected Clock's now). The
     * window bound is INCLUSIVE at both edges, so an asserted date exactly at the tolerance
     * passes and one second beyond it fails (fail-closed).
     */
    @Transactional
    public DatePlausibilityReading submit(UUID channelId, Instant assertedAt, String actor) {
        DatePlausibilityChannel c = channels.findById(channelId).orElseThrow(PlausibilityException::notFound);
        Instant referenceAt = Instant.now(clock);

        if (!c.inWindow(assertedAt, referenceAt)) {
            rejections.record(c.getId(), assertedAt, referenceAt, DateRejectReason.IMPLAUSIBLE_DATE_RANGE, actor);
            metrics.record("dateSubmit", "implausible_date_range");
            throw PlausibilityException.implausibleDateRange();
        }

        DatePlausibilityReading reading = members.persist(new DatePlausibilityReading(UUID.randomUUID(),
            c.getId(), assertedAt, referenceAt, actor, referenceAt));
        metrics.record("dateSubmit", "accepted");
        return reading;
    }

    @Transactional(readOnly = true)
    public DatePlausibilityChannel get(UUID channelId) {
        return channels.findById(channelId).orElseThrow(PlausibilityException::notFound);
    }

    @Transactional(readOnly = true)
    public List<DatePlausibilityReading> readings(UUID channelId) {
        get(channelId);                                  // 404 before an empty list
        return channels.findReadings(channelId);
    }

    @Transactional(readOnly = true)
    public List<DateRejectedAttempt> rejectedAttempts(UUID channelId) {
        get(channelId);
        return channels.findRejectedAttempts(channelId);
    }
}
