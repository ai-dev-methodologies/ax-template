package com.ax.template.authblueprint.saturatingbalance;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for saturating-balance-l0. Structural assertions a deliberate break cannot
 * pass silently: the Balance entity carries the range @Check backstop + @Version + no public
 * setters (mutation only through the package-private applyAccrual/applyDebit hooks), every
 * LedgerEntry column is immutable, and the migration carries the matching DB backstop.
 */
@Tag("SATURATINGBALANCE")
class SaturatingBalanceViolationProofTest {

    // ── Balance — no public setters, @Version, @Check range backstop ──
    @Test @Tag("SATBAL-CEILING-001")
    void violation_balanceNoPublicSetters_versioned_checkBackstop() throws Exception {
        for (Method m : Balance.class.getMethods()) {
            assertThat(m.getName()).as("Balance must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "ownerId", "cap", "createdAt"}) {
            Column col = Balance.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("Balance." + f + " must be immutable").isFalse();
        }
        // current_value IS mutable (it's the balance itself) but ONLY via the package-private hooks
        assertThat(Balance.class.getDeclaredField("current").getAnnotation(Column.class).updatable()).isTrue();
        assertThat(Balance.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();

        Check check = Balance.class.getAnnotation(Check.class);
        assertThat(check).as("Balance must carry a @Check range backstop").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .contains("current_value >= 0").contains("current_value <= cap");
    }

    // ── mutation only through the package-private sole-mutator hooks ──
    @Test @Tag("SATBAL-CONCURRENT-004")
    void violation_mutationOnlyThroughPackagePrivateHooks() throws Exception {
        for (String hook : new String[]{"applyAccrual", "applyDebit", "headroom"}) {
            var method = Balance.class.getDeclaredMethod(hook,
                hook.equals("headroom") ? new Class<?>[]{} : new Class<?>[]{BigDecimal.class});
            assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers()))
                .as(hook + " must NOT be public — only the service (same package) may call it").isFalse();
        }
    }

    // ── LedgerEntry — every column immutable ──
    @Test @Tag("SATBAL-LEDGER-003")
    void violation_ledgerEntryImmutable_noSetters() throws Exception {
        for (Method m : LedgerEntry.class.getMethods()) {
            assertThat(m.getName()).as("LedgerEntry must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "balanceId", "op", "requestedAmount", "appliedAmount", "occurredAt"}) {
            Column col = LedgerEntry.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("LedgerEntry." + f + " must be immutable").isFalse();
        }
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), LedgerOp.ACCRUE,
            BigDecimal.TEN, BigDecimal.TEN, Instant.now());
        assertThat(entry.getOp()).isEqualTo(LedgerOp.ACCRUE);
    }

    // ── the migration carries the same range backstop ──
    @Test @Tag("SATBAL-CEILING-001")
    void violation_migrationCarriesTheSameBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V103__create_saturating_balances.sql")) {
            assertThat(in).as("V103__create_saturating_balances.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("CREATE TABLE saturating_balances");
            assertThat(sql).contains("current_value >= 0 AND current_value <= cap");
            assertThat(sql).contains("saturating_ledger_entries");
        }
    }
}
