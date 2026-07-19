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
 * <ul>
 *   <li>explicit opt-in ({@code ax.auth.expose-dev-tokens=true}) — the ONLY way to expose
 *       tokens under a {@code prod} profile;</li>
 *   <li>the exact {@code dev} profile present in the active-profiles array (and no exact
 *       {@code prod}/{@code production}) — convenience for local development;</li>
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
        // Opt-in overrides the prod block; otherwise only an exact dev profile emits.
        this.enabled = exposeOptIn || (isDev && !isProd);
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
