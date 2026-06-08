package com.ax.template.authblueprint.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DDD decomposition marker (spec: docs/superpowers/specs/2026-06-08-ddd-decomposition-rules-design.md §3/§6).
 *
 * <p>Place {@code @PublishedApi} on the type(s) a feature package deliberately exposes
 * to other features — its published surface (typically a service, a request/response
 * record, or an SPI interface). Everything else in the package is internal.
 *
 * <p>This replaces the brittle "ends in Service" heuristic that HG-FEAT-ISOLATION
 * originally relied on. The isolation guard is <b>default-deny</b>: a cross-feature
 * reference is legal only when the target type is {@code @PublishedApi} (or listed in
 * {@code practices/evals/aggregate_boundary_allowlist.yaml}, or part of the shared
 * kernel). A type that merely ends in {@code Service} but is NOT published is treated
 * as internal.
 *
 * <p>Concept borrowed from Spring Modulith's published-API / named-interface idea, but
 * enforced by a hand-written ArchUnit guard (the catalog does not adopt the Modulith
 * framework — see spec §2).
 *
 * <p>{@link RetentionPolicy#RUNTIME} — ArchUnit reads this from bytecode.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PublishedApi {
}
