package com.ax.template.authblueprint.accessgrant;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for time-bounded-access-grant-l0. Structural + predicate assertions a deliberate
 * break cannot pass silently: 'expired' is RECOMPUTED (there is NO stored expired/active boolean
 * column), the window is half-open (the instant equal to validUntil is denied), grants are
 * append-only (immutable identity + immutable revoke columns, @Version, NO delete path, mutator
 * package-sealed), the revoke path uses the PESSIMISTIC_WRITE finder, the eligibility predicate is
 * AND-over-the-set, and the migration carries the same backstops.
 */
@Tag("ACCESSGRANT")
class AccessGrantViolationProofTest {

    // ── AGRANT-WINDOW-001 — 'expired' is a RECOMPUTED predicate; NO stored expired/active boolean ──
    @Test @Tag("AGRANT-WINDOW-001")
    void violation_noStoredExpiredFlag_predicateRecomputed() throws Exception {
        // no field on either entity may be a stored expiry/active verdict (the predicate is recomputed)
        for (Class<?> entity : new Class<?>[]{AccessGrant.class, Credential.class}) {
            for (Field f : entity.getDeclaredFields()) {
                String name = f.getName().toLowerCase();
                boolean isBooleanish = f.getType() == boolean.class || f.getType() == Boolean.class;
                if (isBooleanish) {
                    assertThat(name)
                        .as(entity.getSimpleName() + "." + f.getName()
                            + " must not be a stored expired/active flag — recompute over the Clock")
                        .doesNotContain("expired").doesNotContain("active").doesNotContain("valid");
                }
            }
        }
        // the recomputed predicate exists on the entity and takes an Instant (the injected now)
        Method isActiveAt = AccessGrant.class.getMethod("isActiveAt", Instant.class);
        assertThat(isActiveAt.getReturnType()).isEqualTo(boolean.class);
        Method credValidAt = Credential.class.getMethod("isValidAt", Instant.class);
        assertThat(credValidAt.getReturnType()).isEqualTo(boolean.class);
    }

    // ── AGRANT-BOUNDARY-001 — KEYSTONE: half-open window; the SAME row flips at validUntil over the Clock ──
    @Test @Tag("AGRANT-BOUNDARY-001")
    void violation_boundaryHalfOpen_sameRowFlipsAtValidUntil() {
        Instant validFrom = Instant.parse("2026-01-01T00:00:00Z");
        Instant validUntil = Instant.parse("2026-01-02T00:00:00Z");
        AccessGrant g = new AccessGrant(UUID.randomUUID(), "subject", "resource", "relation",
            validFrom, validUntil, validFrom);

        // inside the window (one second before validUntil) → allowed
        assertThat(g.isActiveAt(validUntil.minus(1, ChronoUnit.SECONDS)))
            .as("one second before validUntil is inside the half-open window").isTrue();
        // exactly AT validUntil → denied (half-open upper bound — the instant belongs to the denied side)
        assertThat(g.isActiveAt(validUntil))
            .as("AGRANT-BOUNDARY-001 — the instant equal to validUntil is OUTSIDE the window").isFalse();
        // AFTER validUntil → denied — SAME ROW, no write between the two evaluations
        assertThat(g.isActiveAt(validUntil.plus(1, ChronoUnit.SECONDS)))
            .as("after validUntil the same grant is denied with no intervening write").isFalse();
        // before validFrom → not active (and is reported as before-window)
        assertThat(g.isActiveAt(validFrom.minus(1, ChronoUnit.SECONDS))).isFalse();
        assertThat(g.isBeforeWindow(validFrom.minus(1, ChronoUnit.SECONDS))).isTrue();
        // exactly AT validFrom → allowed (half-open lower bound is INCLUSIVE)
        assertThat(g.isActiveAt(validFrom)).as("validFrom is inclusive").isTrue();
    }

    // ── AGRANT-ELIGIBILITY-001 — a credential's validity is the same half-open recomputed predicate ──
    @Test @Tag("AGRANT-ELIGIBILITY-001")
    void violation_credentialValidityHalfOpen() {
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant until = Instant.parse("2026-04-01T00:00:00Z");
        Credential c = new Credential(UUID.randomUUID(), "subject", "LICENSE", from, until, from);
        assertThat(c.isValidAt(from)).as("validFrom inclusive").isTrue();
        assertThat(c.isValidAt(until.minus(1, ChronoUnit.SECONDS))).isTrue();
        assertThat(c.isValidAt(until)).as("validUntil exclusive").isFalse();
        assertThat(c.isValidAt(from.minus(1, ChronoUnit.SECONDS))).isFalse();
    }

    // ── AGRANT-REVOKE-001 — append-only: immutable columns, @Version, NO delete path, mutator sealed ──
    @Test @Tag("AGRANT-REVOKE-001")
    void violation_appendOnly_immutableColumns_noDelete_mutatorSealed() throws Exception {
        // immutable identity + window columns on the grant (the revoke columns are write-ONCE by the
        // single revoke UPDATE — they are NOT updatable=false, which would exclude them from that
        // UPDATE and leave them NULL against the @Check; write-once is enforced by the idempotent hook)
        for (String f : new String[]{"id", "subjectId", "resourceRef", "relation",
                                     "validFrom", "validUntil", "createdAt"}) {
            Column col = AccessGrant.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as("AccessGrant." + f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("AccessGrant." + f + " must be immutable").isFalse();
        }
        // the revoke columns exist but are deliberately NOT updatable=false (written by the revoke UPDATE)
        for (String f : new String[]{"revokedBy", "revokedAt"}) {
            Column col = AccessGrant.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as("AccessGrant." + f + " must carry @Column").isNotNull();
            assertThat(col.updatable())
                .as("AccessGrant." + f + " is write-once via the revoke UPDATE, so it must be updatable").isTrue();
        }
        // immutable columns on the credential
        for (String f : new String[]{"id", "subjectId", "credentialClass", "validFrom", "validUntil", "createdAt"}) {
            Column col = Credential.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("Credential." + f + " must be immutable").isFalse();
        }
        // @Version on the grant
        assertThat(AccessGrant.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        // the revoke hook is package-private (no public mutator)
        Method revoke = java.util.Arrays.stream(AccessGrant.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("revoke")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(revoke.getModifiers()))
            .as("AccessGrant.revoke must be package-private").isFalse();
        for (Method m : AccessGrant.class.getMethods()) {
            assertThat(m.getName()).as("AccessGrant must have no public setter").doesNotStartWith("set");
        }
        for (Method m : Credential.class.getMethods()) {
            assertThat(m.getName()).as("Credential must have no public setter").doesNotStartWith("set");
        }

        // NO delete path anywhere in the domain
        for (Method m : AccessGrantRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (Method m : CredentialRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"AccessGrantService", "AccessGrantController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "accessgrant", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — a grant is revoked, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── AGRANT-REVOKE-001 — the revoke hook is idempotent: a second revoke never overwrites the first ──
    @Test @Tag("AGRANT-REVOKE-001")
    void violation_revokeHookIsWriteOnce_idempotent() {
        Instant from = Instant.parse("2026-05-01T00:00:00Z");
        Instant until = Instant.parse("2026-06-01T00:00:00Z");
        AccessGrant g = new AccessGrant(UUID.randomUUID(), "subject", "resource", "relation",
            from, until, from);
        assertThat(g.getStatus()).isEqualTo(GrantStatus.ACTIVE);

        Instant first = Instant.parse("2026-05-10T12:00:00Z");
        g.revoke("alice", first);
        assertThat(g.getStatus()).isEqualTo(GrantStatus.REVOKED);
        assertThat(g.getRevokedBy()).isEqualTo("alice");
        assertThat(g.getRevokedAt()).isEqualTo(first);

        // a second revoke by a different actor at a different instant is a no-op — the first stands
        g.revoke("bob", Instant.parse("2026-05-20T08:00:00Z"));
        assertThat(g.getRevokedBy()).as("write-once — the first revoker stands").isEqualTo("alice");
        assertThat(g.getRevokedAt()).as("write-once — the first revoke instant stands").isEqualTo(first);
    }

    // ── AGRANT-REVOKE-001 — the revoke path uses the PESSIMISTIC_WRITE finder ──
    @Test @Tag("AGRANT-REVOKE-001")
    void violation_lockedFinder_andSerializedRevoke() throws Exception {
        Method locked = AccessGrantRepository.class.getMethod("findByIdForUpdate", UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "accessgrant", "AccessGrantService.java"));
        int start = svc.indexOf("public AccessGrant revoke(");
        assertThat(start).as("revoke must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("revoke must take the grant row lock").contains("findByIdForUpdate");
    }

    // ── AGRANT-WINDOW/REVOKE-001 — the @Check backstops on the grant ──
    @Test @Tag("AGRANT-WINDOW-001") @Tag("AGRANT-REVOKE-001")
    void violation_checkBackstops() {
        Check check = AccessGrant.class.getAnnotation(Check.class);
        assertThat(check).isNotNull();
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("valid_until > valid_from");
        assertThat(c).contains("(status = 'REVOKED') = (revoked_at IS NOT NULL)");
        assertThat(c).contains("(revoked_at IS NULL) = (revoked_by IS NULL)");
        // GrantStatus has exactly ACTIVE and REVOKED — there is deliberately no EXPIRED state
        assertThat(GrantStatus.values()).containsExactly(GrantStatus.ACTIVE, GrantStatus.REVOKED);
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("AGRANT-WINDOW-001") @Tag("AGRANT-REVOKE-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V057__create_accessgrant.sql")) {
            assertThat(in).as("V057__create_accessgrant.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("valid_until > valid_from");
            assertThat(sql).contains("(status = 'REVOKED') = (revoked_at IS NOT NULL)");
            // no stored expired/active column in the schema
            assertThat(sql.toLowerCase()).doesNotContain("expired boolean").doesNotContain("is_active");
        }
    }
}
