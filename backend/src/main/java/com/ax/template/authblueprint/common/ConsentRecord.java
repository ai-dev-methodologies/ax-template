package com.ax.template.authblueprint.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * IMW5 (IDW4 EMR-lite dogfood, 2026-05-30) — cross-cutting APPEND-ONLY consent
 * ledger ROW. Ships the REAL reusable code for the consent-record obligation that
 * {@code specs/consent-management-l0.yaml#CONSENT-RECORD-001} (GDPR Art 7(1)
 * "demonstrate consent") describes but no domain implemented: the spec is
 * SPEC-ONLY, and all three IDW4 personas hand-rolled the full consent subsystem.
 *
 * <h2>The contradiction this resolves (load-bearing)</h2>
 * {@code CONSENT-RECORD-001} mandates an <em>append-only immutable ledger</em>
 * ("withdrawal appends a new row, never updates"), yet the natural JPA shape for
 * "what is this subject's current consent for purpose X" is a single mutable
 * {@code @Version}'d current-state row. All three IDW4 personas chose the mutable
 * row AND flagged it as a contradiction with the demonstrate-consent obligation —
 * a mutated row destroys the evidence that an earlier GRANT ever existed, so it
 * cannot satisfy Art 7(1) on a later challenge.
 *
 * <p>This record resolves it by making the LEDGER the source of truth: every state
 * change APPENDS one immutable row {@code (subjectId, purpose, action, recordedAt)}.
 * A withdrawal is a new {@link Action#WITHDRAW} row, never an update of the prior
 * GRANT row. "Current consent" is then a DERIVED QUERY — the latest action per
 * {@code (subject, purpose)} — not a mutable column. The append-only ledger is the
 * demonstrate-consent proof; the boolean "is consent active now" is computed by
 * {@link ConsentGate#activeConsent(String, String, java.util.List)}.
 *
 * <h2>Immutability — enforced three ways</h2>
 * Mirrors the {@code auditlog/AuditLog} append-only posture
 * ({@code CONSENT-RECORD-001} notes: "reuses audit-log-l0 posture"):
 * <ul>
 *   <li>every business column is {@code @Column(updatable = false)} — Hibernate
 *       omits it from any UPDATE statement;</li>
 *   <li>NO public setters — the only way to populate a row is the
 *       {@link #grant(String, String)} / {@link #withdraw(String, String)}
 *       factory methods (a withdrawal APPENDS a WITHDRAW row);</li>
 *   <li>fields are package-private with no mutators, so application code cannot
 *       rewrite an existing row's subject / purpose / action / timestamp.</li>
 * </ul>
 * The {@code id} is database-generated (one identity per appended row); a row is
 * never re-saved under a different id.
 *
 * <h2>Framework-light core</h2>
 * The JPA annotations make a row persistable, but the consent <em>decision</em>
 * logic lives in {@link ConsentGate} and operates on a plain {@code List<ConsentRecord>},
 * so it is trivially unit-testable without a database or a Spring context (construct
 * rows via {@link #grant}/{@link #withdraw} and pass a list). No
 * {@code source_ip_masked} / {@code user_agent_summary} / {@code policy_version}
 * columns are modelled here: those are CONSENT-RECORD-001 capture-context fields a
 * fork-receiver adds on their own consent {@code @Entity}; this common primitive
 * ships only the minimal append-only ledger spine the rule-of-three proved every
 * persona re-derives identically.
 */
@Entity
@Table(
    name = "consent_records",
    indexes = {
        // Current-state derivation scans the ledger by (subject, purpose) and
        // takes the latest recordedAt — index that access path.
        @Index(name = "ix_consent_records_subject_purpose",
            columnList = "subject_id,purpose,recorded_at")
    }
)
public class ConsentRecord {

    /**
     * The two consent state-change actions an append-only ledger row can carry.
     * Current consent for a {@code (subject, purpose)} pair is the action of the
     * latest row: {@link #GRANT} means consent is active, {@link #WITHDRAW} (or no
     * row at all) means it is not. A withdrawal never deletes or updates the prior
     * GRANT row — it appends a {@code WITHDRAW} row, preserving the full trail.
     */
    public enum Action {
        /** An explicit affirmative opt-in (CONSENT-CAPTURE-001). */
        GRANT,
        /** A withdrawal of a prior grant (CONSENT-WITHDRAW-001) — appended, not an update. */
        WITHDRAW
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @Column(name = "subject_id", updatable = false, nullable = false, length = 255)
    private String subjectId;

    @Column(name = "purpose", updatable = false, nullable = false, length = 128)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", updatable = false, nullable = false, length = 16)
    private Action action;

    @Column(name = "recorded_at", updatable = false, nullable = false)
    private Instant recordedAt;

    /** JPA requires a no-arg constructor; not for application use. */
    protected ConsentRecord() {
    }

    private ConsentRecord(String subjectId, String purpose, Action action, Instant recordedAt) {
        this.subjectId = requireText(subjectId, "subjectId");
        this.purpose = requireText(purpose, "purpose");
        this.action = Objects.requireNonNull(action, "action must be non-null");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must be non-null");
    }

    /**
     * Append a {@link Action#GRANT} row for {@code (subjectId, purpose)} stamped at
     * the current UTC instant. The caller persists the returned row; it is never
     * an update of an earlier row.
     */
    public static ConsentRecord grant(String subjectId, String purpose) {
        return new ConsentRecord(subjectId, purpose, Action.GRANT, Instant.now());
    }

    /**
     * Append a {@link Action#WITHDRAW} row for {@code (subjectId, purpose)} stamped
     * at the current UTC instant. This is how a withdrawal is recorded — a NEW
     * appended row, never a mutation of the prior GRANT row (resolves the
     * CONSENT-RECORD-001 contradiction).
     */
    public static ConsentRecord withdraw(String subjectId, String purpose) {
        return new ConsentRecord(subjectId, purpose, Action.WITHDRAW, Instant.now());
    }

    /**
     * Append a row with an explicit action and timestamp. Intended for tests and
     * for back-dated import of an existing consent trail; production capture paths
     * should prefer {@link #grant}/{@link #withdraw}, which stamp {@code now()}.
     */
    public static ConsentRecord of(String subjectId, String purpose, Action action, Instant recordedAt) {
        return new ConsentRecord(subjectId, purpose, action, recordedAt);
    }

    public Long id() {
        return id;
    }

    public String subjectId() {
        return subjectId;
    }

    public String purpose() {
        return purpose;
    }

    public Action action() {
        return action;
    }

    public Instant recordedAt() {
        return recordedAt;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must be non-blank");
        }
        return value;
    }
}
