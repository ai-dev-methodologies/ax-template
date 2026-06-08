package com.ax.template.authblueprint.approvalworkflow;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * ApprovalRequest — the top-level workflow record.
 *
 * <p>Trace:
 * <ul>
 *   <li>WF-AUTHZ-002 — every lookup filters on {@code requesterUserId} (or
 *       cross-references {@link ApprovalStep#getApproverUserId()})</li>
 *   <li>WF-LIFECYCLE-001..004 — {@code status} mutated only by
 *       {@link ApprovalRequestStateMachine}</li>
 * </ul>
 */
@AggregateRoot
@Entity
@Table(
    name = "approval_requests",
    indexes = {
        @Index(name = "ix_approval_requests_requester_created",
               columnList = "requester_user_id,created_at"),
        @Index(name = "ix_approval_requests_status", columnList = "status")
    }
)
public class ApprovalRequest {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "requester_user_id", nullable = false, updatable = false, length = 255)
    private String requesterUserId;

    @Column(name = "type", nullable = false, updatable = false, length = 64)
    private String type;

    @Column(name = "title", length = 128)
    private String title;

    /**
     * Payload JSON captured at creation. {@code updatable=false} closes WF-PAYLOAD-001 —
     * the request body that approvers see at submit time CANNOT be silently swapped under
     * them mid-flight. Hibernate ignores any update attempt at the persistence layer.
     *
     * <p><b>PII contract — R83 iter1 F4.</b> {@code payloadJson} is stored VERBATIM
     * (up to 16384 chars) and fanned out to every visible approver via
     * {@link ApprovalRequestResponse#payload}. Korean enterprise 결재 payloads
     * commonly carry 주민등록번호 / 연봉 / 계약 상대 PII / 인사 evaluation language —
     * each step approver in the ladder reads the verbatim content. The catalog
     * deliberately does NOT redact at storage (would break structured-decision UX
     * and the {@code updatable=false} forensic invariant) but every consumer
     * MUST apply the privacy policy:
     * <ul>
     *   <li>Audit-log emission referencing payload fields → hash via
     *       {@code common.AuditPiiHelper.piiHash} (R67), do NOT log verbatim.</li>
     *   <li>SIEM / export / replication crossing the trust boundary → field-level
     *       redaction at the egress boundary, not at storage.</li>
     *   <li>Multi-step ladders → assume EVERY approver in the chain sees the
     *       payload; do not store information that only a subset should see.</li>
     * </ul>
     */
    @Column(name = "payload_json", length = 16384, updatable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ApprovalRequestStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    /** Required by JPA. */
    protected ApprovalRequest() {}

    private ApprovalRequest(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.requesterUserId = b.requesterUserId;
        this.type = b.type;
        this.title = b.title;
        this.payloadJson = b.payloadJson;
        this.status = (b.status != null) ? b.status : ApprovalRequestStatus.DRAFT;
        this.createdAt = (b.createdAt != null) ? b.createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public String getRequesterUserId() { return requesterUserId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getPayloadJson() { return payloadJson; }
    public ApprovalRequestStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public List<ApprovalStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    // Package-private — mutated only by the state machine + service.
    void addStep(ApprovalStep step) {
        step.setRequest(this);
        this.steps.add(step);
    }

    void setStatus(ApprovalRequestStatus next) { this.status = next; }
    void setSubmittedAt(Instant ts) { this.submittedAt = ts; }
    void setCompletedAt(Instant ts) { this.completedAt = ts; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String requesterUserId;
        private String type;
        private String title;
        private String payloadJson;
        private ApprovalRequestStatus status;
        private Instant createdAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder requesterUserId(String v) { this.requesterUserId = v; return this; }
        public Builder type(String v) { this.type = v; return this; }
        public Builder title(String v) { this.title = v; return this; }
        public Builder payloadJson(String v) { this.payloadJson = v; return this; }
        public Builder status(ApprovalRequestStatus v) { this.status = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public ApprovalRequest build() { return new ApprovalRequest(this); }
    }
}
