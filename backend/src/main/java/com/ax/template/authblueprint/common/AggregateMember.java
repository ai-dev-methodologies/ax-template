package com.ax.template.authblueprint.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DDD decomposition marker (spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §3).
 *
 * <p>Place {@code @AggregateMember(root = SomeRoot.class)} on a NON-root entity that
 * belongs inside an aggregate (e.g. {@code OrderItem} inside the {@code Order}
 * aggregate, {@code ApprovalStep} inside {@code ApprovalRequest}). The mandatory
 * {@link #root()} attribute names the owning {@link AggregateRoot} — this is what
 * completes the member→root map.
 *
 * <p>Why {@code root()} is required and not inferred: a marker like {@code @AggregateRoot}
 * alone cannot say WHICH root a member belongs to. A child→own-root back-reference
 * ({@code ApprovalStep.request} pointing at its own {@code ApprovalRequest}) is legal;
 * a pointer to a <em>different</em> aggregate's root is the violation. Only an explicit
 * {@code root} lets HG-AGG-REF tell the two apart.
 *
 * <p>Consumed by:
 * <ul>
 *   <li><b>HG-AGG-REPO</b> — a member entity must not have its own repository
 *       (mutate it through its root).</li>
 *   <li><b>HG-AGG-REF</b> — distinguishes a legal child→own-root back-reference from
 *       an illegal cross-aggregate object pointer.</li>
 *   <li><b>HG-AGG-MEMBER-ENCAP</b> — a member must not be referenced from outside its
 *       owning feature package.</li>
 * </ul>
 *
 * <p>{@link RetentionPolicy#RUNTIME} — ArchUnit reads {@link #root()} from bytecode.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateMember {

    /**
     * The {@link AggregateRoot}-annotated entity that owns this member. Required —
     * the member→root map is undefined without it.
     */
    Class<?> root();
}
