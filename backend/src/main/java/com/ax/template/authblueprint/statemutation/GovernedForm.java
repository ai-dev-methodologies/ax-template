package com.ax.template.authblueprint.statemutation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * state-conditional-mutability-l0 root: one GovernedForm whose MUTABLE FIELD-SET is a function of
 * its {@link #state} ({@link StateFieldPolicy}). Lifecycle moves ONLY via {@link GovernedFormStateMachine}
 * (the sole status mutator); field content moves ONLY via the package-private {@link #applyEdit}, called
 * by {@link StateMutationService} under the form's PESSIMISTIC_WRITE row lock (STATEMUTATION-TOCTOU-001).
 * The @Check backstop records the structural invariant that LOCKED is the only terminal that may carry
 * a non-null {@link #lockedAt}, and that a submitted/approved form has its content frozen-by-state (the
 * by-state freeze is enforced in code; the row carries the audit basis of the last edit).
 */
@AggregateRoot
@Entity
@Table(name = "governed_forms")
@Check(constraints =
    "(state <> 'LOCKED' OR locked_at IS NOT NULL)"
    + " AND (last_edited_at IS NULL OR last_edited_field IS NOT NULL)")
public class GovernedForm {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The form owner (the authenticated principal who opened it) — recorded verbatim, immutable. */
    @Column(name = "owner", nullable = false, updatable = false, length = 200)
    private String owner;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "body", length = 4000)
    private String body;

    @Column(name = "reviewer_note", length = 2000)
    private String reviewerNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private FormState state;

    /** The recorded basis of the last field edit — which field, when — so the audit is reconstructible. */
    @Enumerated(EnumType.STRING)
    @Column(name = "last_edited_field", length = 20)
    private FormField lastEditedField;

    @Column(name = "last_edited_at")
    private Instant lastEditedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected GovernedForm() {}

    public GovernedForm(UUID id, String owner, String title, String body, Instant createdAt) {
        this.id = id;
        this.owner = owner;
        this.title = title;
        this.body = body;
        this.state = FormState.DRAFT;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — apply an edit to one field with its recorded basis (STATEMUTATION-AUTHORITY-001).
     *  Authority (is this field mutable in this state?) is enforced by the service UNDER the row lock. */
    void applyEdit(FormField field, String value, Instant at) {
        switch (field) {
            case TITLE -> this.title = value;
            case BODY -> this.body = value;
            case REVIEWER_NOTE -> this.reviewerNote = value;
        }
        this.lastEditedField = field;
        this.lastEditedAt = at;
    }

    /** Sole-mutator hook — set the lifecycle state. Called ONLY by {@link GovernedFormStateMachine}. */
    void setState(FormState next) {
        this.state = next;
    }

    /** Sole-mutator hook — stamp the terminal lock instant. Called ONLY by the state machine. */
    void markLocked(Instant at) {
        this.lockedAt = at;
    }

    /** The DECLARED mutable-field-set for the form's CURRENT state — the same table the edit path enforces. */
    public Set<FormField> mutableFields() {
        return StateFieldPolicy.mutableFields(state);
    }

    public UUID getId() { return id; }
    public String getOwner() { return owner; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getReviewerNote() { return reviewerNote; }
    public FormState getState() { return state; }
    public FormField getLastEditedField() { return lastEditedField; }
    public Instant getLastEditedAt() { return lastEditedAt; }
    public Instant getLockedAt() { return lockedAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
