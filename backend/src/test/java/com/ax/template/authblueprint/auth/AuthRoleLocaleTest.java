package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D2 — locale-safe case folding. Under the Turkish locale, {@code "admin".toUpperCase()}
 * folds to {@code "ADMİN"} (dotted capital I), so {@code UserRole.valueOf(...)} throws and a
 * legitimate {@code admin} role override is rejected with a 400. The role fold now pins
 * {@link Locale#ROOT}, so it resolves regardless of the ambient default locale.
 */
class AuthRoleLocaleTest {

    // resolveRole only reads the allowRoleOverride flag — the object collaborators are unused,
    // so a no-context instance with allowRoleOverride=true exercises the fold directly.
    private static AuthServiceImpl serviceWithRoleOverride() {
        return new AuthServiceImpl(
                null, null, null, null, null, null, null,
                30L, false, /* allowRoleOverride */ true);
    }

    @Test
    @Tag("ASVS")
    void resolveRole_lowercaseAdmin_resolvesUnderTurkishLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(serviceWithRoleOverride().resolveRole("admin"))
                .as("lowercase 'admin' must fold to ADMIN even under the Turkish locale "
                  + "(Locale.ROOT), not throw a 400")
                .isEqualTo(UserRole.ADMIN);
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    @Tag("ASVS")
    void resolveRole_lowercaseAdmin_resolvesUnderRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ROOT);
            assertThat(serviceWithRoleOverride().resolveRole("admin"))
                .isEqualTo(UserRole.ADMIN);
        } finally {
            Locale.setDefault(previous);
        }
    }
}
