package com.ax.template.authblueprint.emailoutbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateMember;

/**
 * Immutable snapshot of an {@link EmailTemplate} at a specific version.
 *
 * <p>R60 iter1 F10 closure via Wave D2 — every
 * {@link EmailTemplateService#upsertTemplate} call captures the PRE-update
 * subject/body content in a new history row. A future audit
 * (post-incident reconstruction, regulatory inspection, legal hold) can
 * resolve "which template version produced the enqueue row with this
 * created_at" by joining {@code email_outbox.created_at} against the
 * history table's {@code captured_at} range.
 *
 * <p>Every column carries {@code updatable = false} so the JPA layer
 * cannot rewrite a snapshot — once a history row exists, it is frozen.
 */
@AggregateMember(root = EmailTemplate.class)
@Entity
@Table(
    name = "email_template_history",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_email_template_history_code_version",
        columnNames = {"template_code", "version"}
    ),
    indexes = {
        @Index(name = "ix_email_template_history_code_captured",
               columnList = "template_code,captured_at")
    }
)
public class EmailTemplateHistory {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "template_code", updatable = false, nullable = false, length = 64)
    private String templateCode;

    @Column(name = "version", updatable = false, nullable = false)
    private int version;

    @Column(name = "subject_template", updatable = false, nullable = false, length = 998)
    private String subjectTemplate;

    @Column(name = "body_template", updatable = false, nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "captured_at", updatable = false, nullable = false)
    private Instant capturedAt;

    /** Required by JPA. */
    protected EmailTemplateHistory() {}

    public EmailTemplateHistory(UUID id, String templateCode, int version,
                                 String subjectTemplate, String bodyTemplate,
                                 Instant capturedAt) {
        this.id = id;
        this.templateCode = templateCode;
        this.version = version;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.capturedAt = capturedAt;
    }

    /** Factory for an immutable snapshot of the supplied template at its current version. */
    public static EmailTemplateHistory snapshot(EmailTemplate template, Instant when) {
        return new EmailTemplateHistory(
            UUID.randomUUID(),
            template.getTemplateCode(),
            template.getVersion(),
            template.getSubjectTemplate(),
            template.getBodyTemplate(),
            when
        );
    }

    public UUID getId() { return id; }
    public String getTemplateCode() { return templateCode; }
    public int getVersion() { return version; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public Instant getCapturedAt() { return capturedAt; }
}
