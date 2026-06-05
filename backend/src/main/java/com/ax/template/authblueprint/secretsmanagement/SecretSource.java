package com.ax.template.authblueprint.secretsmanagement;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * SECRET-SOURCE-001 — externalized secret sourcing (The Twelve-Factor App, III. Config). A secret is
 * resolved at runtime from an external source (env var / secret store), NEVER from a committed
 * literal. Two guarantees this enforces:
 *
 * <ol>
 *   <li>FAIL-FAST on a config literal: {@link #requireExternalized(String, String)} rejects a
 *       property whose value is a NON-placeholder secret literal — a fork-receiver who pastes a real
 *       credential into {@code application.yml} gets a clear error, not a silent leak.</li>
 *   <li>PRESENCE-ONLY status: {@link #presenceStatus(String...)} returns {@code {key: present|absent}}
 *       booleans only — never a resolved value — so a config-status probe can show what is wired
 *       without ever echoing a secret.</li>
 * </ol>
 *
 * <p>A missing required secret aborts (the caller surfaces 500) rather than falling back to a
 * default. Spec: specs/secrets-management-l0.yaml#SECRET-SOURCE-001.
 */
@Component
public class SecretSource {

    /** A config value of this exact form is the ONLY allowed placeholder (it means "inject at runtime"). */
    public static final String UNRESOLVED_PLACEHOLDER = "__INJECT_AT_RUNTIME__";

    private final Environment environment;

    public SecretSource(Environment environment) {
        this.environment = environment;
    }

    /**
     * SECRET-SOURCE-001 fail-fast. A property bound from config MUST be either the explicit
     * {@link #UNRESOLVED_PLACEHOLDER} (to be injected from the env/store at runtime) or genuinely
     * absent. A real secret literal sitting in config is rejected → {@link SecretException.Kind#LITERAL_IN_CONFIG}
     * (the caller maps it to 500).
     */
    public void requireExternalized(String propertyName, String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()
                || UNRESOLVED_PLACEHOLDER.equals(configuredValue)) {
            return; // absent or the explicit "inject at runtime" placeholder — both acceptable
        }
        throw new SecretException(SecretException.Kind.LITERAL_IN_CONFIG,
                "Secret property '" + propertyName + "' carries a literal value in config; "
                        + "source it from the env/secret store instead.");
    }

    /** Resolve a required secret from the external environment, aborting if absent (no default fallback). */
    public String resolveRequired(String envKey) {
        String value = environment.getProperty(envKey);
        if (value == null || value.isBlank() || UNRESOLVED_PLACEHOLDER.equals(value)) {
            throw new SecretException(SecretException.Kind.NOT_FOUND,
                    "Required secret '" + envKey + "' is not present in the runtime environment.");
        }
        return value;
    }

    /** SECRET-SOURCE-001 presence-only status: booleans, never values. */
    public Map<String, Boolean> presenceStatus(String... envKeys) {
        java.util.LinkedHashMap<String, Boolean> out = new java.util.LinkedHashMap<>();
        for (String key : envKeys) {
            String value = environment.getProperty(key);
            out.put(key, value != null && !value.isBlank() && !UNRESOLVED_PLACEHOLDER.equals(value));
        }
        return out;
    }
}
