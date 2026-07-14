package com.ax.template.authblueprint.bilateralhandoff;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for bilateral-handoff-l0. Structural assertions a deliberate break cannot pass
 * silently: no public setter, @Version present, immutable party columns, status mutated only
 * through {@link HandoffStateMachine} (the complete/voidHandoff hooks are package-private), the
 * accept/confirm path takes the row-wide PESSIMISTIC_WRITE lock, no delete path, and the @Check
 * backstops (a COMPLETED row always has both confirmations; custody is always a named party) are
 * present on both the entity and the migration.
 */
@Tag("BILATERALHANDOFF")
class HandoffViolationProofTest {

    // ── BHO-FSM-001 — no public setter; status mutated only through the state machine ──
    @Test @Tag("BHO-FSM-001")
    void violation_handoffNoPublicSetter_versionPresent() throws Exception {
        for (Method m : Handoff.class.getMethods()) {
            assertThat(m.getName()).as("Handoff must have no public setter").doesNotStartWith("set");
        }
        for (String hook : new String[]{"complete", "voidHandoff", "markReleasorConfirmed", "markReceiverConfirmed"}) {
            Method m = java.util.Arrays.stream(Handoff.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers())).as("Handoff." + hook + " must be package-private").isFalse();
        }
        assertThat(Handoff.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── BHO-FSM-001 — the two named parties are immutable after proposal ──
    @Test @Tag("BHO-FSM-001")
    void violation_partiesImmutable() throws Exception {
        for (String f : new String[]{"id", "releasorParty", "receiverParty", "createdAt"}) {
            Column col = Handoff.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Handoff." + f + " must be immutable").isFalse();
        }
        // the @Check backstops: a COMPLETED row always has both confirmations; custody is a named party
        Check check = Handoff.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("releasor_confirmed_at IS NOT NULL AND receiver_confirmed_at IS NOT NULL");
        assertThat(c).contains("custody_holder = releasor_party OR custody_holder = receiver_party");
    }

    // ── BHO-ATOMIC-001 — the confirm path takes the row-wide PESSIMISTIC_WRITE lock ──
    @Test @Tag("BHO-ATOMIC-001")
    void violation_confirmTakesPessimisticWriteLock() throws Exception {
        Method locked = HandoffRepository.class.getMethod("findByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).as("findByIdForUpdate must carry @Lock").isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "bilateralhandoff", "HandoffService.java"));
        int start = svc.indexOf("public Handoff confirm(");
        assertThat(start).as("confirm must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("confirm must take the row-wide lock").contains("findByIdForUpdate");
    }

    // ── BHO-VOID-001 — no delete path anywhere in the domain ──
    @Test @Tag("BHO-VOID-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : HandoffRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"HandoffService", "HandoffController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "bilateralhandoff", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — a handoff is completed or voided, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same @Check backstops ──
    @Test @Tag("BHO-FSM-001") @Tag("BHO-ATOMIC-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V097__create_bilateralhandoff.sql")) {
            assertThat(in).as("V097__create_bilateralhandoff.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("releasor_confirmed_at IS NOT NULL AND receiver_confirmed_at IS NOT NULL");
            assertThat(sql).contains("custody_holder = releasor_party OR custody_holder = receiver_party");
        }
    }
}
