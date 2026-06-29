/**
 * Rating-summary domain: a denormalized, derived aggregate (a product's cached average rating
 * and review count) kept provably consistent with the moderated reviews it summarizes. A common
 * read-optimization pattern wherever a hot aggregate is materialized from an underlying record set.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Derived-aggregate consistency (the keystone).</b> The stored average and count always
 *       equal the independent {@code AVG}/{@code COUNT} derivation over the eligible review set,
 *       because every state change recomputes the aggregate in the SAME transaction. There is no
 *       window in which a committed review and the cached aggregate disagree.</li>
 *   <li><b>Eligibility.</b> Only moderated (APPROVED) reviews feed the aggregate; a PENDING or
 *       REJECTED review never moves it.</li>
 *   <li><b>Empty sentinel.</b> An empty eligible set yields average = 0.00 and count = 0 — never a
 *       divide-by-zero — and an absent summary row reads as that same empty sentinel.</li>
 * </ol>
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link RatingSummary} is the derived aggregate; its {@code average}
 *       and {@code reviewCount} have NO public setter and an {@code @Version} guard. The only
 *       mutation path is the package-private {@code recomputeFrom}, so a caller cannot hand-edit the
 *       derived value.</li>
 *   <li><b>Source record + status</b> — {@link Review} and {@link ReviewStatus} (the moderation
 *       lifecycle whose APPROVED subset is eligible).</li>
 *   <li><b>Sole-mutator service</b> — {@link RatingSummaryService} is the only writer; approve /
 *       reject recompute the aggregate within the calling transaction, materializing the summary
 *       row lazily on first approval.</li>
 *   <li><b>Controller surface</b> — {@link RatingSummaryController} (thin HTTP);
 *       {@link RatingSummaryExceptions} renders RFC 9457 Problem Details.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testRatingSummary} (spec {@code derived-aggregate-consistency-l0}, 3 items:
 * CONSISTENCY / ELIGIBILITY / EMPTY). The package ships
 * {@code RatingSummaryViolationProofTest} asserting the derived fields have no public setter, so the
 * sole-mutator recompute path cannot be bypassed.
 *
 * <h2>External grounding</h2>
 * The empty-set sentinel closes the divide-by-zero classified as
 * <a href="https://cwe.mitre.org/data/definitions/369.html">CWE-369</a>; the recompute-in-the-same
 * -transaction discipline keeps the denormalized aggregate consistent with its source of truth; the
 * error contract is <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details.
 */
package com.ax.template.authblueprint.ratingsummary;
