package com.ax.template.authblueprint.commercepricing;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for commercepricing — no Spring context; pure computation + file reads.
 *
 * Covers:
 * 1. PricingPipeline.priceOrder is deterministic (same inputs → identical PricedOrder)
 * 2. No {@code double} or {@code float} in PricingPipeline source (integer-only arithmetic)
 * 3. Tax on NET (post-discount) base is strictly less than tax on gross — ordering is observable
 */
@Tag("COMMERCEPRICING")
class CommercePricingViolationProofTest {

    private final PricingPipeline pipeline = new PricingPipeline();

    // ── 1. Determinism: same inputs → identical PricedOrder across 2 calls ────────

    @Test @Tag("PRICING-ORDER-001")
    void violation_pipeline_isDeterministic_sameInputsProduceSameOutput() {
        List<PricingPipeline.Line> lines = List.of(
            new PricingPipeline.Line("SKU-A", 333L),
            new PricingPipeline.Line("SKU-B", 333L),
            new PricingPipeline.Line("SKU-C", 334L)
        );

        PricingPipeline.PricedOrder first  = pipeline.priceOrder(lines, 100L, 50L, 1000);
        PricingPipeline.PricedOrder second = pipeline.priceOrder(lines, 100L, 50L, 1000);

        assertThat(first.subTotal()).isEqualTo(second.subTotal());
        assertThat(first.orderDiscount()).isEqualTo(second.orderDiscount());
        assertThat(first.shipping()).isEqualTo(second.shipping());
        assertThat(first.totalTax()).isEqualTo(second.totalTax());
        assertThat(first.total()).isEqualTo(second.total());
        assertThat(first.lines()).hasSize(second.lines().size());

        for (int i = 0; i < first.lines().size(); i++) {
            PricingPipeline.PricedLine l1 = first.lines().get(i);
            PricingPipeline.PricedLine l2 = second.lines().get(i);
            assertThat(l1.gross()).as("line[%d].gross must be identical", i).isEqualTo(l2.gross());
            assertThat(l1.proratedDiscount()).as("line[%d].proratedDiscount must be identical", i).isEqualTo(l2.proratedDiscount());
            assertThat(l1.taxableBase()).as("line[%d].taxableBase must be identical", i).isEqualTo(l2.taxableBase());
            assertThat(l1.tax()).as("line[%d].tax must be identical", i).isEqualTo(l2.tax());
        }
    }

    // ── 2. No double/float in PricingPipeline source ─────────────────────────────

    @Test @Tag("PRICING-ORDER-001")
    void violation_pricingPipeline_noDoubleOrFloat_integerArithmeticOnly() throws Exception {
        // Find PricingPipeline.java relative to the working dir (backend/)
        Path srcPath = Paths.get(System.getProperty("user.dir"),
            "src", "main", "java",
            "com", "ax", "template", "authblueprint", "commercepricing",
            "PricingPipeline.java");

        assertThat(srcPath.toFile().exists())
            .as("PricingPipeline.java must exist at: " + srcPath)
            .isTrue();

        String src = Files.readString(srcPath);

        // Strip single-line comments, then check for double/float in arithmetic context
        String stripped = src.replaceAll("//[^\n]*", "").replaceAll("/\\*.*?\\*/", "");

        // Check: no " double " variable declarations or casts in arithmetic
        assertThat(stripped)
            .as("PricingPipeline must not contain \" double \" (double literal/var) — integer arithmetic only")
            .doesNotContain(" double ");

        // Check: no " float " variable declarations or casts in arithmetic
        assertThat(stripped)
            .as("PricingPipeline must not contain \" float \" (float literal/var) — integer arithmetic only")
            .doesNotContain(" float ");

        // Check: Math.multiplyExact is used (fail-closed overflow guard)
        assertThat(src)
            .as("PricingPipeline must use Math.multiplyExact for percent calculation (overflow fail-closed)")
            .contains("Math.multiplyExact");
    }

    // ── 3. Tax-on-NET is strictly less than tax-on-gross (ordering keystone) ─────

    @Test @Tag("PRICING-ORDER-002")
    void violation_taxOnNet_strictlyLessThan_taxOnGross_orderingIsObservable() {
        // A discounted order: 1 line at 1000, discount 200, tax 10% (1000bp)
        List<PricingPipeline.Line> lines = List.of(
            new PricingPipeline.Line("SKU-A", 1000L)
        );

        PricingPipeline.PricedOrder result = pipeline.priceOrder(lines, 200L, 0L, 1000);

        // Pipeline uses NET base: taxableBase = 1000 - 200 = 800, tax = 80
        long pipelineTax = result.totalTax();

        // What gross-base tax would be (wrong phase order): tax = 10% of 1000 = 100
        long grossBaseTax = 1000L * 1000L / 10_000L; // = 100

        assertThat(pipelineTax)
            .as("PRICING-ORDER-002 keystone: pipeline NET tax (%d) must be STRICTLY LESS THAN gross-base tax (%d)",
                pipelineTax, grossBaseTax)
            .isLessThan(grossBaseTax);

        // Verify exact values for full traceability
        assertThat(pipelineTax)
            .as("NET tax must be exactly 80 (10%% of taxableBase 800)")
            .isEqualTo(80L);
        assertThat(grossBaseTax)
            .as("gross-base tax must be 100 (10%% of gross 1000)")
            .isEqualTo(100L);

        // Also verify the per-line taxableBase
        assertThat(result.lines().get(0).taxableBase())
            .as("taxableBase must be 800 (1000 gross - 200 prorated discount)")
            .isEqualTo(800L);

        // And total uses net tax
        long expectedTotal = 1000L - 200L + 0L + 80L; // = 880
        long wrongTotal = 1000L - 200L + 0L + 100L;   // = 900 if gross tax used
        assertThat(result.total())
            .as("total must use net tax → 880, not gross-tax total 900")
            .isEqualTo(expectedTotal)
            .isNotEqualTo(wrongTotal);
    }
}
