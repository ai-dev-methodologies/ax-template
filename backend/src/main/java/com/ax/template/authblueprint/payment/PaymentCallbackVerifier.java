package com.ax.template.authblueprint.payment;

import java.util.Map;

/**
 * SPI for verifying redirect-style PG callback signatures.
 *
 * <p>One implementation per registered redirect-style provider (KG이니시스 /
 * NICE페이먼츠 / KCP / Toss V1 / etc.). Implementations are typically
 * {@code @Component} beans annotated to identify themselves to the
 * {@code PaymentCallbackController} routing layer.
 *
 * <p>Spec anchors:
 * <ul>
 *   <li>specs/payment-l0.yaml#PAYMENT-CALLBACK-001 — signature verify BEFORE
 *       state read; HTTP 401 on missing/mismatched signature; audit ledger
 *       row tagged {@code source=callback, outcome=signature_fail}</li>
 *   <li>blueprints/payment-manifest.yaml#callback — declares
 *       {@code signature_verification: required}, names this SPI as
 *       {@code signature_verifier_spi: PaymentCallbackVerifier}.</li>
 * </ul>
 *
 * <p>The catalog ships this interface as a contract surface only. The
 * controller, the routing registry (`PaymentCallbackVerifier#providerName`),
 * the per-PG implementations (KG/NICE/KCP signature algorithms), and the
 * service hook ({@link PaymentService#markCapturedFromCallback}) compose
 * around this SPI in a follow-up PRD (R18+). Fork-receivers implementing
 * a redirect-style PG provide their own {@code @Component} that returns a
 * matching {@link #providerName()} and validates per the PG's signature spec
 * (KG이니시스 SignatureKey, NICE페이먼츠 MerchantKey, etc.).
 */
public interface PaymentCallbackVerifier {

    /**
     * Provider slug this verifier handles. MUST match a value in
     * {@code blueprints/payment-manifest.yaml#provider.type_allowed}
     * (enforced by {@code practices/evals/payment_provider_type_enum_guard.sh}).
     * The path parameter {@code {provider}} in
     * {@code POST /api/payments/callback/{provider}} is routed to the
     * matching verifier by this name.
     */
    String providerName();

    /**
     * Verify the callback signature.
     *
     * <p>Implementations MUST reject (return {@link Result#invalid}) on:
     * <ul>
     *   <li>missing signature header / field</li>
     *   <li>signature mismatch under the PG's HMAC algorithm</li>
     *   <li>replay window violation (if the PG provides a nonce/timestamp)</li>
     * </ul>
     *
     * <p>Implementations MUST NOT mutate any persistent state during
     * verification. Callers (typically the controller) decide what to do with
     * the {@link Result} — on failure, the controller emits an audit row and
     * returns HTTP 401.
     *
     * @param rawPayload the form-encoded or JSON callback payload as received
     * @param headers request headers (some PGs put the signature here, e.g.
     *        {@code X-INI-SIGNATURE}; others embed it in the payload)
     * @return verification result; never null
     */
    Result verify(Map<String, String> rawPayload, Map<String, String> headers);

    /**
     * Verification outcome.
     *
     * @param valid          true if the signature checked out
     * @param tid            PG-issued transaction id (extracted from payload);
     *                       null when {@code valid == false}
     * @param orderId        the merchant order id from the payload (used by
     *                       the service to look up the {@code Payment} entity);
     *                       null when invalid
     * @param signedPayload  the canonical signed payload string (the exact
     *                       bytes the signature was computed over) — passed
     *                       through to {@link PaymentProvider#captureFromCallback}
     *                       so adapters can include it in the server-to-server
     *                       approval if required
     * @param failReason     short, non-secret reason code on failure (e.g.
     *                       {@code MISSING_SIGNATURE}, {@code SIGNATURE_MISMATCH},
     *                       {@code REPLAY_WINDOW}). Goes to the audit ledger.
     */
    record Result(
        boolean valid,
        String tid,
        String orderId,
        String signedPayload,
        String failReason
    ) {
        public static Result ok(String tid, String orderId, String signedPayload) {
            return new Result(true, tid, orderId, signedPayload, null);
        }

        public static Result invalid(String failReason) {
            return new Result(false, null, null, null, failReason);
        }
    }
}
