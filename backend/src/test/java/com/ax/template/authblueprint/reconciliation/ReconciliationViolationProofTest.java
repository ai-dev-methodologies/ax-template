package com.ax.template.authblueprint.reconciliation;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for external-reconciliation-l0. Structural assertions a deliberate break cannot
 * pass silently: the classified items are append-only one-per-(run, key) with their basis
 * immutable, the disposition is gated by the @Check backstops (disposed ⇒ BREAK + every
 * disposition field present), the run + item carry @Version, NO delete path exists anywhere in
 * the domain, mutators are package-sealed, the dispose/resolve paths use the PESSIMISTIC_WRITE
 * finders, the classifier is deterministic, and the migration carries the same backstops.
 */
@Tag("RECONCILIATION")
class ReconciliationViolationProofTest {

    // ── RECON-CLASSIFY-001 — items append-only, one per (run, key), basis immutable ──
    @Test @Tag("RECON-CLASSIFY-001")
    void violation_itemsAppendOnly_uniquePerRunKey_basisImmutable() throws Exception {
        for (Method m : ReconciliationItem.class.getMethods()) {
            assertThat(m.getName()).as("ReconciliationItem must have no public setter").doesNotStartWith("set");
        }
        // identity + classification + basis columns are immutable
        for (String f : new String[]{"id", "runId", "itemKey", "classification",
                                     "internalAmount", "externalAmount", "delta", "createdAt"}) {
            Column col = ReconciliationItem.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ReconciliationItem." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = ReconciliationItem.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames()).containsExactly("run_id", "item_key");
        // the run carries the idempotency identity uq(source_key, feed_snapshot_hash)
        jakarta.persistence.Table runTable = ReconciliationRun.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(runTable.uniqueConstraints()[0].columnNames()).containsExactly("source_key", "feed_snapshot_hash");
    }

    // ── RECON-CLASSIFY-001 — the classifier is deterministic from key membership + amount ──
    @Test @Tag("RECON-CLASSIFY-001")
    void violation_classifierDeterministic() {
        assertThat(ItemClassification.of(new BigDecimal("100"), new BigDecimal("100"))).isEqualTo(ItemClassification.MATCHED);
        assertThat(ItemClassification.of(new BigDecimal("100"), new BigDecimal("90"))).isEqualTo(ItemClassification.BREAK);
        // BigDecimal.compareTo ignores scale — 100 and 100.00 are MATCHED, not a BREAK
        assertThat(ItemClassification.of(new BigDecimal("100"), new BigDecimal("100.00"))).isEqualTo(ItemClassification.MATCHED);
        assertThat(ItemClassification.of(new BigDecimal("100"), null)).isEqualTo(ItemClassification.INTERNAL_ONLY);
        assertThat(ItemClassification.of(null, new BigDecimal("100"))).isEqualTo(ItemClassification.EXTERNAL_ONLY);
    }

    // ── RECON-DISPOSE/RESOLVE-001 — @Check backstops; mutators sealed; @Version; NO delete path ──
    @Test @Tag("RECON-DISPOSE-001") @Tag("RECON-RESOLVE-001")
    void violation_checkBackstops_mutatorsSealed_noDeletePath() throws Exception {
        for (Method m : ReconciliationRunRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).doesNotContain("delete");
        }
        for (String src : new String[]{"ReconciliationService", "ReconciliationController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "reconciliation", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — runs are append-only, never removed")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
        Check check = ReconciliationItem.class.getAnnotation(Check.class);
        String c = check.constraints().replaceAll("\\s+", " ");
        assertThat(c).contains("disposed = FALSE OR classification = 'BREAK'");
        assertThat(c).contains("disposition_type IS NOT NULL");
        assertThat(c).contains("disposed_by IS NOT NULL");
        assertThat(c).contains("disposed_at IS NOT NULL");
        assertThat(c).contains("disposition_reason IS NOT NULL");

        for (String hook : new String[]{"dispose"}) {
            Method m = java.util.Arrays.stream(ReconciliationItem.class.getDeclaredMethods())
                .filter(x -> x.getName().equals(hook)).findFirst().orElseThrow();
            assertThat(Modifier.isPublic(m.getModifiers()))
                .as("ReconciliationItem." + hook + " must be package-private").isFalse();
        }
        Method resolveHook = java.util.Arrays.stream(ReconciliationRun.class.getDeclaredMethods())
            .filter(x -> x.getName().equals("resolve")).findFirst().orElseThrow();
        assertThat(Modifier.isPublic(resolveHook.getModifiers()))
            .as("ReconciliationRun.resolve must be package-private").isFalse();

        // @Version on both the run and the item
        assertThat(ReconciliationRun.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        assertThat(ReconciliationItem.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
        // immutable identity columns on the run
        for (String f : new String[]{"id", "sourceKey", "feedSnapshotHash", "createdAt"}) {
            Column col = ReconciliationRun.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col.updatable()).as("ReconciliationRun." + f + " must be immutable").isFalse();
        }
    }

    // ── RECON-CONCURRENT-001 — the dispose path uses the PESSIMISTIC_WRITE item finder ──
    @Test @Tag("RECON-CONCURRENT-001")
    void violation_lockedFinder_andSerializedDispose() throws Exception {
        Method locked = ReconciliationRunRepository.class.getMethod("findItemByIdForUpdate", java.util.UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "reconciliation", "ReconciliationService.java"));
        int start = svc.indexOf("public ReconciliationItem dispose(");
        assertThat(start).as("dispose must exist").isPositive();
        String body = svc.substring(start, svc.indexOf("\n    }", start));
        assertThat(body).as("dispose must take the item row lock").contains("findItemByIdForUpdate");
        assertThat(body).as("the dispose precondition gates on the disposed-once state")
            .contains("item.isDisposed()");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("RECON-CLASSIFY-001") @Tag("RECON-IDEMPOTENT-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V053__create_reconciliation.sql")) {
            assertThat(in).as("V053__create_reconciliation.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("disposed = FALSE OR classification = 'BREAK'");
            assertThat(sql).contains("UNIQUE INDEX uq_recon_source_feed");
            assertThat(sql).contains("(source_key, feed_snapshot_hash)");
            assertThat(sql).contains("UNIQUE INDEX uq_recon_run_item");
            assertThat(sql).contains("(run_id, item_key)");
        }
    }

    // ── P1-65 REVERT (audit-seal-11) — the run insert is NON-TERMINAL (items follow), so it must NOT
    //    be isolated in a REQUIRES_NEW inner tx (that would durably commit a run whose items can then
    //    be lost → a permanent orphan a later re-run short-circuits onto). Run + items are ONE atomic
    //    unit in ReconciliationRunCreator.doRun, and run() catches the uq(source,feed) race OUTSIDE
    //    that tx so the loser rolls back atomically (no orphan) and requeries the winner in a fresh tx. ──
    @Test @Tag("RECON-IDEMPOTENT-001")
    void violation_runAndItemsAtomic_catchOutsideReplay_noRequiresNewIsolation() throws Exception {
        // the collaborator commits run + items atomically in ONE (REQUIRED, not REQUIRES_NEW) tx
        Method doRun = ReconciliationRunCreator.class.getDeclaredMethod("doRun",
            String.class, String.class, java.util.Map.class, java.util.Map.class);
        org.springframework.transaction.annotation.Transactional tx =
            doRun.getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx).as("doRun must be @Transactional (run + items atomic)").isNotNull();
        assertThat(tx.propagation())
            .as("doRun must be REQUIRED — run + items share ONE tx, never an isolated run insert")
            .isEqualTo(org.springframework.transaction.annotation.Propagation.REQUIRED);

        String creatorSrc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "reconciliation", "ReconciliationRunCreator.java"));
        int drAt = creatorSrc.indexOf("ReconciliationRun doRun(");
        assertThat(drAt).as("doRun must exist").isPositive();
        String drBody = creatorSrc.substring(drAt, creatorSrc.indexOf("\n    }", drAt));
        int runInsertAt = drBody.indexOf("saveAndFlush");
        int itemInsertAt = drBody.indexOf("members.persist");
        assertThat(runInsertAt).as("doRun inserts the run").isNotNegative();
        assertThat(itemInsertAt).as("doRun persists the items in the SAME tx AFTER the run")
            .isGreaterThan(runInsertAt);

        // run() is NON-transactional and catches the DIVE OUTSIDE doRun's tx, then replays the winner
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "reconciliation", "ReconciliationService.java"));
        assertThat(svc).as("run() must NOT be @Transactional — the catch must run OUTSIDE doRun's tx")
            .doesNotContain("@Transactional\n    public ReconciliationRun run(");
        int runAt = svc.indexOf("public ReconciliationRun run(");
        assertThat(runAt).as("run() must exist").isPositive();
        String runBody = svc.substring(runAt, svc.indexOf("\n    }", runAt));
        assertThat(runBody).as("run() delegates the atomic insert to the creator").contains("creator.doRun");
        assertThat(runBody).as("run() catches the race OUTSIDE the creator's tx and replays the winner")
            .contains("catch (DataIntegrityViolationException").contains("creator.replay");

        // the service no longer isolates a run insert through IdempotentInsert (atomic doRun replaces it)
        assertThat(java.util.Arrays.stream(ReconciliationService.class.getDeclaredFields())
                .noneMatch(f -> f.getType() == com.ax.template.authblueprint.common.IdempotentInsert.class))
            .as("ReconciliationService must NOT hold an IdempotentInsert — atomic doRun replaces the isolated insert")
            .isTrue();
    }
}
