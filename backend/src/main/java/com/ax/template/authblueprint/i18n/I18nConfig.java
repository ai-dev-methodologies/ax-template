package com.ax.template.authblueprint.i18n;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * i18n-policy-l0 canonical Spring wiring (specs/i18n-policy-l0.yaml).
 *
 * <p>Cross-cutting, BACKEND-ONLY policy domain — no entity, no state machine, no UI.
 * Mirrors the {@code multi-tenant} shape: it ships Spring i18n CONFIG (a
 * {@link LocaleResolver} + a {@link MessageSource}) that a fork-receiver composes when
 * their recipe declares {@code supported_locales} with more than one entry.
 *
 * <h2>Additive-only contract</h2>
 * Both beans are NET-NEW and do not shadow an existing bean:
 * <ul>
 *   <li>No other backend domain defines a {@code localeResolver} or {@code messageSource}
 *       bean, and {@code application.yml} sets no {@code spring.messages.basename}, so
 *       Spring Boot's {@code MessageSourceAutoConfiguration} contributes nothing to
 *       override. These beans are the first and only declarations.</li>
 *   <li>The fallback Locale is {@link Locale#ENGLISH} (en) — the de-facto default the
 *       reference workloads already ship ({@code Accept-Language: en}). The global
 *       default is therefore UNCHANGED relative to current behavior; existing domains'
 *       responses are unaffected.</li>
 * </ul>
 *
 * <p>I18N-LOCALE-NEG-001: {@link AcceptHeaderLocaleResolver} honors the RFC 7231 §5.3.5
 * quality-value chain and falls back to an EXPLICIT non-null default Locale (never
 * {@code System.getDefault()}, which varies per host JVM and produces non-deterministic
 * responses across replicas).
 *
 * <p>I18N-MESSAGE-SOURCE-001: {@code setUseCodeAsDefaultMessage(false)} so a missing
 * bundle key surfaces a {@code NoSuchMessageException} (detectable) rather than silently
 * echoing the message code.
 */
@Configuration
public class I18nConfig {

    /** Reference baseline supported locales (BCP 47). Fork-receivers extend this list. */
    static final List<Locale> SUPPORTED_LOCALES =
            List.of(Locale.forLanguageTag("ko-KR"), Locale.forLanguageTag("en-US"));

    /** Explicit fallback when negotiation finds no match (I18N-LOCALE-NEG-001). */
    static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    /**
     * I18N-LOCALE-NEG-001 — resolve a request Locale from the {@code Accept-Language}
     * header (RFC 7231 §5.3.5 q-value chain) with an explicit, non-null fallback.
     */
    @Bean
    LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(SUPPORTED_LOCALES);
        resolver.setDefaultLocale(DEFAULT_LOCALE);
        return resolver;
    }

    /**
     * I18N-MESSAGE-SOURCE-001 — user-facing strings come from a resource bundle, never
     * inline literals. {@code useCodeAsDefaultMessage=false} makes a missing key throw
     * (detectable) instead of silently echoing the code.
     */
    @Bean
    MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:i18n/messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(false);
        // Fall back to the en bundle (messages.properties) when a requested locale's
        // bundle is missing a key — deterministic, never System.getDefault().
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
