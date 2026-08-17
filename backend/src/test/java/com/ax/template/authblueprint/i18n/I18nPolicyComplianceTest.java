package com.ax.template.authblueprint.i18n;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Black-box compliance verification for i18n-policy-l0 (specs/i18n-policy-l0.yaml).
 *
 * <p>Covers the 5 TESTABLE items (I18N-DEFAULT-DECL-001 is verification_type: review and
 * is documented in the L4 README, not mechanically tested per the spec). RestAssured
 * over a random port — portable, no MockMvc, no @WithMockUser.
 */
// R22 aggregate-test isolation: this is a new RANDOM_PORT @SpringBootTest sharing the
// default-properties context-cache key. Under the aggregate `./gradlew test` run it pushed
// the Spring TestContext ContextCache past its capacity-32 LRU, evicting a sibling
// default-properties context whose Hikari pool was then shut down while
// SessionRevocationCheckTest still held it (UndeclaredThrowableException at
// prepareStatement). BEFORE_CLASS forces this class to boot a fresh context and not park
// a perturbing entry in the LRU — same surgical closure as BillingFlowIT /
// FeatureFlagFlowIT / ApiKeyComplianceTest / EcommerceE2ETest.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class I18nPolicyComplianceTest {

    @LocalServerPort int port;

    @Autowired MessageSource messageSource;

    // ── I18N-LOCALE-NEG-001 ──────────────────────────────────────────────────
    @Test
    @Tag("I18N")
    @Tag("I18N-LOCALE-NEG-001")
    void i18n_LOCALE_NEG_001_acceptLanguageNegotiationWithFallback() {
        // RFC 7231 §5.3.5 q-value chain: ko-KR wins over en;q=0.8.
        Response ko = given().header("Accept-Language", "ko-KR,en;q=0.8")
                .when().get("/api/i18n/greeting");
        assertThat(ko.statusCode()).isEqualTo(200);
        assertThat(ko.jsonPath().getString("locale")).isEqualTo("ko-KR");
        assertThat(ko.jsonPath().getString("greeting")).isEqualTo("안녕하세요");

        // No header → explicit declared fallback (en, NOT System.getDefault).
        Response fallback = given().when().get("/api/i18n/greeting");
        assertThat(fallback.statusCode()).isEqualTo(200);
        assertThat(fallback.jsonPath().getString("locale")).startsWith("en");
        assertThat(fallback.jsonPath().getString("greeting")).isEqualTo("Hello");
    }

    // ── I18N-MESSAGE-SOURCE-001 ──────────────────────────────────────────────
    @Test
    @Tag("I18N")
    @Tag("I18N-MESSAGE-SOURCE-001")
    void i18n_MESSAGE_SOURCE_001_userFacingStringsFromBundle() {
        // The string returned over HTTP is sourced from the bundle, per locale.
        Response en = given().header("Accept-Language", "en-US")
                .when().get("/api/i18n/greeting");
        assertThat(en.statusCode()).isEqualTo(200);
        assertThat(en.jsonPath().getString("greeting")).isEqualTo("Hello");

        // Key-set PARITY across every supported_locales bundle — a missing key in any
        // locale must fail (build-time parity check, here at test time).
        Set<Object> base = loadKeys("/i18n/messages.properties");
        Set<Object> ko = loadKeys("/i18n/messages_ko.properties");
        Set<Object> enKeys = loadKeys("/i18n/messages_en.properties");
        assertThat(ko)
                .as("ko-KR bundle must cover the same key set as the default bundle")
                .isEqualTo(base);
        assertThat(enKeys)
                .as("en-US bundle must cover the same key set as the default bundle")
                .isEqualTo(base);
    }

    // ── I18N-MESSAGE-SOURCE-002 ──────────────────────────────────────────────
    @Test
    @Tag("I18N")
    @Tag("I18N-MESSAGE-SOURCE-002")
    void i18n_MESSAGE_SOURCE_002_pluralAndGenderUseIcuMessageFormat() {
        // English: 2 grammatical number forms resolved per count via MessageFormat.
        assertThat(plural("en-US", 1)).isEqualTo("one item");
        assertThat(plural("en-US", 5)).isEqualTo("5 items");
        assertThat(plural("en-US", 0)).isEqualTo("no items");

        // Korean: 1 grammatical number form — same key, single form for any count.
        assertThat(plural("ko-KR", 1)).isEqualTo("1개");
        assertThat(plural("ko-KR", 5)).isEqualTo("5개");
    }

    // ── I18N-TIMEZONE-001 ────────────────────────────────────────────────────
    @Test
    @Tag("I18N")
    @Tag("I18N-TIMEZONE-001")
    void i18n_TIMEZONE_001_persistAsInstantConvertAtBoundary() {
        // Same UTC Instant read from two zones: identical Instant, differing wall-clock.
        String utcAt = "2026-05-27T03:42:18Z";
        Response seoul = given()
                .queryParam("at", utcAt).queryParam("zone", "Asia/Seoul")
                .when().get("/api/i18n/time");
        Response ny = given()
                .queryParam("at", utcAt).queryParam("zone", "America/New_York")
                .when().get("/api/i18n/time");

        assertThat(seoul.statusCode()).isEqualTo(200);
        assertThat(ny.statusCode()).isEqualTo(200);
        // Stored Instant identical (Z-form, I18N-TIMEZONE-001 F9).
        assertThat(seoul.jsonPath().getString("instant")).isEqualTo("2026-05-27T03:42:18Z");
        assertThat(ny.jsonPath().getString("instant")).isEqualTo("2026-05-27T03:42:18Z");
        // Offsets differ: Seoul +09:00 (32400s) vs New York -04:00 EDT (-14400s) = 13h gap.
        int seoulOffset = seoul.jsonPath().getInt("offsetSeconds");
        int nyOffset = ny.jsonPath().getInt("offsetSeconds");
        assertThat(seoulOffset).isEqualTo(32400);
        assertThat(nyOffset).isEqualTo(-14400);
        assertThat(seoulOffset - nyOffset).isEqualTo(13 * 3600);

        // Naive (offset-less) inbound date-time → 400 INVALID_DATETIME (RFC 3339 §5.6).
        Response naive = given()
                .queryParam("at", "2026-05-27T03:42:18").queryParam("zone", "UTC")
                .when().get("/api/i18n/time");
        assertThat(naive.statusCode()).isEqualTo(400);
        assertThat(naive.jsonPath().getString("code")).isEqualTo("INVALID_DATETIME");
    }

    // ── I18N-FORMATTING-001 ──────────────────────────────────────────────────
    @Test
    @Tag("I18N")
    @Tag("I18N-FORMATTING-001")
    void i18n_FORMATTING_001_currencyAndGroupingPerLocale() {
        // ko-KR currency: ₩ symbol, 0 fraction digits (KRW per ISO 4217).
        Response won = given().header("Accept-Language", "ko-KR")
                .queryParam("amount", 12345678).queryParam("type", "currency")
                .when().get("/api/i18n/format");
        assertThat(won.statusCode()).isEqualTo(200);
        assertThat(won.jsonPath().getString("formatted")).contains("₩").contains("12,345,678");

        // en-US currency: $ prefix, 3-digit grouping.
        Response usd = given().header("Accept-Language", "en-US")
                .queryParam("amount", 12345678).queryParam("type", "currency")
                .when().get("/api/i18n/format");
        assertThat(usd.statusCode()).isEqualTo(200);
        assertThat(usd.jsonPath().getString("formatted")).startsWith("$").contains("12,345,678");

        // Locale-aware number grouping (no currency symbol).
        Response number = given().header("Accept-Language", "en-US")
                .queryParam("amount", 12345678).queryParam("type", "number")
                .when().get("/api/i18n/format");
        assertThat(number.statusCode()).isEqualTo(200);
        assertThat(number.jsonPath().getString("formatted")).isEqualTo("12,345,678");
    }

    // ── I18N-MESSAGE-SOURCE-001 (strict: a missing key MUST fail, not echo the code) ──
    @Test
    @Tag("I18N")
    @Tag("I18N-MESSAGE-SOURCE-001")
    void i18n_MESSAGE_SOURCE_001_missingKeyThrowsNotSilentCode() {
        // Adversarial-review closure: key-parity alone did not prove the spec MUST
        // "missing keys MUST fail, not silently fall back to the message code".
        // useCodeAsDefaultMessage=false makes a missing key throw, never echo the code.
        assertThatThrownBy(() ->
                messageSource.getMessage("i18n.nonexistent.key", null, Locale.ENGLISH))
            .isInstanceOf(NoSuchMessageException.class);
        assertThatThrownBy(() ->
                messageSource.getMessage("i18n.nonexistent.key", null, Locale.forLanguageTag("ko-KR")))
            .isInstanceOf(NoSuchMessageException.class);
    }

    // ── I18N-TIMEZONE-001 (strict-offset enforced at the policy utility, not just HTTP) ──
    @Test
    @Tag("I18N")
    @Tag("I18N-TIMEZONE-001")
    void i18n_TIMEZONE_001_parseStrictOffsetRejectsNaiveAtTheUtility() {
        // Adversarial-review closure: the HTTP test proved the endpoint rejects naive
        // input, but the strict-offset contract lives in the reusable policy utility —
        // pin it directly so a service-layer caller can't bypass it with a lax parser.
        assertThatThrownBy(() -> I18nTimePolicy.parseStrictOffset("2026-05-27T03:42:18"))
            .isInstanceOf(I18nTimePolicy.InvalidDateTimeException.class);
        assertThatThrownBy(() -> I18nTimePolicy.parseStrictOffset(null))
            .isInstanceOf(I18nTimePolicy.InvalidDateTimeException.class);
        // offset-bearing forms (Z and ±HH:MM) parse — and the two equal forms agree.
        assertThatCode(() -> I18nTimePolicy.parseStrictOffset("2026-05-27T03:42:18Z"))
            .doesNotThrowAnyException();
        assertThat(I18nTimePolicy.parseStrictOffset("2026-05-27T12:42:18+09:00"))
            .isEqualTo(I18nTimePolicy.parseStrictOffset("2026-05-27T03:42:18Z"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────
    private String plural(String acceptLanguage, int count) {
        return given().header("Accept-Language", acceptLanguage)
                .queryParam("count", count)
                .when().get("/api/i18n/plural")
                .then().statusCode(200).extract().response()
                .jsonPath().getString("message");
    }

    private Set<Object> loadKeys(String classpathResource) {
        Properties props = new Properties();
        try (InputStream in = getClass().getResourceAsStream(classpathResource)) {
            assertThat(in).as("bundle %s must exist on classpath", classpathResource).isNotNull();
            // .properties are ISO-8859-1 on disk; key NAMES are ASCII so encoding is moot here.
            props.load(in);
        } catch (IOException e) {
            throw new AssertionError("failed to load " + classpathResource, e);
        }
        return props.keySet();
    }
}
