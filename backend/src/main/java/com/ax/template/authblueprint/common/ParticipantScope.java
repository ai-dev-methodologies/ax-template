package com.ax.template.authblueprint.common;

import java.util.function.BooleanSupplier;
import org.springframework.security.core.Authentication;

/**
 * Cross-cutting M:N-actor access primitive — ships the REAL reusable code for the
 * "a caller may access a resource because they have a RELATIONSHIP with it" contract
 * that IDW4 (hospital appointment + EMR-lite dogfood, 2026-05-30) found all three
 * personas hand-rolled identically (rule of three).
 *
 * <h2>The gap IDW4 closed</h2>
 * {@link CallerScope} models exactly one access shape: OWNER-vs-admin — a row carries
 * a single {@code owner_user_id} and the caller may see it iff they are that owner (or
 * an admin). That covers the dominant single-tenant CRUD case, but it does NOT cover
 * the regulated-domain shape that actually dominated the EMR dogfood and recurs across
 * marketplaces and messaging:
 * <ul>
 *   <li>a PROVIDER may read a PATIENT's encounter because a care RELATIONSHIP exists
 *       between them (neither party "owns" the row);</li>
 *   <li>a marketplace BUYER and SELLER may both read an order because each is a
 *       PARTICIPANT in it;</li>
 *   <li>a messaging participant may read a thread because they are a member of it.</li>
 * </ul>
 * In every case visibility is decided by a relationship that lives in the DOMAIN's own
 * tables (a {@code care_relationships} / {@code order_parties} / {@code thread_members}
 * row), NOT by an owner column on the resource. All three IDW4 personas re-derived the
 * same control flow by hand — "is the caller a participant? if not and not an admin,
 * 404" — and (exactly like the owner-scope drift {@link CallerScope} documents) the
 * relationship-absent branch is the kind of subtle authorization rule that silently
 * becomes a 403-leak (IDOR / BOLA) when copy-pasted. This record centralises it so a
 * fork-receiver derives it once, correctly.
 *
 * <h2>The domain supplies the relationship check; this primitive supplies the decision</h2>
 * This primitive deliberately knows NOTHING about how a relationship is stored — that is
 * irreducibly domain-specific (a JPA {@code existsBy...} query, a join-table lookup, a
 * graph traversal). The domain passes the relationship-existence test in as a
 * {@link BooleanSupplier} the primitive invokes lazily (so the — possibly expensive —
 * repository call is skipped entirely when the caller is an admin and the bypass applies):
 * <pre>{@code
 * ParticipantScope caller = ParticipantScope.of(authentication);
 * // The domain owns the "is there a care relationship?" predicate:
 * caller.requireParticipantOrThrow(
 *     () -> careRelationshipRepository.existsByProviderIdAndPatientId(caller.callerId(), patientId));
 * // ... only reached when the caller is a participant OR an admin.
 * Encounter e = encounterRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
 * }</pre>
 *
 * <h2>Composes with CallerScope (admin bypass)</h2>
 * Admin always bypasses the relationship check — an admin (ROLE_ADMIN) sees every row,
 * exactly as in {@link CallerScope#ownerScope()}. {@link #of(Authentication)} reuses
 * {@link CallerScope#of(Authentication)} so admin detection is derived once (no second,
 * driftable copy of the {@code ROLE_ADMIN} loop). For an emergency, relationship-LESS
 * provider read (the "I have no care relationship but this is a life-threatening
 * emergency" case) see {@link BreakGlass} — that is a deliberately separate, heavily
 * audited override, NOT a silent widening of this participant rule.
 *
 * <h2>IDOR-safe 404, never 403</h2>
 * A caller who is neither a participant nor an admin gets {@link ResourceNotFoundException}
 * (HTTP 404), never a 403 — the same IDOR-safe rule {@link ResourceNotFoundException}
 * documents: a 403 would confirm the resource exists and let an attacker enumerate ids
 * (OWASP API1:2023 Broken Object Level Authorization). "No relationship" and "no such
 * resource" must be indistinguishable to the caller.
 *
 * <h2>Framework-light</h2>
 * Spring Security {@link Authentication} in via {@link #of(Authentication)}, plain
 * immutable record out. The record itself ({@code callerId}, {@code isAdmin}) has no
 * Spring dependency, so it is trivial to construct in a unit test
 * ({@code new ParticipantScope("provider-1", false)}) without a security context, and
 * the relationship predicate is a plain {@link BooleanSupplier} a test can stub with a
 * lambda. All methods are pure / side-effect-free except the deliberate {@code throw} on
 * the {@code require...} guard and the lazy invocation of the supplied predicate.
 */
public record ParticipantScope(String callerId, boolean isAdmin) {

    /**
     * Derive the participant scope from a Spring Security {@link Authentication},
     * reusing {@link CallerScope#of(Authentication)} so the {@code callerId}
     * ({@link Authentication#getName()}) and admin detection ({@code ROLE_ADMIN}) are
     * derived by the single canonical implementation — never a second copy of the
     * authority loop that could drift.
     *
     * @param authentication the current request's authentication; must be non-null
     *                       (controllers receive this injected, so a null here is a
     *                       programming error, not a client error)
     * @return an immutable {@link ParticipantScope}
     * @throws IllegalArgumentException when {@code authentication} is null
     */
    public static ParticipantScope of(Authentication authentication) {
        CallerScope owner = CallerScope.of(authentication);
        return new ParticipantScope(owner.userId(), owner.isAdmin());
    }

    /**
     * Whether this caller may access a resource — true if the caller is an admin (sees
     * all, bypassing the relationship check) OR the supplied relationship predicate
     * reports that a relationship exists.
     *
     * <p>The predicate is evaluated lazily and ONLY when the caller is not an admin, so
     * an admin caller never pays for the (possibly expensive) relationship lookup.
     *
     * @param relationshipExists the domain-supplied test for "does a relationship between
     *                           this caller and the target resource exist?" — typically a
     *                           repository {@code existsBy...} call; must be non-null
     * @return true when the caller is admin or a participant
     * @throws IllegalArgumentException when {@code relationshipExists} is null
     */
    public boolean canAccess(BooleanSupplier relationshipExists) {
        if (relationshipExists == null) {
            throw new IllegalArgumentException("relationshipExists predicate must be non-null");
        }
        return isAdmin || relationshipExists.getAsBoolean();
    }

    /**
     * Gate access by the participant rule: returns silently when the caller
     * {@link #canAccess(BooleanSupplier)}; otherwise raises {@link ResourceNotFoundException}
     * (HTTP 404) — NOT a 403, so the resource's existence is never leaked to a
     * non-participant (the IDOR-safe rule; see {@link ResourceNotFoundException}).
     *
     * <p>Call this BEFORE returning any field of the resource, so a non-participant's
     * "no relationship" and a genuine "no such resource" collapse to the same 404,
     * indistinguishable to the caller:
     * <pre>{@code
     * ParticipantScope caller = ParticipantScope.of(authentication);
     * caller.requireParticipantOrThrow(
     *     () -> threadMemberRepository.existsByThreadIdAndUserId(threadId, caller.callerId()));
     * }</pre>
     *
     * @param relationshipExists the domain-supplied relationship-existence test; must be
     *                           non-null
     * @throws ResourceNotFoundException when the caller is neither admin nor a participant
     * @throws IllegalArgumentException when {@code relationshipExists} is null
     */
    public void requireParticipantOrThrow(BooleanSupplier relationshipExists) {
        if (!canAccess(relationshipExists)) {
            throw new ResourceNotFoundException();
        }
    }
}
