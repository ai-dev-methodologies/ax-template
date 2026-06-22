package com.ax.template.authblueprint.authzparity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * authorization-parity-l0 root: an authorization ENVELOPE. The action type, the exact authorized
 * parameters (canonical key=value map), the canonical SHA-256 {@code parityHash} over them, the
 * declared MANDATORY companion gate keys, the high-value flag, and the requester are all recorded
 * immutably at authorize time ({@code @Column(updatable=false)}). EXECUTED-matches-authorized
 * (AUTHZPARITY-EXEC-001), four-eyes (AUTHZPARITY-FOUREYES-001), and positive-gates
 * (AUTHZPARITY-GATES-001) all read from THIS row — never from the execution request — so a forged
 * execution cannot move its own goalposts. The status moves only via the package-private
 * {@link #markExecuted} hook, called by {@link AuthorizationParityService} under the row lock
 * (AUTHZPARITY-CONCURRENT-001). There is NO delete path.
 */
@AggregateRoot
@Entity
@Table(name = "authorized_actions")
// AUTHZPARITY-EXEC-001 — an EXECUTED action records when; an AUTHORIZED one has not. LIVE under ddl-auto.
@Check(constraints = "status = 'AUTHORIZED' OR executed_at IS NOT NULL")
public class AuthorizedAction {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "action_type", nullable = false, updatable = false, length = 100)
    private String actionType;

    /** The EXACT authorized parameters, canonical serialization (sorted key=value). Immutable. */
    @Column(name = "authorized_params", nullable = false, updatable = false, length = 2000)
    private String authorizedParams;

    /** Canonical SHA-256 over the sorted authorized parameters — the parity anchor. Immutable. */
    @Column(name = "parity_hash", nullable = false, updatable = false, length = 64)
    private String parityHash;

    @Column(name = "high_value", nullable = false, updatable = false)
    private boolean highValue;

    @Column(name = "requester_user_id", nullable = false, updatable = false, length = 200)
    private String requesterUserId;

    /**
     * The declared MANDATORY companion gate keys — set once at authorize time (AUTHZPARITY-GATES-001).
     * Immutability is enforced structurally: no mutator exists, the constructor takes a defensive copy,
     * and {@link #getRequiredGates()} returns a {@code Set.copyOf} read-only view. (Hibernate forbids
     * {@code updatable=false} on an element-collection value column — it requires consistent
     * insertable/updatable — so the immutability is held in code, not on the join column.)
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "authorized_action_required_gates",
        joinColumns = @JoinColumn(name = "action_id"))
    @Column(name = "gate_key", nullable = false, length = 100)
    private Set<String> requiredGates = new TreeSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActionStatus status;

    @Column(name = "executed_at")
    private Instant executedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthorizedAction() {}

    public AuthorizedAction(UUID id, String actionType, String authorizedParams, String parityHash,
                            boolean highValue, String requesterUserId, Set<String> requiredGates,
                            Instant createdAt) {
        this.id = id;
        this.actionType = actionType;
        this.authorizedParams = authorizedParams;
        this.parityHash = parityHash;
        this.highValue = highValue;
        this.requesterUserId = requesterUserId;
        this.requiredGates = new TreeSet<>(requiredGates);
        this.status = ActionStatus.AUTHORIZED;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook (service, under the row lock) — move to EXECUTED exactly once. */
    void markExecuted(Instant at) {
        this.status = ActionStatus.EXECUTED;
        this.executedAt = at;
    }

    public UUID getId() { return id; }
    public String getActionType() { return actionType; }
    public String getAuthorizedParams() { return authorizedParams; }
    public String getParityHash() { return parityHash; }
    public boolean isHighValue() { return highValue; }
    public String getRequesterUserId() { return requesterUserId; }
    public Set<String> getRequiredGates() { return Set.copyOf(requiredGates); }
    public ActionStatus getStatus() { return status; }
    public Instant getExecutedAt() { return executedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
