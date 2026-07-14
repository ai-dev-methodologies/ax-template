package com.ax.template.authblueprint.derivedstatement;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * derived-statement-l0 root (STMT-DERIVE-001): a generated statement whose IDENTITY is
 * {@code (subject, period, basisHash)} — a SHA-256 content hash of the canonicalized basis
 * line items, never a client-supplied idempotency header. The SAME (subject, period, basis)
 * always resolves to this SAME row; a CHANGED basis appends a NEW row at {@code version+1},
 * this row's content untouched (STMT-IMMUTABLE-003). Every column is
 * {@code updatable=false} — there is no update path, only append. The {@code uq_statement_basis}
 * DB backstop makes the identity unrepresentable-as-duplicate even if application logic
 * regresses (STMT-RETRY-002 — a benign concurrent-identical-submit race resolves onto this
 * constraint, never a second row).
 */
@AggregateRoot
@Entity
@Table(name = "derived_statements", uniqueConstraints = {
    @UniqueConstraint(name = "uq_statement_basis", columnNames = {"subject", "period", "basis_hash"})
})
public class DerivedStatement {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "subject", nullable = false, updatable = false, length = 200)
    private String subject;

    @Column(name = "period", nullable = false, updatable = false, length = 50)
    private String period;

    /** Statement version FOR THIS (subject, period) — increments only when the basis changes. */
    @Column(name = "version_no", nullable = false, updatable = false)
    private int versionNo;

    /** SHA-256 hex of the canonicalized basis line items — the identity STMT-DERIVE-001 pins. */
    @Column(name = "basis_hash", nullable = false, updatable = false, length = 64)
    private String basisHash;

    /** The reproducibility trail — the exact basis line items this statement was generated from. */
    @Column(name = "basis_json", nullable = false, updatable = false, length = 4000)
    private String basisJson;

    @Column(name = "total_amount", nullable = false, updatable = false, precision = 15, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    protected DerivedStatement() {}

    public DerivedStatement(UUID id, String subject, String period, int versionNo, String basisHash,
                            String basisJson, BigDecimal totalAmount, Instant generatedAt) {
        this.id = id;
        this.subject = subject;
        this.period = period;
        this.versionNo = versionNo;
        this.basisHash = basisHash;
        this.basisJson = basisJson;
        this.totalAmount = totalAmount;
        this.generatedAt = generatedAt;
    }

    public UUID getId() { return id; }
    public String getSubject() { return subject; }
    public String getPeriod() { return period; }
    public int getVersionNo() { return versionNo; }
    public String getBasisHash() { return basisHash; }
    public String getBasisJson() { return basisJson; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getGeneratedAt() { return generatedAt; }
}
