/**
 * Identity-claim-on-first-auth domain: when a previously ANONYMOUS principal (a guest session)
 * authenticates for the first time, the records it accreted while anonymous (cart / order / draft /
 * wishlist) are CLAIMED by the now-authenticated identity. Cross-cutting — it recurs in every
 * guest-checkout-then-register and wishlist-before-signup flow.
 *
 * <h2>Correctness invariant</h2>
 * <ol>
 *   <li><b>Atomic claim (the keystone).</b> On first authentication, every record matching the
 *       guest's claim key transfers to the authenticated user id in ONE transaction — all of them
 *       or none — so a partial claim can never strand half a cart.</li>
 *   <li><b>Idempotent, exactly-once.</b> A replayed claim or a concurrent first-login from a second
 *       device is a no-op: the transfer is an atomic compare-and-set that only touches still-unclaimed
 *       rows, so a second invocation matches zero rows. At-least-once delivery yields exactly one
 *       effect — never a duplicate, never a lost record.</li>
 *   <li><b>Already-owned guard.</b> A record already owned by a DIFFERENT registered principal is
 *       refused: the transfer's {@code WHERE owner_user_id IS NULL} predicate makes claiming
 *       someone else's record structurally impossible, and the read path filters records owned by
 *       another principal so existence never leaks.</li>
 * </ol>
 *
 * <p>The binding that ties an anonymous record to its rightful claimant is possession of an
 * UNGUESSABLE claim token issued to the guest session — NOT a guessable identifier like a bare
 * email — so only the guest who holds the token can claim its records. (Handing the token to the
 * authenticated session is the auth boundary's job; this domain assumes a high-entropy key.)
 *
 * <h2>Key components (DDD shape)</h2>
 * <ul>
 *   <li><b>Aggregate root</b> — {@link ClaimableRecord} carries an immutable
 *       ({@code @Column(updatable=false)}) claim key and a nullable {@code ownerUserId} with NO
 *       public setter and an {@code @Version} guard; null means unclaimed.</li>
 *   <li><b>Atomic compare-and-set</b> — {@link ClaimableRecordRepository#claimUnowned} is the ONLY
 *       ownership-transfer path: a single {@code UPDATE ... WHERE owner_user_id IS NULL} that sets
 *       the owner only when the row is still unclaimed.</li>
 *   <li><b>Sole-mutator service</b> — {@link IdentityClaimService} performs the whole claim in one
 *       {@code @Transactional} and is the single writer of the aggregate.</li>
 *   <li><b>Controller surface</b> — {@link IdentityClaimController} (thin HTTP) over
 *       {@link IdentityClaimDtos}; Problem Details render the error contract.</li>
 * </ul>
 *
 * <h2>Verification</h2>
 * Run {@code ./gradlew testIdentityClaim} (spec {@code identity-claim-on-auth-l0}, 3 items: CLAIM /
 * IDEMPOTENT / GUARD). The package ships {@code IdentityClaimViolationProofTest} asserting the
 * ownership field has no public setter, so the atomic compare-and-set is the only transfer path.
 *
 * <h2>External grounding</h2>
 * The atomic compare-and-set transfer closes the check-then-act race classified as
 * <a href="https://cwe.mitre.org/data/definitions/367.html">CWE-367</a> (TOCTOU); keying the claim
 * on an unguessable token rather than a guessable email closes the improper-authorization exposure
 * of <a href="https://cwe.mitre.org/data/definitions/639.html">CWE-639</a> (Authorization Bypass
 * Through User-Controlled Key); the error contract is
 * <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457</a> Problem Details.
 */
package com.ax.template.authblueprint.identityclaim;
