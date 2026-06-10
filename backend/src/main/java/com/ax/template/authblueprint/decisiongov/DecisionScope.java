package com.ax.template.authblueprint.decisiongov;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * decision-governance-l0 scope root: one governed decision subject (a quote scope, a
 * credit-limit subject). {@code currentVersion} is the cheap latest pointer (DG-CHAIN-001),
 * advanced ONLY via the package-private {@link #advanceVersion} by {@link DecisionService}
 * under a PESSIMISTIC_WRITE row lock (DG-CONCURRENT-001). Version rows are
 * {@link DecisionVersion} members — reads via JPQL on {@link DecisionScopeRepository},
 * writes via the shared {@code common/MemberWriter} (AX-DDD-MEMBER-REPO end-state).
 */
@AggregateRoot
@Entity
@Table(name = "decision_scopes")
@Check(constraints = "current_version >= 1")
public class DecisionScope {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope_key", nullable = false, updatable = false, length = 200, unique = true)
    private String scopeKey;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DecisionScope() {}

    public DecisionScope(UUID id, String scopeKey, Instant createdAt) {
        this.id = id;
        this.scopeKey = scopeKey;
        this.currentVersion = 1;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (service, under the row lock) — advance the latest-version pointer. */
    void advanceVersion(int next) {
        this.currentVersion = next;
    }

    public UUID getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public int getCurrentVersion() { return currentVersion; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
