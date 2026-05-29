package com.ax.template.authblueprint.common;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Cross-cutting caller-identity / owner-scope primitive — ships the REAL reusable
 * code for the owner-scope contract that IDW2 (ecommerce-seller-admin dogfood,
 * 2026-05-29) found was prose-only across every domain.
 *
 * <p>The dogfood observed all three personas hand-roll the SAME three things on
 * every authenticated endpoint:
 * <ol>
 *   <li>read the caller's user id as {@link Authentication#getName()};</li>
 *   <li>detect {@code ROLE_ADMIN} by looping {@link Authentication#getAuthorities()};</li>
 *   <li>decide the owner filter — "admin sees all rows, a non-admin sees only their
 *       own" — and, for a single-row read, return an IDOR-safe 404 when the row
 *       belongs to someone else.</li>
 * </ol>
 * Three copies of (2) drifted (one used {@code hasRole("ADMIN")} semantics that
 * forgot the {@code ROLE_} prefix; one compared the wrong authority string) and the
 * owner-scoped-404 in (3) was the kind of subtle authorization rule that silently
 * becomes a 403-leak when copy-pasted. This record centralises all three so a
 * fork-receiver derives them once, correctly.
 *
 * <h2>ownerScope() — the admin-sees-all filter</h2>
 * {@link #ownerScope()} returns the user id to filter list/find queries by when the
 * caller is NOT an admin, or {@code null} when the caller IS an admin (admin sees
 * every row). The {@code null} is a deliberate sentinel meaning "no owner
 * restriction"; pair it with a repository finder that treats a {@code null} owner as
 * "match all", e.g.
 * <pre>{@code
 * CallerScope caller = CallerScope.of(authentication);
 * // Repository: WHERE (:owner IS NULL OR owner_user_id = :owner)
 * return repository.findVisible(caller.ownerScope(), pageable);
 * }</pre>
 *
 * <h2>ownerScopedOrThrow / requireOwnerOr403 — single-row access</h2>
 * For a single-row read, look the row up unconditionally, then gate visibility with
 * {@link #ownerScopedOrThrow(Optional)} (or {@link #requireOwnerOr403(String)} when
 * the row is already loaded). A non-owner non-admin gets {@link ResourceNotFoundException}
 * (HTTP 404), never a 403 — the IDOR-safe rule documented on that exception. (The
 * {@code 403} naming on {@link #requireOwnerOr403(String)} reflects the conceptual
 * "you are not authorised" outcome; the wire status is intentionally 404 so existence
 * is never leaked. See {@link ResourceNotFoundException} for the rationale.)
 *
 * <h2>Framework-light</h2>
 * Spring Security {@link Authentication} in, plain immutable record out. The record
 * itself ({@code userId}, {@code isAdmin}) has no Spring dependency, so it is trivial
 * to construct in a unit test ({@code new CallerScope("u1", false)}) without a
 * security context. All methods are pure / side-effect-free except the deliberate
 * {@code throw} on the {@code ...OrThrow} / {@code require...} guards.
 */
public record CallerScope(String userId, boolean isAdmin) {

    /** The Spring Security authority string that grants admin (all-rows) visibility. */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Derive the caller scope from a Spring Security {@link Authentication}: the
     * {@code userId} is {@link Authentication#getName()} (the JWT subject / username),
     * and {@code isAdmin} is true iff {@link #ROLE_ADMIN} is present among the
     * granted authorities.
     *
     * @param authentication the current request's authentication; must be non-null
     *                       and authenticated (controllers receive this injected, so
     *                       a null here is a programming error, not a client error)
     * @return an immutable {@link CallerScope}
     * @throws IllegalArgumentException when {@code authentication} is null
     */
    public static CallerScope of(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("Authentication must be non-null to derive a CallerScope");
        }
        return new CallerScope(authentication.getName(), hasAdminRole(authentication));
    }

    /**
     * The owner id to filter queries by, or {@code null} when the caller is an admin
     * (admin sees all rows — no owner restriction). See the class javadoc for the
     * {@code WHERE (:owner IS NULL OR owner = :owner)} repository idiom.
     *
     * @return {@code null} for an admin caller, otherwise {@link #userId()}
     */
    public String ownerScope() {
        return isAdmin ? null : userId;
    }

    /**
     * Whether this caller may resolve {@code resourceOwnerId} — true if the caller is
     * an admin (sees all) or owns the resource.
     *
     * @param resourceOwnerId the owner id stored on the resource (may be null)
     * @return true when the caller is admin or the owner
     */
    public boolean canAccess(String resourceOwnerId) {
        return isAdmin || (userId != null && userId.equals(resourceOwnerId));
    }

    /**
     * Gate access to an already-loaded resource's owner id. Returns silently when the
     * caller {@link #canAccess(String)}; otherwise raises {@link ResourceNotFoundException}
     * (HTTP 404) — NOT a 403, so the resource's existence is never leaked to a
     * non-owner (the IDOR-safe rule; see {@link ResourceNotFoundException}).
     *
     * @param resourceOwnerId the owner id stored on the resource
     * @throws ResourceNotFoundException when the caller is neither admin nor the owner
     */
    public void requireOwnerOr403(String resourceOwnerId) {
        if (!canAccess(resourceOwnerId)) {
            throw new ResourceNotFoundException();
        }
    }

    /**
     * Unwrap a single-row lookup with IDOR-safe semantics: returns the value when the
     * {@link Optional} is present, otherwise throws {@link ResourceNotFoundException}
     * (HTTP 404).
     *
     * <p>The intended usage scopes the LOOKUP to the owner so a non-owner's row never
     * loads in the first place, and an absent row and a not-yours row collapse to the
     * SAME 404 — indistinguishable to the caller:
     * <pre>{@code
     * CallerScope caller = CallerScope.of(authentication);
     * // Owner-scoped finder: admin (ownerScope()==null) matches any row; a member
     * // only matches their own. Either "no such id" or "not mine" yields empty → 404.
     * Order order = caller.ownerScopedOrThrow(repository.findVisible(id, caller.ownerScope()));
     * }</pre>
     *
     * @param found the (owner-scoped) lookup result
     * @param <T>   the resource type
     * @return the present value
     * @throws ResourceNotFoundException when {@code found} is empty
     */
    public <T> T ownerScopedOrThrow(Optional<T> found) {
        return found.orElseThrow(ResourceNotFoundException::new);
    }

    private static boolean hasAdminRole(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ROLE_ADMIN.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
