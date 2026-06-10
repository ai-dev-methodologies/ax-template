package com.ax.template.authblueprint.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Mutate-through-root persistence seam for {@link AggregateMember @AggregateMember} entities
 * (DDD decomposition — AX-DDD-MEMBER-REPO retire, BACKLOG P0-12~20).
 *
 * <p>A member entity MUST NOT own a Spring Data repository ({@code HG-AGG-REPO}): the member
 * is written only as part of its root's use cases. The root's SERVICE (the aggregate's sole
 * orchestrator, already inside the root's transaction/lock) persists members through THIS
 * seam; member READS live as explicit JPQL {@code @Query} methods on the ROOT's repository.
 *
 * <p>Constraint — this is NOT a general-purpose writer: call it only from the owning root's
 * service, inside that root's transaction, for entities marked {@code @AggregateMember}.
 * Using it to write another aggregate's data from outside its service re-creates exactly the
 * boundary leak the DDD guards exist to block. (The seam is no more powerful than the
 * {@code EntityManager} any bean could inject — its value is being the ONE named, reviewable
 * place member writes happen; precedent: {@link OptimisticLockingSupport} is the same
 * EntityManager-backed kind of common helper.)
 */
/* @Repository (NOT @Component) is load-bearing: PersistenceExceptionTranslationPostProcessor
 * proxies only @Repository beans, so a flush-time constraint violation surfaces as Spring's
 * DataIntegrityViolationException — exactly what the deleted SimpleJpaRepository proxies
 * provided and what callers' catch blocks (e.g. governedrecord sequence-conflict → 409) rely on. */
@Repository
public class MemberWriter {

    @PersistenceContext
    private EntityManager entityManager;

    /** Persist a new member row (INSERT). Returns the same instance for fluency. */
    public <T> T persist(T member) {
        entityManager.persist(member);
        return member;
    }

    /** Persist and flush immediately — for members whose constraints must surface in-call. */
    public <T> T persistAndFlush(T member) {
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }

    /** Load a member by primary key (the through-root replacement for memberRepo.findById). */
    public <T, ID> Optional<T> find(Class<T> memberType, ID id) {
        return Optional.ofNullable(entityManager.find(memberType, id));
    }
}
