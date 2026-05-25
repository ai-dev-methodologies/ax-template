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

    protected EmailTemplate() {}

    public EmailTemplate(String templateCode, String subjectTemplate, String bodyTemplate) {
        this.templateCode = templateCode;
        this.subjectTemplate = subjectTemplate;
        this.bodyTemplate = bodyTemplate;
    }

    public String getTemplateCode() { return templateCode; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate() { return bodyTemplate; }
}
