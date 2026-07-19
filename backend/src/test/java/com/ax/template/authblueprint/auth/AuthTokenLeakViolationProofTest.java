package com.ax.template.authblueprint.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D1 — VIOLATION proof that the auth reference workload never emits its DB-persisted,
 * single-use VERIFY / RESET bearer tokens UNCONDITIONALLY. Mirrors the emailoutbox
 * ViolationProof convention (structural invariants that would re-open a credential-leak
 * surface if relaxed).
 *
 * <p>Historically {@code AuthServiceImpl} dumped tokens via raw
 * {@code System.out.println("[AUTH-TOKEN] ...token=" + token)} with no profile / opt-in
 * gate — leaking bearer credentials into infra logs. They are now routed through the
 * gated {@link DevTokenSink}.
 */
@Tag("ASVS")
class AuthTokenLeakViolationProofTest {

    /** Auth module main-source root (working dir is the {@code backend} module). */
    private static final Path AUTH_MAIN_DIR =
            Path.of("src/main/java/com/ax/template/authblueprint/auth");

    @Test
    @Tag("ASVS-AUTH-TOKEN-LEAK-001")
    void violation_noRawSystemOutOrErrSinkInAnyAuthMainSource() throws Exception {
        // Falsification: a raw System.out / System.err in ANY auth/ main source is exactly
        // the unconditional (ungated) sink we forbid — every dev token dump MUST route
        // through the gated DevTokenSink. Broadened from AuthServiceImpl-only so a
        // re-introduced raw sink is caught wherever it lands in the auth module.
        assertThat(AUTH_MAIN_DIR).exists();
        try (Stream<Path> paths = Files.walk(AUTH_MAIN_DIR)) {
            List<Path> offenders = paths
                    .filter(p -> p.getFileName().toString().endsWith(".java"))
                    .filter(AuthTokenLeakViolationProofTest::usesRawStdSink)
                    .collect(Collectors.toList());
            assertThat(offenders)
                .as("no auth/ main source may emit tokens/secrets via a raw System.out / "
                  + "System.err sink — dev token dumps MUST route through the gated DevTokenSink")
                .isEmpty();
        }
    }

    private static boolean usesRawStdSink(Path javaFile) {
        try {
            String source = Files.readString(javaFile);
            return source.contains("System.out") || source.contains("System.err");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @Tag("ASVS-AUTH-TOKEN-LEAK-002")
    void violation_devTokenSinkGatedOffInProdAndDefaultWithoutOptIn() {
        // prod / production profiles without opt-in MUST be a no-op sink.
        assertThat(new DevTokenSink(false, env("prod")).isEnabled())
            .as("prod profile without opt-in must not expose tokens").isFalse();
        assertThat(new DevTokenSink(false, env("production")).isEnabled())
            .as("production profile without opt-in must not expose tokens").isFalse();
        // Empty active profiles == the default state (getActiveProfiles() is empty when only
        // the default profile is active) — the fork-receiver-never-sets-a-profile prod-safe
        // case MUST be a no-op.
        assertThat(new DevTokenSink(false, env()).isEnabled())
            .as("default (no active profile) must be a no-op").isFalse();
        assertThat(new DevTokenSink(false, env("test")).isEnabled())
            .as("non-dev (test) profile without opt-in must be a no-op").isFalse();
        // Exact-match guard: a profile that merely CONTAINS "dev" as a substring is NOT the
        // dev profile and must not emit (regression proof for the old substring match).
        assertThat(new DevTokenSink(false, env("livedev")).isEnabled())
            .as("a prod-ish profile named 'livedev' must not be treated as dev").isFalse();
        // prod + dev together MUST stay a no-op — the exact prod match vetoes the dev match.
        assertThat(new DevTokenSink(false, env("prod", "dev")).isEnabled())
            .as("prod present alongside dev must veto emission (no-op)").isFalse();
    }

    @Test
    @Tag("ASVS-AUTH-TOKEN-LEAK-003")
    void violation_devTokenSinkEnabledOnlyByOptInOrDevProfile() {
        // The ONLY ways to expose tokens: explicit opt-in (any profile, incl. prod),
        // or the exact dev profile.
        assertThat(new DevTokenSink(true, env("prod")).isEnabled())
            .as("explicit opt-in overrides the prod block").isTrue();
        assertThat(new DevTokenSink(true, env()).isEnabled())
            .as("explicit opt-in enables even with no active profile").isTrue();
        assertThat(new DevTokenSink(false, env("dev")).isEnabled())
            .as("exact dev profile enables the convenience dump").isTrue();
    }

    /** A test {@link Environment} whose {@code getActiveProfiles()} returns exactly {@code profiles}. */
    private static Environment env(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        if (profiles.length > 0) {
            environment.setActiveProfiles(profiles);
        }
        return environment;
    }
}
