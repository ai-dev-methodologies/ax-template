package com.ax.template.authblueprint.commercepromotion;

import com.ax.template.authblueprint.common.AggregateMember;
import com.ax.template.authblueprint.common.AggregateRoot;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for commerce-promotion-l0. Structural assertions that a deliberate break
 * cannot pass silently — no Spring context, pure reflection and file reads.
 *
 * Covers:
 * 1. OfferRedemption is fully append-only (PROMO-MAXUSES-001 / PROMO-IDEMPOTENT-001)
 * 2. UNIQUE(offer_id, order_ref) is declared on entity AND migration (PROMO-MAXUSES-001)
 * 3. @Version on Offer (PROMO-MAXUSES-001 pessimistic lock support)
 * 4. DDD tags: Offer = @AggregateRoot, OfferCode/OfferRedemption = @AggregateMember(root=PromoOffer.class)
 * 5. No member repositories (AX-DDD-MEMBER-REPO)
 * 6. PromotionService uses Math.multiplyExact (no double/float in discount arithmetic)
 * 7. Migration carries the UNIQUE constraint backstop
 */
@Tag("PROMOTION")
class CommercePromotionViolationProofTest {

    // ── 1. OfferRedemption is fully append-only ───────────────────────────────────

    @Test @Tag("PROMO-MAXUSES-001") @Tag("PROMO-REDEMPTION-IMMUTABLE-001")
    void violation_offerRedemption_fullyAppendOnly_noPublicSetter() throws Exception {
        for (Method m : PromoOfferRedemption.class.getMethods()) {
            assertThat(m.getName())
                .as("OfferRedemption must have no public setter — it is append-only")
                .doesNotStartWith("set");
        }
        for (Field f : PromoOfferRedemption.class.getDeclaredFields()) {
            Column col = f.getAnnotation(Column.class);
            if (col != null) {
                assertThat(col.updatable())
                    .as("OfferRedemption." + f.getName() + " must be updatable=false (append-only)")
                    .isFalse();
            }
        }
    }

    // ── 2. UNIQUE(offer_id, order_ref) declared on entity ────────────────────────

    @Test @Tag("PROMO-MAXUSES-001")
    void violation_offerRedemption_uniqueConstraint_offerIdOrderRef_onEntity() {
        Table table = PromoOfferRedemption.class.getAnnotation(Table.class);
        assertThat(table).as("OfferRedemption must carry @Table").isNotNull();
        UniqueConstraint[] constraints = table.uniqueConstraints();
        assertThat(constraints).as("OfferRedemption must have at least one unique constraint").isNotEmpty();

        boolean found = Arrays.stream(constraints).anyMatch(uc ->
            Arrays.asList(uc.columnNames()).contains("offer_id")
            && Arrays.asList(uc.columnNames()).contains("order_ref"));
        assertThat(found)
            .as("UNIQUE(offer_id, order_ref) must be declared on OfferRedemption entity — atomic backstop for PROMO-MAXUSES-001")
            .isTrue();
    }

    // ── 3. @Version on Offer ──────────────────────────────────────────────────────

    @Test @Tag("PROMO-MAXUSES-001")
    void violation_offer_hasVersionForPessimisticLock() throws Exception {
        Field versionField = PromoOffer.class.getDeclaredField("version");
        assertThat(versionField.isAnnotationPresent(Version.class))
            .as("Offer.version must carry @Version (required for PESSIMISTIC_WRITE lock support)")
            .isTrue();
    }

    // ── 4. DDD tags ───────────────────────────────────────────────────────────────

    @Test @Tag("PROMO-MAXUSES-001")
    void violation_dddTags_offerIsAggregateRoot_membersTaggedCorrectly() {
        assertThat(PromoOffer.class.isAnnotationPresent(AggregateRoot.class))
            .as("Offer must be @AggregateRoot").isTrue();

        AggregateMember codeTag = PromoOfferCode.class.getAnnotation(AggregateMember.class);
        assertThat(codeTag).as("OfferCode must carry @AggregateMember").isNotNull();
        assertThat(codeTag.root()).as("OfferCode.root must be PromoOffer.class").isEqualTo(PromoOffer.class);

        AggregateMember redemptionTag = PromoOfferRedemption.class.getAnnotation(AggregateMember.class);
        assertThat(redemptionTag).as("OfferRedemption must carry @AggregateMember").isNotNull();
        assertThat(redemptionTag.root()).as("OfferRedemption.root must be PromoOffer.class").isEqualTo(PromoOffer.class);
    }

    // ── 5. No member repositories ────────────────────────────────────────────────

    @Test @Tag("PROMO-MAXUSES-001")
    void violation_noMemberRepositories_onlyOfferRepository() {
        // OfferCode and OfferRedemption must NOT have their own repository interfaces.
        // If someone creates OfferCodeRepository or OfferRedemptionRepository the guard fails.
        try {
            Class.forName("com.ax.template.authblueprint.commercepromotion.OfferCodeRepository");
            assertThat(false).as("OfferCodeRepository must NOT exist (AX-DDD-MEMBER-REPO violation)").isTrue();
        } catch (ClassNotFoundException expected) {
            // correct — no such class
        }
        try {
            Class.forName("com.ax.template.authblueprint.commercepromotion.OfferRedemptionRepository");
            assertThat(false).as("OfferRedemptionRepository must NOT exist (AX-DDD-MEMBER-REPO violation)").isTrue();
        } catch (ClassNotFoundException expected) {
            // correct — no such class
        }
    }

    // ── 6. PromotionService uses Math.multiplyExact — no double/float ─────────────

    @Test @Tag("PROMO-CONSERVE-001")
    void violation_promotionService_discountMath_usesMultiplyExact_noDoubleOrFloat() throws Exception {
        String src = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "commercepromotion", "PromotionService.java"));

        assertThat(src)
            .as("PromotionService must use Math.multiplyExact for percent calculation (overflow fail-closed)")
            .contains("Math.multiplyExact");

        // The service must not use double or float for monetary calculation
        // (comments and import lines are acceptable, but casting/arithmetic is not)
        // Strip single-line comments and check for double/float in arithmetic context
        String strippedComments = src.replaceAll("//[^\n]*", "").replaceAll("/\\*.*?\\*/", "");
        boolean hasDoubleArithmetic = strippedComments.contains("(double)")
            || strippedComments.contains("(float)")
            || strippedComments.matches(".*\\bdouble\\s+\\w+\\s*=.*") // double d =
            || strippedComments.matches("(?s).*\\bfloat\\s+\\w+\\s*=.*"); // float f =
        assertThat(hasDoubleArithmetic)
            .as("PromotionService must not cast to double or float for monetary arithmetic")
            .isFalse();
    }

    // ── 7. Migration carries UNIQUE backstop ────────────────────────────────────

    @Test @Tag("PROMO-MAXUSES-001")
    void violation_migrationCarriesUniqueBackstop() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V071__create_commercepromotion.sql")) {
            assertThat(in).as("V071__create_commercepromotion.sql must exist on classpath").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql)
                .as("Migration must contain UNIQUE constraint on (offer_id, order_ref)")
                .containsIgnoringCase("UNIQUE");
            assertThat(sql).containsIgnoringCase("offer_id");
            assertThat(sql).containsIgnoringCase("order_ref");
            assertThat(sql).containsIgnoringCase("promo_redemptions");
        }
    }

    // ── 8. @Check on Offer carries the value guards ──────────────────────────────

    @Test @Tag("PROMO-CLAMP-001")
    void violation_offerEntityHasCheckConstraint_discountValueAndPriority() {
        Check check = PromoOffer.class.getAnnotation(Check.class);
        assertThat(check).as("Offer must carry @Check").isNotNull();
        String c = check.constraints();
        assertThat(c).contains("discount_value >= 0");
        assertThat(c).contains("priority >= 0");
        assertThat(c).contains("max_uses >= 0");
    }
}
