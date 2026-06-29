/**
 * Order-level tax application of a commerce platform: given a declared-exempt
 * scope and an injected jurisdiction rate, it decides whether an order owes tax
 * and reconciles the result to a single, idempotently-recomputed tax record. The
 * rate table, nexus rules, and any external tax provider are deliberately out of
 * scope (the rate is supplied), so the invariants hold for any tax engine.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Exempt-skip.</b> The non-exempt taxable base excludes every
 *       declared-exempt scope: a tax-exempt customer makes the order's taxable
 *       base {@code 0}; each tax-exempt line contributes {@code 0}; only
 *       non-exempt positive line bases are summed. Exemption is a <em>declared</em>
 *       property — never inferred from amounts, never a client-asserted figure.</li>
 *   <li><b>Idempotent recompute.</b> Re-pricing reconciles the order's tax to a
 *       single derived record by find-existing then update-or-create-or-remove:
 *       a taxable order has exactly one row carrying
 *       {@code amount == round(nonExemptTaxableBase * injectedRate)} half-up; a
 *       non-taxable order has no row. A second row per order is unrepresentable
 *       ({@code UNIQUE(order_id)}), and a now-exempt order's prior row is removed,
 *       not stranded. Repeated application has the same effect on persisted state
 *       as one application.</li>
 * </ol>
 * The amount is derived from the declared input plus the injected rate each time
 * (integer minor units), never read from the request.
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate roots</b> — {@link TaxableOrder} (the order with an
 *       {@code @ElementCollection} of {@link TaxLine}s and a re-declarable
 *       customer-exempt flag) and {@link TaxAssessment} (the single combined tax
 *       record referencing the order by identity, {@code UNIQUE(order_id)},
 *       {@code @Check} non-negative amounts, immutable identity, no public
 *       setter).</li>
 *   <li><b>Sole-mutator service</b> — {@link TaxApplicationService} is the only
 *       writer; its {@code computeTax}/{@code taxableBase} are pure (an injected
 *       {@link java.time.Clock} supplies only the computed-at stamp).</li>
 *   <li><b>Value type</b> — {@link TaxLine} (a per-line taxable contribution).</li>
 *   <li><b>Controller surface</b> — {@link TaxApplicationController} (thin HTTP);
 *       {@link TaxApplicationException} renders RFC 9457 Problem Details;
 *       {@link TaxApplicationMetrics} emits a bounded (outcome) counter.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testTaxApplication} (spec {@code tax-application-l0}, 3
 * families: EXEMPT-SKIP / IDEMPOTENT-RECOMPUTE / AUTHZ). The package ships
 * {@code TaxApplicationViolationProofTest} asserting the single-record
 * convergence and exempt-skip invariants are structurally enforced.
 *
 * <h2>External grounding</h2>
 * The single-record convergence is anchored to
 * <a href="https://www.rfc-editor.org/rfc/rfc9110#section-9.2.2">RFC&nbsp;9110
 * section 9.2.2</a> (Idempotent Methods — multiple identical requests have the
 * same effect as one); treating exemption as taxable, or summing an exempt line,
 * is the business-logic error class
 * <a href="https://cwe.mitre.org/data/definitions/840.html">CWE-840</a>.
 */
package com.ax.template.authblueprint.taxapplication;
