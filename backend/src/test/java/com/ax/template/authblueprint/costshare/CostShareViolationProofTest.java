package com.ax.template.authblueprint.costshare;

import com.ax.template.authblueprint.common.WaterfallAllocator;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for accumulator-consume-l0 + ordered-waterfall-l0. Structural + pure-logic
 * invariants that would re-open a money-leak if relaxed. Reflection / pure arithmetic / classpath
 * read — no Spring context.
 */
@Tag("COSTSHARE")
class CostShareViolationProofTest {

    private static final int SCALE = 2;

    private static List<WaterfallAllocator.Tier> cascade(BigDecimal dedCap, BigDecimal rate, BigDecimal oopCap) {
        return List.of(
            new WaterfallAllocator.Tier("ded", WaterfallAllocator.TierKind.ABSORB_TO_CAP, dedCap),
            new WaterfallAllocator.Tier(null, WaterfallAllocator.TierKind.COINSURANCE, rate),
            new WaterfallAllocator.Tier("oop", WaterfallAllocator.TierKind.CLAMP_TOTAL, oopCap));
    }

    // ── ACC-FLIP-001 — a straddling allocation conserves exactly ─────────────────
    @Test @Tag("ACC-FLIP-001")
    void violation_straddlingAllocationConserves() {
        // eligible 150 straddles the 100 deductible watermark
        WaterfallAllocator.Result r = WaterfallAllocator.allocate(
            new BigDecimal("150.00"), cascade(new BigDecimal("100.00"), new BigDecimal("0.20"), new BigDecimal("1000.00")), SCALE);
        assertThat(r.memberPaid().add(r.counterpartyPaid()))
            .as("member + counterparty == eligible (straddle)").isEqualByComparingTo("150.00");
        assertThat(r.memberPaid()).isEqualByComparingTo("110.00");   // 100 deductible + 0.2*50 coinsurance
    }

    // ── WF-CONSERVE-001 / WF-CLAMP-001 — conservation holds across values incl. the clamp ──
    @Test @Tag("WF-CONSERVE-001")
    void violation_conservesAcrossValuesIncludingClamp() {
        for (String e : new String[]{"0.00", "37.50", "100.00", "543.21", "1000.00"}) {
            WaterfallAllocator.Result r = WaterfallAllocator.allocate(
                new BigDecimal(e), cascade(new BigDecimal("100.00"), new BigDecimal("0.20"), new BigDecimal("1000.00")), SCALE);
            assertThat(r.memberPaid().add(r.counterpartyPaid()))
                .as("conserve for eligible=" + e).isEqualByComparingTo(e);
            assertThat(r.memberPaid()).as("member never negative").isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(r.counterpartyPaid()).as("counterparty never negative").isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
        // clamp: uncapped member 280 -> OOP cap 150 -> member 150, conservation still exact
        WaterfallAllocator.Result clamped = WaterfallAllocator.allocate(
            new BigDecimal("1000.00"), cascade(new BigDecimal("100.00"), new BigDecimal("0.20"), new BigDecimal("150.00")), SCALE);
        assertThat(clamped.memberPaid()).isEqualByComparingTo("150.00");
        assertThat(clamped.memberPaid().add(clamped.counterpartyPaid())).isEqualByComparingTo("1000.00");
    }

    // ── no public mutator; @Version; immutable columns ──────────────────────────
    @Test
    void violation_noPublicMutators_versionPresent_immutableColumns() throws Exception {
        for (String banned : new String[]{"setUsed", "advanceUsed", "decrementUsed", "resetUsed", "setLimit", "setScopeKey"}) {
            for (Method m : Accumulator.class.getMethods()) {   // public only
                assertThat(m.getName())
                    .as("Accumulator must have no PUBLIC mutator " + banned + " (service is the sole mutator under a row lock)")
                    .isNotEqualTo(banned);
            }
        }
        Field v = Accumulator.class.getDeclaredField("version");
        assertThat(v.isAnnotationPresent(Version.class)).as("version must carry @Version").isTrue();
        for (String f : new String[]{"id", "scopeKey", "createdAt"}) {
            Column col = Accumulator.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as(f + " must be immutable (@Column updatable=false)").isFalse();
        }
    }

    // ── migration declares the solvency CHECK backstops (fork-receiver Flyway schema) ──
    @Test @Tag("ACC-CLAWBACK-001")
    void violation_migrationDeclaresCheckConstraints() throws Exception {
        String sql;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V035__create_cost_share_accumulators.sql")) {
            assertThat(in).as("V035 migration must be on the classpath").isNotNull();
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertThat(sql).contains("chk_cost_share_used_nonneg");
        assertThat(sql).contains("used >= 0");
        assertThat(sql).contains("chk_cost_share_used_within_limit");
        assertThat(sql).contains("used <= limit_amount");
        assertThat(sql).contains("uq_cost_share_scope_key");
    }
}
