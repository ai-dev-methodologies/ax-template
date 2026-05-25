package com.ax.template.authblueprint.emailoutbox;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Variable substitution for {@link EmailTemplate}.
 *
 * <p>EMAIL-TEMPLATE-001 — substitutes <code>{{varName}}</code> placeholders
 * with values from the supplied map. <strong>Missing variable keys
 * produce empty strings, not exceptions</strong> — operator who edits
 * a template late should not break a previously-working downstream
 * caller that omits a now-optional variable.
 */
@Service
public class EmailTemplateService {

    private static final Pattern VAR = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*}}");

    private final EmailTemplateRepository templateRepository;

    public EmailTemplateService(EmailTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
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
