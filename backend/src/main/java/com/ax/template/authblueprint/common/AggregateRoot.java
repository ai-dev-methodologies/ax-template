package com.ax.template.authblueprint.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DDD decomposition marker (spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §3).
 *
 * <p>Place {@code @AggregateRoot} on the ROOT entity of an aggregate — the single
 * entity that is the entry point for all mutation and the only one with global
 * repository access. An aggregate is, per Vernon (<em>Effective Aggregate Design</em>),
 * synonymous with a transactional consistency boundary; the root owns that boundary.
 *
 * <p>Pure naming cannot distinguish a root from a member ({@code Order} vs
 * {@code OrderItem} are both PascalCase {@code @Entity}). This marker (+ its sibling
 * {@link AggregateMember}) supplies the root/member map that three ArchUnit hard
 * guards key on:
 * <ul>
 *   <li><b>HG-AGG-REPO</b> — exactly one repository per {@code @AggregateRoot};
 *       member entities have no repository.</li>
 *   <li><b>HG-AGG-REF</b> — a field typed as an {@code @AggregateRoot} other than
 *       the declaring entity's own root is a cross-aggregate object pointer (forbidden;
 *       reference by identity instead).</li>
 *   <li><b>HG-AGG-MEMBER-ENCAP</b> — only the root is exposed outside the owning
 *       feature; members ({@link AggregateMember}) stay encapsulated.</li>
 * </ul>
 *
 * <p>A bounded context (= feature package) MAY contain multiple aggregate roots
 * (e.g. dispatch = Provider / Offer / ServiceRequest). This marker does NOT encode
 * one-root-per-package.
 *
 * <p>{@link RetentionPolicy#RUNTIME} — ArchUnit reads this from bytecode (it has no
 * source access), so it cannot be {@code SOURCE}-retained like {@link Phi}.
 *
 * <p>Evidence: Vernon, <em>Effective Aggregate Design</em> I–III (consistency boundary);
 * Evans, <em>DDD</em> (repository per aggregate root; root-only global access);
 * Fowler, <em>DDD_Aggregate</em> ("references from outside the aggregate should only
 * go to the aggregate root").
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AggregateRoot {
}
