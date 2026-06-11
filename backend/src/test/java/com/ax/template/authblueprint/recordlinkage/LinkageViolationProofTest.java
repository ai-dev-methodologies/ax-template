package com.ax.template.authblueprint.recordlinkage;

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
 * VIOLATION proof for record-linkage-l0. Structural assertions a deliberate break cannot pass
 * silently: a bare unexplained verdict is unrepresentable (score/breakdown/thresholds immutable
 * and @Check-bounded), survivorship rows are fully append-only with one-per-field uniqueness,
 * NO delete path exists anywhere in the domain, mutators are package-sealed, write paths use the
 * PESSIMISTIC_WRITE finders in ascending-id order, and the migration carries the same backstops.
 */
@Tag("RECORDLINKAGE")
class LinkageViolationProofTest {

    // ── LINK-BAND-001 — the verdict's evidence is immutable and @Check-bounded ──
    @Test @Tag("LINK-BAND-001")
    void violation_verdictEvidenceImmutable_andBounded() throws Exception {
        for (String f : new String[]{"lowRecordId", "highRecordId", "score", "breakdownJson",
                "lowerThreshold", "upperThreshold", "band"}) {
            Column col = MatchProposal.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("MatchProposal." + f + " must be immutable").isFalse();
        }
        Check check = MatchProposal.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("score >= 0 AND score <= 1");
        assertThat(c).contains("lower_threshold < upper_threshold");
        assertThat(c).contains("status = 'PROPOSED' OR (decided_by IS NOT NULL AND decided_at IS NOT NULL)");
    }

    // ── LINK-SURVIVOR-001 — decisions append-only, one per (proposal, field) ──
    @Test @Tag("LINK-SURVIVOR-001")
    void violation_survivorshipAppendOnly_uniquePerField() throws Exception {
        for (Method m : SurvivorshipDecision.class.getMethods()) {
            assertThat(m.getName()).as("SurvivorshipDecision must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "proposalId", "fieldName", "winningValue",
                "sourceRecordId", "ruleApplied", "decidedAt"}) {
            Column col = SurvivorshipDecision.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("SurvivorshipDecision." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = SurvivorshipDecision.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("proposal_id", "field_name");
    }

    // ── LINK-SURVIVOR/RESOLVE-001 — NO delete path; tombstone implies pointer; mutators sealed ──
    @Test @Tag("LINK-SURVIVOR-001") @Tag("LINK-RESOLVE-001")
    void violation_noDeletePath_tombstonePointer_mutatorsSealed() throws Exception {
        // the repositories declare no delete*; the service/controller sources call no delete
        for (Method m : LinkageRecordRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"LinkageService", "LinkageController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "recordlinkage", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — records are tombstoned")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        Check check = LinkageRecord.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("status <> 'MERGED' OR merged_into_id IS NOT NULL");
        for (String hook : new String[]{"tombstone", "applySurvivorship"}) {
            Method m = java.util.Arrays.stream(LinkageRecord.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("LinkageRecord." + hook + " must be package-private").isFalse();
        }
        Method decide = java.util.Arrays.stream(MatchProposal.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("decide")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(decide.getModifiers()))
            .as("MatchProposal.decide must be package-private").isFalse();
        assertThat(LinkageRecord.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        assertThat(MatchProposal.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── LINK-CONCURRENT-001 — write paths use locked finders; ascending-id lock order ──
    @Test @Tag("LINK-CONCURRENT-001")
    void violation_lockedFinders_andAscendingLockOrder() throws Exception {
        for (Class<?> repo : new Class<?>[]{LinkageRecordRepository.class, MatchProposalRepository.class}) {
            Method locked = repo.getMethod("findByIdForUpdate", java.util.UUID.class);
            org.springframework.data.jpa.repository.Lock lock =
                locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
            assertThat(lock).isNotNull();
            assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        }
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "recordlinkage", "LinkageService.java"));
        for (String method : new String[]{"public MatchProposal propose(", "public MatchProposal confirm("}) {
            int start = svc.indexOf(method);
            assertThat(start).as(method + " must exist").isPositive();
            String body = svc.substring(start, svc.indexOf("\n    }", start));
            assertThat(body).contains("findByIdForUpdate");
        }
        assertThat(svc).as("ascending-id lock order is the deadlock guard")
            .contains("aId.compareTo(bId) < 0");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("LINK-BAND-001") @Tag("LINK-SURVIVOR-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V045__create_record_linkage.sql")) {
            assertThat(in).as("V045__create_record_linkage.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("status <> 'MERGED' OR merged_into_id IS NOT NULL");
            assertThat(sql).contains("status = 'PROPOSED' OR (decided_by IS NOT NULL AND decided_at IS NOT NULL)");
            assertThat(sql).contains("UNIQUE INDEX uq_survivorship_field");
        }
    }
}
