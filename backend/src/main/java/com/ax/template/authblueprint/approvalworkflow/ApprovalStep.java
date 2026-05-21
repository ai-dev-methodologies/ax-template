package com.ax.template.authblueprint.approvalworkflow;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * ApprovalStep — one rung of an {@link ApprovalRequest}'s approval ladder.
 *
 * <p>Trace:
 * <ul>
 *   <li>WF-AUTHZ-003 — {@code approverUserId} is checked against the caller at action time</li>
 *   <li>WF-STEP-001 — {@code orderIndex} is the strict-ordering key</li>
 *   <li>WF-STEP-002 / WF-STEP-003 — {@code actedByUserId}, {@code actedAt},
 *       {@code comment} captured atomically with {@code status} by the state machine</li>
 * </ul>
 */
@Entity
@Table(
    name = "approval_steps",
    indexes = {
        @Index(name = "ix_approval_steps_request_order",
               columnList = "request_id,order_index"),
        @Index(name = "ix_approval_steps_approver_status",
               columnList = "approver_user_id,status")
    }
)
public class ApprovalStep {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, updatable = false)
    private ApprovalRequest request;

    @Column(name = "order_index", nullable = false, updatable = false)
    private int orderIndex;

    @Column(name = "approver_user_id", nullable = false, updatable = false, length = 255)
    private String approverUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApprovalStepStatus status;

    @Column(name = "acted_by_user_id", length = 255)
    private String actedByUserId;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(name = "comment", length = 1024)
    private String comment;

    protected ApprovalStep() {}

    private ApprovalStep(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.orderIndex = b.orderIndex;
        this.approverUserId = b.approverUserId;
        this.status = (b.status != null) ? b.status : ApprovalStepStatus.PENDING;
    }

    public UUID getId() { return id; }

    @JsonIgnore
    public ApprovalRequest getRequest() { return request; }

    public int getOrderIndex() { return orderIndex; }
    public String getApproverUserId() { return approverUserId; }
    public ApprovalStepStatus getStatus() { return status; }
    public String getActedByUserId() { return actedByUserId; }
    public Instant getActedAt() { return actedAt; }
    public String getComment() { return comment; }

    // Package-private — only the state machine + request#addStep call these.
    void setRequest(ApprovalRequest req) { this.request = req; }
    void setStatus(ApprovalStepStatus next) { this.status = next; }
    void setActedByUserId(String userId) { this.actedByUserId = userId; }
    void setActedAt(Instant when) { this.actedAt = when; }
    void setComment(String comment) { this.comment = comment; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private int orderIndex;
        private String approverUserId;
        private ApprovalStepStatus status;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder orderIndex(int v) { this.orderIndex = v; return this; }
        public Builder approverUserId(String v) { this.approverUserId = v; return this; }
        public Builder status(ApprovalStepStatus v) { this.status = v; return this; }

        public ApprovalStep build() { return new ApprovalStep(this); }
    }
}
