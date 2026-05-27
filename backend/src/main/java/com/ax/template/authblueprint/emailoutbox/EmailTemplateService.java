package com.ax.template.authblueprint.emailoutbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable substitution + versioned-update orchestration for {@link EmailTemplate}.
 *
 * <p>EMAIL-TEMPLATE-001 — substitutes <code>{{varName}}</code> placeholders
 * with values from the supplied map. <strong>Missing variable keys
 * produce empty strings, not exceptions</strong> — operator who edits
 * a template late should not break a previously-working downstream
 * caller that omits a now-optional variable.
 *
 * <p>R60 iter1 F10 closure via Wave D2 — {@link #upsertTemplate} bumps the
 * template's {@link EmailTemplate#getVersion version} on every update and
 * captures the PRE-update snapshot in {@link EmailTemplateHistory}.
 * {@link #getTemplateAtVersion} is the forensic lookup used to answer
 * "which template version produced this historical email".
 */
@Service
public class EmailTemplateService {

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final EmailTemplateRepository templateRepository;
    private final EmailTemplateHistoryRepository historyRepository;
    private final Clock clock;

    public EmailTemplateService(EmailTemplateRepository templateRepository,
                                 EmailTemplateHistoryRepository historyRepository,
                                 Clock clock) {
        this.templateRepository = templateRepository;
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    /**
     * Render the (subject, body) pair for a template code with the
     * given variable map. Throws if the template code does not exist.
     */
    public Rendered render(String templateCode, Map<String, String> vars) {
        EmailTemplate template = templateRepository.findById(templateCode)
            .orElseThrow(() -> new IllegalArgumentException("unknown template: " + templateCode));
        String subject = substitute(template.getSubjectTemplate(), vars);
        String body = substitute(template.getBodyTemplate(), vars);
        return new Rendered(subject, body);
    }

    /**
     * R60 iter1 F10 / Wave D2 — create or update a template with
     * versioned-history capture. New template persists at version 1 plus
     * an initial history row; an existing template's PRE-update snapshot
     * is written to history, then the live row is updated and its version
     * incremented.
     *
     * <p>The history write and the live update share a single transaction —
     * either both land or neither does.
     */
    @Transactional
    public EmailTemplate upsertTemplate(String templateCode, String subjectTemplate, String bodyTemplate) {
        Instant now = Instant.now(clock);
        return templateRepository.findById(templateCode)
            .map(existing -> {
                historyRepository.save(EmailTemplateHistory.snapshot(existing, now));
                existing.applyUpdate(subjectTemplate, bodyTemplate);
                return templateRepository.save(existing);
            })
            .orElseGet(() -> {
                EmailTemplate fresh = new EmailTemplate(templateCode, subjectTemplate, bodyTemplate, 1);
                EmailTemplate saved = templateRepository.save(fresh);
                historyRepository.save(EmailTemplateHistory.snapshot(saved, now));
                return saved;
            });
    }

    /**
     * Forensic lookup — return the immutable snapshot of the template at the
     * given version, or {@code null} if no such snapshot exists.
     */
    @Transactional(readOnly = true)
    public EmailTemplateHistory getTemplateAtVersion(String templateCode, int version) {
        return historyRepository.findByTemplateCodeAndVersion(templateCode, version).orElse(null);
    }

    private static String substitute(String template, Map<String, String> vars) {
        if (template == null) return "";
        Matcher m = VAR.matcher(template);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            String replacement = vars == null ? "" : vars.getOrDefault(key, "");
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    public record Rendered(String subject, String body) {}
}
