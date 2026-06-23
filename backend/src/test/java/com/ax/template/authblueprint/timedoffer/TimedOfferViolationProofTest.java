package com.ax.template.authblueprint.timedoffer;

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
 * VIOLATION proof for timed-offer-exclusive-assignment-l0. Structural assertions a deliberate break
 * cannot pass silently: the offer/assignment carry @Version + immutable identity columns + no public
 * setters; the exclusivity backstop is a uq(subject_id) on the Assignment row; the accept path uses
 * the subject-wide PESSIMISTIC_WRITE finder; the @Scheduled sweep reaches expireOne through an @Lazy
 * self proxy (never a bare self-invocation); the re-offer ladder is append-only with NO delete path;
 * the @Check backstops are present; and the migration carries the same backstops.
 */
@Tag("TIMEDOFFER")
class TimedOfferViolationProofTest {

    // ── TIMEDOFFER-LIFECYCLE-001 — the offer has no public setter; status moves via the machine ──
    @Test @Tag("TIMEDOFFER-LIFECYCLE-001")
    void violation_offerNoPublicSetter_versionPresent() throws Exception {
        for (Method m : TimedOffer.class.getMethods()) {
            assertThat(m.getName()).as("TimedOffer must have no public setter").doesNotStartWith("set");
        }
        // status moves through the state machine only — the sole-mutator hook is package-private
        Method decide = java.util.Arrays.stream(TimedOffer.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("decide")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(decide.getModifiers()))
            .as("TimedOffer.decide must be package-private").isFalse();
        assertThat(TimedOffer.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── TIMEDOFFER-LIFECYCLE/LADDER-001 — immutable identity/ladder columns on the offer ──
    @Test @Tag("TIMEDOFFER-LIFECYCLE-001") @Tag("TIMEDOFFER-LADDER-001")
    void violation_offerImmutableColumns() throws Exception {
        for (String f : new String[]{"id", "subjectId", "candidate", "deadline", "attemptSeq",
                                      "priorOfferId", "createdAt"}) {
            Column col = TimedOffer.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("TimedOffer." + f + " must be immutable").isFalse();
        }
        // the @Check backstops: positive attempt, OPEN<->decided-at coupling
        Check check = TimedOffer.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("attempt_seq >= 1");
        assertThat(c).contains("status <> 'OPEN' OR decided_at IS NULL");
        assertThat(c).contains("status = 'OPEN' OR decided_at IS NOT NULL");
    }

    // ── TIMEDOFFER-EXCLUSIVE-001 — the Assignment carries uq(subject_id) + immutable columns, no setter ──
    @Test @Tag("TIMEDOFFER-EXCLUSIVE-001")
    void violation_assignmentUniqueSubject_immutable_noSetter() throws Exception {
        for (Method m : Assignment.class.getMethods()) {
            assertThat(m.getName()).as("Assignment must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subjectId", "offerId", "candidate", "assignedAt"}) {
            Column col = Assignment.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Assignment." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = Assignment.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .as("uq(subject_id) is the exclusivity backstop").containsExactly("subject_id");
    }

    // ── TIMEDOFFER-CONCURRENT-001 — the accept path uses the subject-wide PESSIMISTIC_WRITE finder ──
    @Test @Tag("TIMEDOFFER-CONCURRENT-001")
    void violation_subjectLockedFinder_andSerializedAccept() throws Exception {
        Method locked = TimedOfferRepository.class.getMethod("findBySubjectIdForUpdate", String.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).as("findBySubjectIdForUpdate must carry @Lock").isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "timedoffer", "TimedOfferService.java"));
        int start = svc.indexOf("public TimedOffer accept(");
        assertThat(start).as("accept must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("accept must take the subject-wide lock").contains("findBySubjectIdForUpdate");
        assertThat(body).as("accept must create an Assignment (the uq backstop)").contains("assignments.saveAndFlush");
    }

    // ── TIMEDOFFER-LIFECYCLE-001 — the @Scheduled sweep reaches expireOne through the @Lazy self proxy ──
    @Test @Tag("TIMEDOFFER-LIFECYCLE-001")
    void violation_sweepReachesExpireThroughLazySelfProxy() throws Exception {
        String sweeper = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "timedoffer", "TimedOfferSweeper.java"));
        // the @Lazy self field must exist and the sweep must call self.expireOne (NOT this.expireOne)
        assertThat(sweeper).as("@Lazy self-injection breaks the proxy self-invocation trap")
            .contains("@Lazy TimedOfferSweeper self");
        assertThat(sweeper).as("the tick must reach expireOne through the proxy")
            .contains("self.expireOne(");
        assertThat(sweeper).as("a bare self-invocation would bypass @Transactional")
            .doesNotContain("this.expireOne(");
        // expireOne is REQUIRES_NEW so the cross-bean call gets its own per-row transaction
        assertThat(sweeper).contains("Propagation.REQUIRES_NEW");
    }

    // ── TIMEDOFFER-LADDER-001 — NO delete path anywhere in the domain (the ladder is append-only) ──
    @Test @Tag("TIMEDOFFER-LADDER-001")
    void violation_noDeletePath() throws Exception {
        for (Method m : TimedOfferRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (Method m : AssignmentRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"TimedOfferService", "TimedOfferSweeper", "TimedOfferController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "timedoffer", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — the ladder is append-only")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("TIMEDOFFER-EXCLUSIVE-001") @Tag("TIMEDOFFER-LADDER-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V055__create_timedoffer.sql")) {
            assertThat(in).as("V055__create_timedoffer.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("attempt_seq >= 1");
            assertThat(sql).contains("status <> 'OPEN' OR decided_at IS NULL");
            assertThat(sql).contains("status = 'OPEN' OR decided_at IS NOT NULL");
            assertThat(sql).contains("UNIQUE INDEX uq_timed_offer_subject");
            assertThat(sql).contains("(subject_id)");
        }
    }
}
