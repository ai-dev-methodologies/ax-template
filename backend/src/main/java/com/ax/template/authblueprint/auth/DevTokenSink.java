package com.ax.template.authblueprint.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Dev-only sink for the email-less reference flow's single-use VERIFY / RESET bearer
 * tokens. The auth reference workload has no wired email adapter, so signup / resend /
 * password-reset dump the freshly-issued token to the console for a developer to complete
 * the flow by hand.
 *
 * <p>These tokens are DB-persisted, single-use bearer credentials — emitting them
 * unconditionally leaks credentials into infra logs (the same class of mistake as putting
 * a bearer token in a URL). This sink therefore gates the emission on the resolved
 * {@link Environment#getActiveProfiles() active profiles}, matched by <b>exact element
 * equality</b> (never substring): a profile literally named e.g. {@code livedev} or
 * {@code dev-shared} is NOT the {@code dev} profile and must never enable emission.
 *
 * <p><b>Production is an unconditional veto.</b> When an exact {@code prod} / {@code production}
 * profile is active, this sink NEVER emits — the opt-in property CANNOT override the prod
 * block. A bearer-credential dump has no legitimate use in production, so the opt-in escape
 * hatch only exists in non-production environments. This is stricter than the R47 email
 * dev-stub pattern, because the hazard here is a credential leak, not a functional no-op.
 * <ul>
 *   <li>an exact {@code prod} / {@code production} profile — ALWAYS a no-op, regardless of
 *       the opt-in property;</li>
 *   <li>otherwise (non-production): the exact {@code dev} profile OR the explicit opt-in
 *       ({@code ax.auth.expose-dev-tokens=true}) enables emission — the opt-in only
 *       force-enables in non-production environments (e.g. staging);</li>
 *   <li>anything else is a SILENT no-op. In particular, empty/absent active profiles (the
 *       default state — {@code getActiveProfiles()} returns an empty array when only the
 *       default profile is active) is a no-op: the fork-receiver-never-sets-a-profile case
 *       is prod-safe by construction.</li>
 * </ul>
 *
 * <p>Mirrors {@code emailoutbox.LoggingEmailSenderConfig}'s R47 dev-stub prod hard-stop,
 * except a missing token dump is a no-op rather than a startup throw — a convenience dump
 * must never break the auth flow.
 */
@Component
public class DevTokenSink {

    private static final Logger LOG = LoggerFactory.getLogger("auth.dev-token-sink");

    private final boolean enabled;

    public DevTokenSink(
            @Value("${ax.auth.expose-dev-tokens:false}") boolean exposeOptIn,
            Environment environment) {
        boolean isProd = false;
        boolean isDev = false;
        // Exact element match against the resolved active profiles — NOT substring. An
        // empty array (default profile only) leaves both flags false → no-op (prod-safe).
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                isProd = true;
            } else if ("dev".equals(profile)) {
                isDev = true;
            }
        }
        // Production is an ABSOLUTE veto: an exact prod/production profile NEVER emits,
        // regardless of the opt-in. In non-production, emit if the dev profile is active OR
        // the explicit opt-in property is set (the opt-in only force-enables off-prod).
        this.enabled = !isProd && (isDev || exposeOptIn);
    }

    boolean isEnabled() {
        return enabled;
    }

    /**
     * Emit a dev token dump — a no-op unless {@link #isEnabled()}. When enabled, logs the
     * token type, email, and the raw token so an email-less developer can complete the flow.
     */
    void emit(String tokenType, String email, String token) {
        if (!enabled) {
            return;
        }
        LOG.info("[AUTH-TOKEN] type={} email={} token={}", tokenType, email, token);
    }
}
