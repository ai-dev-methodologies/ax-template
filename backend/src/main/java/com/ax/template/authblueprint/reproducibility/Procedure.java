package com.ax.template.authblueprint.reproducibility;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * reproducible-procedure-l0 root: one auditable deterministic procedure — a DRAW that records a
 * server-generated seed + algorithm + canonical input hash + selected ids (PROC-DRAW-001 /
 * PROC-REPLAY-001), or a CLASSIFICATION that records an input hash + classifier version + resolved
 * class (PROC-CLASS-001). Every basis column is {@code @Column(updatable=false)} — a procedure is
 * append-only and immutable after creation, so there is NO mutator hook and no setter at all; a
 * replay re-derives the result from the recorded seed without touching the row. The sensitive
 * subject is stored {@code @JsonIgnore} raw + reached only by ADMIN via the unmask path
 * (PROC-BLIND-001). Built only via the {@link #draw} / {@link #classify} static factories.
 */
@AggregateRoot
@Entity
@Table(name = "procedures", uniqueConstraints = {
    @UniqueConstraint(name = "uq_procedure_class", columnNames = {"input_hash", "classifier_version", "kind"})
})
@Check(constraints =
    "draw_k >= 0"
    + " AND (kind <> 'DRAW' OR (seed IS NOT NULL AND algorithm IS NOT NULL AND selected_ids IS NOT NULL))"
    + " AND (kind <> 'CLASSIFICATION' OR (classifier_version IS NOT NULL AND resolved_class IS NOT NULL))")
public class Procedure {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 20)
    private ProcedureKind kind;

    /** The external reference of the input set / subject (opaque, recorded verbatim). */
    @Column(name = "input_set_ref", nullable = false, updatable = false, length = 200)
    private String inputSetRef;

    /** Canonical SHA-256 hex of the (caller-sorted) input set — the recorded reproducibility basis. */
    @Column(name = "input_hash", nullable = false, updatable = false, length = 64)
    private String inputHash;

    // ── DRAW basis (PROC-DRAW-001) ──
    @Column(name = "seed", updatable = false)
    private Long seed;

    @Column(name = "algorithm", updatable = false, length = 60)
    private String algorithm;

    @Column(name = "draw_k", nullable = false, updatable = false)
    private int drawK;

    /** The canonical (sorted) candidate list, comma-joined — recorded so replay re-derives from it. */
    @Column(name = "candidates", updatable = false, length = 4000)
    private String candidates;

    /** The produced selection, comma-joined — the recorded draw result a replay must reproduce. */
    @Column(name = "selected_ids", updatable = false, length = 4000)
    private String selectedIds;

    // ── CLASSIFICATION basis (PROC-CLASS-001) ──
    @Column(name = "classifier_version", updatable = false, length = 60)
    private String classifierVersion;

    @Column(name = "resolved_class", updatable = false, length = 120)
    private String resolvedClass;

    // ── BLINDING (PROC-BLIND-001) — raw stored, never serialized; reached only by ADMIN unmask ──
    @JsonIgnore
    @Column(name = "raw_subject", updatable = false, length = 400)
    private String rawSubject;

    @Column(name = "actor", nullable = false, updatable = false, length = 200)
    private String actor;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Procedure() {}

    private Procedure(UUID id, ProcedureKind kind, String inputSetRef, String inputHash,
                      String actor, Instant createdAt) {
        this.id = id;
        this.kind = kind;
        this.inputSetRef = inputSetRef;
        this.inputHash = inputHash;
        this.actor = actor;
        this.createdAt = createdAt;
        this.drawK = 0;
    }

    /** PROC-DRAW-001 — record a draw with its server-generated seed + algorithm + selection. */
    static Procedure draw(UUID id, String inputSetRef, String inputHash, String algorithm,
                          long seed, int k, String candidates, String selectedIds,
                          String rawSubject, String actor, Instant createdAt) {
        Procedure p = new Procedure(id, ProcedureKind.DRAW, inputSetRef, inputHash, actor, createdAt);
        p.seed = seed;
        p.algorithm = algorithm;
        p.drawK = k;
        p.candidates = candidates;
        p.selectedIds = selectedIds;
        p.rawSubject = rawSubject;
        return p;
    }

    /** PROC-CLASS-001 — record a classification pinned to its classifier version. */
    static Procedure classify(UUID id, String inputSetRef, String inputHash, String classifierVersion,
                              String resolvedClass, String rawSubject, String actor, Instant createdAt) {
        Procedure p = new Procedure(id, ProcedureKind.CLASSIFICATION, inputSetRef, inputHash, actor, createdAt);
        p.classifierVersion = classifierVersion;
        p.resolvedClass = resolvedClass;
        p.rawSubject = rawSubject;
        return p;
    }

    /** The recorded canonical candidate list — replay re-derives the selection from this + the seed. */
    List<String> sortedCandidates() {
        return splitCsv(candidates);
    }

    /** The recorded selection as a list — what a replay must reproduce byte-identically. */
    List<String> selectedIdList() {
        return splitCsv(selectedIds);
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(csv.split(","));
    }

    public UUID getId() { return id; }
    public ProcedureKind getKind() { return kind; }
    public String getInputSetRef() { return inputSetRef; }
    public String getInputHash() { return inputHash; }
    public Long getSeed() { return seed; }
    public String getAlgorithm() { return algorithm; }
    public int getDrawK() { return drawK; }
    public String getSelectedIds() { return selectedIds; }
    public String getClassifierVersion() { return classifierVersion; }
    public String getResolvedClass() { return resolvedClass; }
    public String getActor() { return actor; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }

    /** The deterministic masked projection of the blinded subject — what a MEMBER may see. */
    public String maskedSubject() { return FieldBlinder.mask(rawSubject); }

    /** The raw blinded subject — reached only by the ADMIN unmask path (PROC-BLIND-001). */
    @JsonIgnore
    String rawSubject() { return rawSubject; }
}
