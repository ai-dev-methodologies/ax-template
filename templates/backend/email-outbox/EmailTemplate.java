/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailTemplate
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "Spring Data JPA Reference — Entity mapping with @Entity, @Id, @GeneratedValue"
 *     url: "https://docs.spring.io/spring-data/jpa/reference/jpa/entity-persistence.html"
 *   - source_type: external
 *     citation: "OWASP Server-Side Template Injection — SSTI prevention via logic-less templates"
 *     url: "https://owasp.org/www-community/attacks/Server_Side_Template_Injection"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   EmailTemplate stores reusable email templates identified by a short code (e.g. "forgot-password").
 *   The template uses simple {{key}} placeholders — no scripting, no OGNL, SSTI-safe.
 *   EmailTemplateService.render(code, vars) resolves the template and substitutes vars.
 */
package com.example.app.emailoutbox;

import com.example.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.SQLDelete;

/**
 * Email template entity — stores subject and body templates identified by a code.
 *
 * <p>Template syntax: {@code {{key}}} placeholders are replaced by
 * {@link EmailTemplateService#render(String, Object)} at send time.
 * Missing keys resolve to empty string (no exception) per EMAIL-TEMPLATE-001.
 *
 * <p>Example templates:
 * <ul>
 *   <li>{@code forgot-password} — password reset link
 *   <li>{@code welcome} — new user welcome
 *   <li>{@code otp} — one-time password delivery
 * </ul>
 *
 * <p>Extends {@code BaseEntity} (SP13) for: id, createdAt, updatedAt, deleted.
 */
@Entity
@SQLDelete(sql = "UPDATE email_templates SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
    name = "email_templates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_email_templates_code", columnNames = "code")
    }
)
public class EmailTemplate extends BaseEntity {

    /**
     * Short identifier for this template (e.g., "forgot-password", "welcome", "otp").
     * Must be unique; used to look up the template at runtime.
     */
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    /** Human-readable name for admin display. */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    /** Subject line template; may contain {{key}} placeholders. */
    @Column(name = "subject_template", nullable = false, length = 998)
    private String subjectTemplate;

    /** Body template (HTML or plain text); may contain {{key}} placeholders. */
    @Column(name = "body_template", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    protected EmailTemplate() {
        // JPA
    }

    /**
     * Factory — creates a new email template.
     *
     * @param code           unique short identifier
     * @param name           human-readable display name
     * @param subjectTemplate subject line with optional {{key}} placeholders
     * @param bodyTemplate   body with optional {{key}} placeholders
     */
    public static EmailTemplate create(
            String code,
            String name,
            String subjectTemplate,
            String bodyTemplate) {
        var t = new EmailTemplate();
        t.code = code;
        t.name = name;
        t.subjectTemplate = subjectTemplate;
        t.bodyTemplate = bodyTemplate;
        return t;
    }

    public String getCode()            { return code; }
    public String getName()            { return name; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyTemplate()    { return bodyTemplate; }
}
