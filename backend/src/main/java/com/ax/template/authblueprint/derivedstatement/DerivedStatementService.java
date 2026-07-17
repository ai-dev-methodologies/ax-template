package com.ax.template.authblueprint.derivedstatement;

import com.ax.template.authblueprint.common.IdempotentInsert;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * derived-statement-l0 sole orchestrator. {@link #generate} NEVER trusts a client-supplied
 * idempotency token: the statement's identity is a SHA-256 hash of the canonicalized basis
 * line items, so an identical resubmission (STMT-RETRY-002) resolves onto the SAME row through
 * the lookup-before-insert path — and a benign concurrent-identical-submit race is caught by
 * the {@code uq(subject, period, basis_hash)} DB constraint and resolved to the winning row,
 * never a 500. A changed basis appends a NEW version; no row is ever mutated (STMT-IMMUTABLE-003).
 */
@Service
public class DerivedStatementService {

    private final DerivedStatementRepository statements;
    private final IdempotentInsert idempotentInsert;
    private final DerivedStatementMetrics metrics;
    private final Clock clock;

    public DerivedStatementService(DerivedStatementRepository statements, IdempotentInsert idempotentInsert,
                                   DerivedStatementMetrics metrics, Clock clock) {
        this.statements = statements;
        this.idempotentInsert = idempotentInsert;
        this.metrics = metrics;
        this.clock = clock;
    }

    /**
     * STMT-DERIVE/RETRY-001/002 — generate (or replay) a statement for (subject, period, basis).
     * The SAME basis for the SAME (subject, period) always returns the SAME row; a CHANGED basis
     * appends version+1.
     */
    @Transactional
    public DerivedStatement generate(String subject, String period, List<? extends LineItem> basis) {
        if (basis == null || basis.isEmpty()) {
            metrics.record("rejected");
            throw DerivedStatementException.emptyBasis();
        }
        String basisJson = canonicalize(basis);
        String basisHash = sha256(basisJson);

        // STMT-DERIVE-001 — the IDENTICAL basis for this (subject, period) returns the existing row.
        var existing = statements.findBySubjectAndPeriodAndBasisHash(subject, period, basisHash);
        if (existing.isPresent()) {
            metrics.record("replayed");
            return existing.get();
        }

        int nextVersion = statements.findTopBySubjectAndPeriodOrderByVersionNoDesc(subject, period)
            .map(s -> s.getVersionNo() + 1).orElse(1);
        BigDecimal total = basis.stream().map(LineItem::amount).reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            // P1-65 — save() defers the INSERT to commit (outside this try), so a race would surface
            // at commit as a 500 and this catch would never fire. saveAndFlush forces the uq(subject,
            // period, basis_hash) violation to fire INSIDE the try; the REQUIRES_NEW boundary keeps
            // that violation from poisoning this outer tx on PostgreSQL (25P02) so the replay requery
            // below runs clean.
            DerivedStatement saved = idempotentInsert.insert(() -> statements.saveAndFlush(
                new DerivedStatement(UUID.randomUUID(), subject, period,
                    nextVersion, basisHash, basisJson, total, Instant.now(clock))));
            metrics.record("created");
            return saved;
        } catch (DataIntegrityViolationException raced) {
            // STMT-RETRY-002 — a concurrent identical submit won the uq(subject, period, basis_hash)
            // race; return ITS row rather than erroring or inserting a second one.
            metrics.record("raced");
            return statements.findBySubjectAndPeriodAndBasisHash(subject, period, basisHash)
                .orElseThrow(DerivedStatementException::notFound);
        }
    }

    @Transactional(readOnly = true)
    public DerivedStatement get(UUID id) {
        return statements.findById(id).orElseThrow(DerivedStatementException::notFound);
    }

    @Transactional(readOnly = true)
    public List<DerivedStatement> versionsOf(String subject, String period) {
        return statements.findBySubjectAndPeriodOrderByVersionNoAsc(subject, period);
    }

    /** Canonical basis JSON, sorted by label so the hash is independent of submission order. */
    private static String canonicalize(List<? extends LineItem> basis) {
        List<LineItem> sorted = new ArrayList<>(basis);
        sorted.sort((a, b) -> a.label().compareTo(b.label()));
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sorted.size(); i++) {
            LineItem item = sorted.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"label\":\"").append(item.label())
              .append("\",\"amount\":\"").append(item.amount().toPlainString()).append("\"}");
        }
        return sb.append(']').toString();
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** A single (label, amount) basis line item — the content STMT-DERIVE-001 hashes. */
    public interface LineItem {
        String label();
        BigDecimal amount();
    }
}
