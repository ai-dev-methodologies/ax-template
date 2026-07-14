package com.ax.template.authblueprint.piecewisedeadband;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import com.ax.template.authblueprint.common.MemberWriter;

/**
 * piecewise-deadband-l0 sole orchestrator. {@link #createConfig} validates the tiling ONCE
 * (PWDB-SEGMENT-001) and persists the config + its segments atomically. {@link #evaluate} resolves the
 * covering segment, computes the signed deviation (PWDB-EVAL-001), and is IDEMPOTENT by a deterministic
 * input hash (PWDB-IMMUTABLE-001) — a race between two identical concurrent calls is resolved by the DB
 * unique constraint (never a lock: a config + its segments never change after creation, so there is
 * nothing to serialize evaluation against beyond the idempotency constraint itself).
 */
@Service
public class DeadbandService {

    static final int MEASURE_SCALE = 4;

    private final DeadbandConfigRepository configs;
    private final MemberWriter members;
    private final DeadbandMetrics metrics;
    private final Clock clock;

    public DeadbandService(DeadbandConfigRepository configs, MemberWriter members,
                           DeadbandMetrics metrics, Clock clock) {
        this.configs = configs;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** A caller-declared segment before persistence — {@code ordinal} is assigned from list order. */
    public record SegmentSpec(BigDecimal start, BigDecimal end, BigDecimal obligationTarget,
                              BigDecimal deadbandWidth) {}

    public record EvaluationResult(DeadbandEvaluation evaluation, boolean created) {}

    @Transactional
    public DeadbandConfig createConfig(String configKey, BigDecimal domainStart, BigDecimal domainEnd,
                                       List<SegmentSpec> segments) {
        if (domainStart == null || domainEnd == null || domainStart.compareTo(domainEnd) >= 0) {
            metrics.record("create", "invalid");
            throw DeadbandException.invalidValue();
        }
        BigDecimal dStart = domainStart.setScale(MEASURE_SCALE);
        BigDecimal dEnd = domainEnd.setScale(MEASURE_SCALE);
        validateTiling(dStart, dEnd, segments);
        if (configs.existsByConfigKey(configKey)) {
            metrics.record("create", "rejected");
            throw DeadbandException.duplicateConfig();
        }
        try {
            DeadbandConfig config = configs.saveAndFlush(
                new DeadbandConfig(UUID.randomUUID(), configKey, dStart, dEnd, Instant.now(clock)));
            for (int i = 0; i < segments.size(); i++) {
                SegmentSpec s = segments.get(i);
                members.persist(new DeadbandSegment(UUID.randomUUID(), config.getId(), i,
                    s.start().setScale(MEASURE_SCALE), s.end().setScale(MEASURE_SCALE),
                    s.obligationTarget().setScale(MEASURE_SCALE), s.deadbandWidth().setScale(MEASURE_SCALE)));
            }
            metrics.record("create", "ok");
            return config;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw DeadbandException.duplicateConfig();
        }
    }

    /** PWDB-SEGMENT-001 — sorted-by-caller-order segments must tile [domainStart, domainEnd) exactly. */
    private void validateTiling(BigDecimal domainStart, BigDecimal domainEnd, List<SegmentSpec> segments) {
        if (segments == null || segments.isEmpty()) {
            metrics.record("create", "invalid");
            throw DeadbandException.invalidSegments();
        }
        BigDecimal cursor = domainStart;
        for (SegmentSpec s : segments) {
            if (s.start() == null || s.end() == null || s.obligationTarget() == null || s.deadbandWidth() == null
                    || s.deadbandWidth().signum() < 0 || s.start().compareTo(s.end()) >= 0
                    || s.start().setScale(MEASURE_SCALE).compareTo(cursor) != 0) {
                metrics.record("create", "invalid");
                throw DeadbandException.invalidSegments();
            }
            cursor = s.end().setScale(MEASURE_SCALE);
        }
        if (cursor.compareTo(domainEnd) != 0) {
            metrics.record("create", "invalid");
            throw DeadbandException.invalidSegments();
        }
    }

    /** PWDB-EVAL-001 / PWDB-IMMUTABLE-001 — resolve the covering segment, compare, and record idempotently. */
    @Transactional
    public EvaluationResult evaluate(String configKey, BigDecimal pointX, BigDecimal actualValue) {
        DeadbandConfig config = configs.findByConfigKey(configKey).orElseThrow(DeadbandException::notFound);
        if (pointX == null || actualValue == null) {
            metrics.record("evaluate", "invalid");
            throw DeadbandException.invalidValue();
        }
        BigDecimal x = pointX.setScale(MEASURE_SCALE);
        BigDecimal actual = actualValue.setScale(MEASURE_SCALE);
        if (x.compareTo(config.getDomainStart()) < 0 || x.compareTo(config.getDomainEnd()) >= 0) {
            metrics.record("evaluate", "rejected");
            throw DeadbandException.pointOutOfDomain();
        }

        String idempotencyKey = idempotencyKeyFor(config.getId(), x, actual);
        var existing = configs.findEvaluationByIdempotencyKey(config.getId(), idempotencyKey);
        if (existing.isPresent()) {
            metrics.record("evaluate", "replayed");
            return new EvaluationResult(existing.get(), false);
        }

        DeadbandSegment covering = findCoveringSegment(config.getId(), x);
        BigDecimal deviation = actual.subtract(covering.getObligationTarget()).setScale(MEASURE_SCALE);
        boolean compliant = deviation.abs().compareTo(covering.getDeadbandWidth()) <= 0;

        try {
            long seq = configs.maxEvaluationSequence(config.getId()) + 1;
            DeadbandEvaluation row = members.persistAndFlush(new DeadbandEvaluation(UUID.randomUUID(),
                config.getId(), covering.getId(), x, actual, covering.getObligationTarget(),
                covering.getDeadbandWidth(), deviation, compliant, idempotencyKey, seq, Instant.now(clock)));
            metrics.record("evaluate", compliant ? "compliant" : "deviation");
            return new EvaluationResult(row, true);
        } catch (DataIntegrityViolationException e) {
            // PWDB-IMMUTABLE-001 — a concurrent identical evaluate committed first; replay it, not a new row.
            metrics.record("evaluate", "replayed");
            return new EvaluationResult(
                configs.findEvaluationByIdempotencyKey(config.getId(), idempotencyKey)
                    .orElseThrow(DeadbandException::notFound),
                false);
        }
    }

    private DeadbandSegment findCoveringSegment(UUID configId, BigDecimal x) {
        return configs.findSegments(configId).stream()
            .filter(s -> s.covers(x))
            .findFirst()
            .orElseThrow(DeadbandException::pointOutOfDomain);
    }

    private String idempotencyKeyFor(UUID configId, BigDecimal x, BigDecimal actual) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = configId + "|" + x.toPlainString() + "|" + actual.toPlainString();
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }

    @Transactional(readOnly = true)
    public DeadbandConfig getConfig(String configKey) {
        return configs.findByConfigKey(configKey).orElseThrow(DeadbandException::notFound);
    }

    @Transactional(readOnly = true)
    public List<DeadbandSegment> getSegments(String configKey) {
        DeadbandConfig config = getConfig(configKey);
        return configs.findSegments(config.getId());
    }

    @Transactional(readOnly = true)
    public Page<DeadbandEvaluation> listEvaluations(String configKey, int page, int size) {
        DeadbandConfig config = getConfig(configKey);
        return configs.findEvaluationsPage(config.getId(), PageRequest.of(safePage(page), safeSize(size)));
    }

    private int safePage(int page) { return Math.max(page, 0); }

    private int safeSize(int size) { return Math.min(Math.max(size, 1), 200); }
}
