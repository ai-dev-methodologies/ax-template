package com.ax.template.authblueprint.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Unit coverage for {@link ParticipantScope} — closes the zero-code gap of the
 * M:N-actor / relationship-based access contract IDW4 (EMR-lite dogfood, 2026-05-30)
 * found all three personas hand-rolled identically.
 *
 * <p>Framework-light: builds real Spring Security {@link Authentication} tokens (no
 * mocking framework, no Spring context) and stubs the domain relationship predicate
 * with plain lambdas, so it runs under the default {@code test} task. The
 * {@code @Tag("COMMON_PARTICIPANT_SCOPE")} is UPPERCASE per the
 * test_tag_naming_convention_guard contract.
 */
@Tag("COMMON_PARTICIPANT_SCOPE")
class ParticipantScopeTest {

    private static final BooleanSupplier RELATED = () -> true;
    private static final BooleanSupplier NOT_RELATED = () -> false;

    private static Authentication admin(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static Authentication provider(String userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, "n/a", List.of(new SimpleGrantedAuthority("ROLE_MEMBER")));
    }

    // ─── of() composes with CallerScope (reuses admin detection) ──────────

    @Test
    void of_adminAuthn_isAdminTrue() {
        ParticipantScope caller = ParticipantScope.of(admin("boss"));
        assertThat(caller.callerId()).isEqualTo("boss");
        assertThat(caller.isAdmin()).isTrue();
    }

    @Test
    void of_nonAdminAuthn_isAdminFalse() {
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThat(caller.callerId()).isEqualTo("dr-house");
        assertThat(caller.isAdmin()).isFalse();
    }

    @Test
    void of_nullAuthentication_rejected() {
        assertThatThrownBy(() -> ParticipantScope.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void record_constructableWithoutSpringContext() {
        ParticipantScope caller = new ParticipantScope("provider-1", false);
        assertThat(caller.callerId()).isEqualTo("provider-1");
        assertThat(caller.isAdmin()).isFalse();
    }

    // ─── canAccess(predicate) ─────────────────────────────────────────────

    @Test
    void canAccess_participantTrue_nonParticipantFalse() {
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThat(caller.canAccess(RELATED)).isTrue();
        assertThat(caller.canAccess(NOT_RELATED)).isFalse();
    }

    @Test
    void canAccess_adminBypassesRelationship_evenWhenNotRelated() {
        ParticipantScope admin = ParticipantScope.of(admin("root"));
        assertThat(admin.canAccess(NOT_RELATED)).isTrue();
    }

    @Test
    void canAccess_adminDoesNotEvaluateRelationshipPredicate() {
        // The (possibly expensive) repo lookup must be skipped entirely for an admin.
        AtomicBoolean evaluated = new AtomicBoolean(false);
        BooleanSupplier tracking = () -> {
            evaluated.set(true);
            return false;
        };
        ParticipantScope admin = ParticipantScope.of(admin("root"));
        assertThat(admin.canAccess(tracking)).isTrue();
        assertThat(evaluated).isFalse();
    }

    @Test
    void canAccess_nonAdminEvaluatesRelationshipPredicate() {
        AtomicBoolean evaluated = new AtomicBoolean(false);
        BooleanSupplier tracking = () -> {
            evaluated.set(true);
            return true;
        };
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThat(caller.canAccess(tracking)).isTrue();
        assertThat(evaluated).isTrue();
    }

    @Test
    void canAccess_nullPredicate_rejected() {
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThatThrownBy(() -> caller.canAccess(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ─── requireParticipantOrThrow(predicate) — IDOR-safe 404 ─────────────

    @Test
    void requireParticipant_participant_proceeds() {
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThatCode(() -> caller.requireParticipantOrThrow(RELATED))
                .doesNotThrowAnyException();
    }

    @Test
    void requireParticipant_admin_proceeds() {
        ParticipantScope admin = ParticipantScope.of(admin("root"));
        assertThatCode(() -> admin.requireParticipantOrThrow(NOT_RELATED))
                .doesNotThrowAnyException();
    }

    @Test
    void requireParticipant_nonParticipant_throws404NotForbidden_idorSafe() {
        // IDOR-safe: a non-participant sees 404, never 403 — existence is not leaked.
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThatThrownBy(() -> caller.requireParticipantOrThrow(NOT_RELATED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireParticipant_nullPredicate_rejected() {
        ParticipantScope caller = ParticipantScope.of(provider("dr-house"));
        assertThatThrownBy(() -> caller.requireParticipantOrThrow(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
