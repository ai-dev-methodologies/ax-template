package com.ax.template.authblueprint.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Unit coverage for {@link CallerScope} — closes the zero-code gap of the
 * owner-scope / IDOR-safe-404 contract IDW2 found prose-only.
 *
 * <p>Framework-light: builds real Spring Security {@link Authentication} tokens
 * (no mocking framework, no Spring context) so it runs under the default
 * {@code test} task. The {@code @Tag("COMMON_CALLER_SCOPE")} is UPPERCASE per
 * the test_tag_naming_convention_guard contract.
 */
@Tag("COMMON_CALLER_SCOPE")
class CallerScopeTest {

    private static Authentication admin(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static Authentication member(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    // ─── of(admin) → isAdmin true / ownerScope null (admin sees all) ──────

    @Test
    void of_adminAuthn_isAdminTrueAndOwnerScopeNull() {
        CallerScope caller = CallerScope.of(admin("boss"));
        assertThat(caller.userId()).isEqualTo("boss");
        assertThat(caller.isAdmin()).isTrue();
        // null ownerScope == "no owner restriction" — admin sees every row.
        assertThat(caller.ownerScope()).isNull();
    }

    // ─── of(member) → isAdmin false / ownerScope == userId ────────────────

    @Test
    void of_memberAuthn_isAdminFalseAndOwnerScopeIsUserId() {
        CallerScope caller = CallerScope.of(member("alice"));
        assertThat(caller.userId()).isEqualTo("alice");
        assertThat(caller.isAdmin()).isFalse();
        assertThat(caller.ownerScope()).isEqualTo("alice");
    }

    @Test
    void of_memberWithMultipleNonAdminRoles_isNotAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "u1", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_MEMBER"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")));
        assertThat(CallerScope.of(auth).isAdmin()).isFalse();
    }

    @Test
    void of_nullAuthentication_rejected() {
        assertThatThrownBy(() -> CallerScope.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void record_constructableWithoutSpringContext() {
        // Framework-light proof: trivial to build in a plain unit test.
        CallerScope caller = new CallerScope("u9", false);
        assertThat(caller.ownerScope()).isEqualTo("u9");
    }

    // ─── canAccess / requireOwnerOr403 (single already-loaded row) ─────────

    @Test
    void canAccess_ownerTrue_otherFalse_adminAlwaysTrue() {
        CallerScope alice = CallerScope.of(member("alice"));
        assertThat(alice.canAccess("alice")).isTrue();
        assertThat(alice.canAccess("bob")).isFalse();

        CallerScope admin = CallerScope.of(admin("root"));
        assertThat(admin.canAccess("anyone")).isTrue();
    }

    @Test
    void requireOwnerOr403_owner_proceeds() {
        CallerScope alice = CallerScope.of(member("alice"));
        assertThatCode(() -> alice.requireOwnerOr403("alice")).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOr403_admin_proceeds() {
        CallerScope admin = CallerScope.of(admin("root"));
        assertThatCode(() -> admin.requireOwnerOr403("someone-else")).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOr403_nonOwner_throws404NotForbidden_idorSafe() {
        // IDOR-safe: a non-owner sees 404, never 403 — existence is not leaked.
        CallerScope alice = CallerScope.of(member("alice"));
        assertThatThrownBy(() -> alice.requireOwnerOr403("bob"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── ownerScopedOrThrow (single owner-scoped lookup) ──────────────────

    @Test
    void ownerScopedOrThrow_present_returnsValue() {
        CallerScope alice = CallerScope.of(member("alice"));
        String value = alice.ownerScopedOrThrow(Optional.of("the-row"));
        assertThat(value).isEqualTo("the-row");
    }

    @Test
    void ownerScopedOrThrow_empty_throws404() {
        // An owner-scoped finder returns empty for BOTH "no such id" and "not mine"
        // — they collapse to the same 404, indistinguishable to the caller.
        CallerScope alice = CallerScope.of(member("alice"));
        assertThatThrownBy(() -> alice.ownerScopedOrThrow(Optional.empty()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resourceNotFound_mapsTo404ViaResponseStatus() {
        // The @ResponseStatus(NOT_FOUND) annotation is the zero-wiring default mapping.
        var annotation = ResourceNotFoundException.class.getAnnotation(
                org.springframework.web.bind.annotation.ResponseStatus.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
    }
}
