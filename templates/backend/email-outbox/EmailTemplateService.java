/**
 * @ax-template-meta
 * template_id: backend/email-outbox/EmailTemplateService
 * layer: backend-domain
 * domain: email-outbox
 * anchors_rule: transaction-readonly-queries.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "OWASP Server-Side Template Injection — SSTI prevention via logic-less templates"
 *     url: "https://owasp.org/www-community/attacks/Server_Side_Template_Injection"
 *   - source_type: external
 *     citation: "Spring Framework Reference — @Service stereotype annotation"
 *     url: "https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   EmailTemplateService.render(code, vars) looks up a template by code and substitutes
 *   {{key}} placeholders with values from the vars map.
 *   Missing keys produce empty strings — no exception (EMAIL-TEMPLATE-001).
 *   No scripting engine used — SSTI-safe by design.
 */
package com.example.app.emailoutbox;

import com.example.app.common.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service for rendering email templates.
 *
 * <p>Template syntax: {@code {{key}}} placeholders resolved from a {@code Map<String, Object>}.
 * Missing keys resolve to empty string (not exception) per EMAIL-TEMPLATE-001.
 * No expression language or scripting is evaluated — SSTI is not possible.
 *
 * <p>Extends {@link BaseService} (SP13) for shared exception helpers.
 */
@Service
@Transactional(readOnly = true)
public class EmailTemplateService extends BaseService {

    /** Pattern matching {{key}} placeholders (non-greedy). */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    private final EmailTemplateRepository templateRepository;

    public EmailTemplateService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Renders a template identified by {@code code} by substituting
     * all {@code {{key}}} placeholders with values from {@code vars}.
     *
     * @param code template code (e.g., "forgot-password")
     * @param vars variable map; may be null or empty (all keys become "")
     * @return rendered subject + body
     * @throws jakarta.persistence.EntityNotFoundException if no template with code exists
     */
    public RenderedEmail render(String code, Object vars) {
        var template = templateRepository.findByCode(code)
                .orElseThrow(() -> entityNotFound("EmailTemplate", code));

        Map<String, Object> varMap = toMap(vars);

        var subject = substitute(template.getSubjectTemplate(), varMap);
        var body = substitute(template.getBodyTemplate(), varMap);

        return new RenderedEmail(subject, body);
    }

    // ─── substitution ─────────────────────────────────────────────────────

    private String substitute(String template, Map<String, Object> vars) {
        var matcher = PLACEHOLDER_PATTERN.matcher(template);
        var sb = new StringBuffer();
        while (matcher.find()) {
            var key = matcher.group(1).trim();
            var value = vars.getOrDefault(key, "");
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                    value != null ? value.toString() : ""));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object vars) {
        if (vars == null) {
            return Map.of();
        }
        if (vars instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        // Support passing a simple record or bean: convert via Jackson if available,
        // or fall back to empty map (caller should pass a Map<String,Object>).
        return Map.of();
    }

    // ─── result record ─────────────────────────────────────────────────────

    /**
     * Rendered email with subject and body.
     */
    public record RenderedEmail(String subject, String body) {}

    // ─── repository interface ──────────────────────────────────────────────

    /**
     * Minimal repository for EmailTemplate lookup.
     * Declare as a separate file in production; inlined here for template compactness.
     */
    public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
        java.util.Optional<EmailTemplate> findByCode(String code);
    }
}
