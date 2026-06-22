package com.ax.template.authblueprint.authzparity;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for authorization-parity-l0. Structural assertions a deliberate break cannot pass
 * silently: the authorization envelope is immutable (action type / authorized params / parity hash
 * / high-value / requester / required gates all @Column(updatable=false)); the signoff carries the
 * @Check separating the approver from the requester; the blocked-attempt and gate rows are fully
 * append-only with their uniqueness/backstop constraints; NO delete path exists anywhere in the
 * domain; mutators are package-sealed; the execute/signoff write paths use the PESSIMISTIC_WRITE
 * finder; and the migration carries the same backstops.
 */
@Tag("AUTHZPARITY")
class AuthzParityViolationProofTest {

    private static String src(String simpleName) throws Exception {
        return Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "authzparity", simpleName + ".java"));
    }

    // ── AUTHZPARITY-ENVELOPE-001 — the envelope is immutable and @Check-bounded ──
    @Test @Tag("AUTHZPARITY-ENVELOPE-001")
    void violation_envelopeImmutable_andExecutedBounded() throws Exception {
        for (String f : new String[]{"actionType", "authorizedParams", "parityHash", "highValue",
                "requesterUserId"}) {
            Column col = AuthorizedAction.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("AuthorizedAction." + f + " must be immutable").isFalse();
        }
        // the declared gate set is structurally immutable: getRequiredGates() returns a read-only
        // Set.copyOf view (Hibernate forbids updatable=false on an element-collection value column).
        AuthorizedAction sample = new AuthorizedAction(java.util.UUID.randomUUID(), "T", "p=1", "h",
            false, "req", java.util.Set.of("BUDGET_CHECK"), java.time.Instant.now());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> sample.getRequiredGates().add("X"))
            .as("AuthorizedAction.getRequiredGates() must be an unmodifiable view")
            .isInstanceOf(UnsupportedOperationException.class);

        Check check = AuthorizedAction.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("status = 'AUTHORIZED' OR executed_at IS NOT NULL");

        assertThat(AuthorizedAction.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        // markExecuted is the sole-mutator hook — package-private, not public
        Method exec = Arrays.stream(AuthorizedAction.class.getDeclaredMethods())
            .filter(m -> m.getName().equals("markExecuted")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(exec.getModifiers()))
            .as("AuthorizedAction.markExecuted must be package-private").isFalse();
        // no public setter
        for (Method m : AuthorizedAction.class.getMethods()) {
            assertThat(m.getName()).as("AuthorizedAction must have no public setter").doesNotStartWith("set");
        }
    }

    // ── AUTHZPARITY-FOUREYES-001 — signoff append-only, @Check approver<>requester, unique per approver ──
    @Test @Tag("AUTHZPARITY-FOUREYES-001")
    void violation_signoffAppendOnly_separationBackstopped() throws Exception {
        for (Method m : ActionSignoff.class.getMethods()) {
            assertThat(m.getName()).as("ActionSignoff must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "actionId", "approverUserId", "requesterUserId", "signedAt"}) {
            Column col = ActionSignoff.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ActionSignoff." + f + " must be immutable").isFalse();
        }
        Check check = ActionSignoff.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("approver_user_id <> requester_user_id");
        jakarta.persistence.Table table = ActionSignoff.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("action_id", "approver_user_id");
    }

    // ── AUTHZPARITY-GATES + EXEC — gate & blocked-attempt rows append-only + bounded ──
    @Test @Tag("AUTHZPARITY-GATES-001") @Tag("AUTHZPARITY-EXEC-001")
    void violation_gateAndBlockedRowsAppendOnly() throws Exception {
        for (Class<?> member : new Class<?>[]{GateSatisfaction.class, BlockedAttempt.class}) {
            for (Method m : member.getMethods()) {
                assertThat(m.getName()).as(member.getSimpleName() + " must have no public setter")
                    .doesNotStartWith("set");
            }
            for (java.lang.reflect.Field fld : member.getDeclaredFields()) {
                Column col = fld.getAnnotation(Column.class);
                if (col != null) {
                    assertThat(col.updatable()).as(member.getSimpleName() + "." + fld.getName()
                        + " must be immutable").isFalse();
                }
            }
        }
        jakarta.persistence.Table gateTable = GateSatisfaction.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(gateTable.uniqueConstraints()[0].columnNames()).containsExactly("action_id", "gate_key");
        Check blockedCheck = BlockedAttempt.class.getAnnotation(Check.class);
        assertThat(blockedCheck.constraints().replaceAll("\\s+", " "))
            .contains("offered_hash <> authorized_hash");
    }

    // ── AUTHZPARITY — NO delete path anywhere; mutators sealed ──
    @Test @Tag("AUTHZPARITY-EXEC-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : AuthorizedActionRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String s : new String[]{"AuthorizationParityService", "AuthorizationParityController"}) {
            String text = src(s);
            assertThat(text).as(s + " must contain no delete call — an action is permanent")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── AUTHZPARITY-CONCURRENT-001 — write paths use the locked finder ──
    @Test @Tag("AUTHZPARITY-CONCURRENT-001")
    void violation_lockedFinder_onWritePaths() throws Exception {
        Method locked = AuthorizedActionRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = src("AuthorizationParityService");
        for (String method : new String[]{"public AuthorizedAction execute(",
                "public ActionSignoff signoff(", "public GateSatisfaction satisfyGate("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).as(method + " must lock the action row").contains("findByIdForUpdate");
        }
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("AUTHZPARITY-ENVELOPE-001") @Tag("AUTHZPARITY-FOUREYES-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V049__create_authz_parity.sql")) {
            assertThat(in).as("V049__create_authz_parity.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("status = 'AUTHORIZED' OR executed_at IS NOT NULL");
            assertThat(sql).contains("approver_user_id <> requester_user_id");
            assertThat(sql).contains("offered_hash <> authorized_hash");
            assertThat(sql).contains("uq_signoff_action_approver");
            assertThat(sql).contains("uq_gate_action_key");
        }
    }
}
