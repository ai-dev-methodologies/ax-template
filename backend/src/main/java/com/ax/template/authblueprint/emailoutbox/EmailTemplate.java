package com.ax.template.authblueprint.emailoutbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Email template (subject + body) keyed by templateCode.
 * <p>
 * Trace: EMAIL-TEMPLATE-001 — {@link EmailTemplateService#render} substitutes
 * variables; missing variable keys produce empty strings, not exceptions.
 *
 * <p>R60 iter1 F10 closure via Wave D2 — {@link #version} increments on
 * every {@link EmailTemplateService#upsertTemplate} call and the prior
 * snapshot is captured in {@link EmailTemplateHistory}. A future audit
 * (post-incident reconstruction, regulatory inspection, legal hold)
 * can resolve "which template version produced this enqueue" by
 * joining {@code email_outbox.created_at} against the history table's
 * {@code captured_at}.
 */
@Entity
@Table(name = "email_templates")
public class EmailTemplate {

    @Id
    @Column(name = "template_code", nullable = false, length = 64)
    private String templateCode;

    @Column(name = "subject_template", nullable = false, length = 998)
    private String subjectTemplate;

    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    /**
     * R60 iter1 F10 / Wave D2 — incrementing version counter. Starts at
     * 1 for a fresh row; {@link EmailTemplateService#upsertTemplate}
     * bumps it on every update and captures the prior snapshot in
     * {@link EmailTemplateHistory}.
     */
    @Column(name = "version", nullable = false)
    private int version;

    protected EmailTemplate() {}

    public EmailTemplate(String templateCode, String subjectTemplate, String bodyTemplate) {
        this(templateCode, subjectTemplate, bodyTemplate, 1);
    }

    public EmailTemplate(String templateCode, String subjectTemplate, String bodyTemplate, int version) {
        this.templateCode = templateCode;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.version = version;
    }

    /**
     * Package-private mutation surface for {@link EmailTemplateService#upsertTemplate}.
     * Bumps version, replaces subject/body templates atomically.
     */
    void applyUpdate(String subjectTemplate, String bodyTemplate) {
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
        this.version = this.version + 1;
    }

    public String getTemplateCode() { return templateCode; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
    public int getVersion() { return version; }
}
