package com.ax.template.authblueprint.i18n;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reference endpoint set for i18n-policy-l0 compliance verification
 * (specs/i18n-policy-l0.yaml). Production projects do not need to ship this; only the
 * {@link I18nConfig} beans + the message bundles are required. The controller exercises
 * the policy BLACK-BOX so {@code I18nPolicyComplianceTest} can assert it over real HTTP.
 *
 * <p>The active request Locale is resolved by the {@code LocaleResolver} bean
 * ({@link I18nConfig}) from the {@code Accept-Language} header and exposed via
 * {@link LocaleContextHolder} — no endpoint reads the header by hand.
 */
@RestController
@RequestMapping("/api/i18n")
public class I18nProbeController {

    private final MessageSource messageSource;

    public I18nProbeController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * I18N-MESSAGE-SOURCE-001 — return a user-facing string sourced from the resource
     * bundle for the negotiated Locale (never an inline literal).
     */
    @GetMapping("/greeting")
    public Map<String, String> greeting() {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("locale", locale.toLanguageTag());
        body.put("greeting", messageSource.getMessage("greeting", null, locale));
        return body;
    }

    /**
     * I18N-MESSAGE-SOURCE-002 — resolve a plural-bearing key via ICU MessageFormat
     * (Spring delegates to {@code java.text.MessageFormat} with a {@code choice}/ICU
     * pattern). Korean resolves 1 grammatical form; English resolves 2 (singular vs
     * plural) for the same key.
     */
    @GetMapping("/plural")
    public Map<String, Object> plural(@RequestParam int count) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", locale.toLanguageTag());
        body.put("count", count);
        body.put("message",
                messageSource.getMessage("items.count", new Object[] {count}, locale));
        return body;
    }

    /**
     * I18N-FORMATTING-001 — locale-aware currency / number formatting via
     * {@link NumberFormat#getCurrencyInstance(Locale)} (never a hard-coded format
     * string). ko-KR renders {@code ₩} with 0 fraction digits; en-US renders {@code $}
     * prefix. Grouping separators follow the locale's CLDR data.
     */
    @GetMapping("/format")
    public Map<String, Object> format(@RequestParam long amount,
                                      @RequestParam(defaultValue = "currency") String type) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", locale.toLanguageTag());
        body.put("type", type);
        NumberFormat fmt = "number".equals(type)
                ? NumberFormat.getNumberInstance(locale)
                : NumberFormat.getCurrencyInstance(locale);
        body.put("formatted", fmt.format(amount));
        return body;
    }

    /**
     * I18N-TIMEZONE-001 — store-as-UTC-Instant, display-at-boundary round-trip. Accepts
     * an offset-bearing ISO 8601 {@code at} and a {@code zone} (IANA tz id). Returns the
     * canonical UTC Instant (Jackson Z-form) PLUS the wall-clock rendering in the
     * requested zone. A naive (offset-less) {@code at} is rejected 400 INVALID_DATETIME
     * by {@link I18nTimePolicy#parseStrictOffset(String)}.
     */
    @GetMapping("/time")
    public Map<String, Object> time(@RequestParam String at,
                                    @RequestParam(defaultValue = "UTC") String zone) {
        Instant instant = I18nTimePolicy.parseStrictOffset(at);
        ZoneId zoneId = ZoneId.of(zone);
        ZonedDateTime display = I18nTimePolicy.displayIn(instant, zoneId);

        Map<String, Object> body = new LinkedHashMap<>();
        // Stored canonical form — UTC Instant, ISO 8601 Z-form (I18N-TIMEZONE-001 F9).
        body.put("instant", instant.toString());
        body.put("zone", zoneId.getId());
        body.put("offsetSeconds", display.getOffset().getTotalSeconds());
        body.put("wallClock", display.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        return body;
    }
}
