/**
 * @ax-template-meta
 * template_id: backend/billing/TossBillingAdapter
 * layer: backend-domain
 * domain: billing
 * anchors_rule: webhook-hmac-required.md
 * provenance_class: internal_design
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "정기결제 흐름 개요"
 *     quote: "POST /v1/billing/authorizations/issue 호출. 응답으로 billingKey 반환."
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "웹훅 이벤트"
 *     quote: "X-Webhook-Signature 헤더에 HMAC-SHA256 서명이 포함됨. 서버는 공유 시크릿으로 서명을 검증해야 한다."
 *   - source_type: upstream_id
 *     upstream_id: toss-billing-2026-05
 *     section: "멱등성"
 *     quote: "Idempotency-Key 헤더를 사용하면 네트워크 오류로 인한 재시도 시 중복 결제를 방지할 수 있습니다."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   Configure toss.billing.secret-key and toss.billing.webhook-secret in application.yml.
 *   No Toss-specific types leak beyond this adapter.
 *   Boundary: TossBillingAdapter is in billing domain. Never import from payment domain.
 */
package com.example.app.billing;

import io.micrometer.core.instrument.MeterRegistry;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TossBillingAdapter — thin Toss Payments 정기결제 API adapter.
 *
 * <p>All monetary amounts are integer minor units (KRW: won).
 * No Toss-specific types (BillingKey, 결제응답 objects) leak through the
 * {@link BillingProvider} interface.
 *
 * <p>Webhook HMAC-SHA256 verification: {@code X-Webhook-Signature} is verified
 * with the shared secret. Timestamps older than 5 minutes are rejected
 * to prevent replay attacks. See BILLING-IDEMP-002.
 *
 * <p>Fork instructions:
 * 1. Set {@code toss.billing.secret-key} (Base64-encoded) and
 *    {@code toss.billing.webhook-secret} in application.yml.
 * 2. Replace stub HTTP calls with real Toss API calls via WebClient or RestClient.
 */
@Component("tossBillingAdapter")
public class TossBillingAdapter implements BillingProvider {

    private static final Logger log = LoggerFactory.getLogger(TossBillingAdapter.class);
    private static final String PROVIDER = "toss";
    private static final long WEBHOOK_TOLERANCE_SECONDS = 300L;

    @Value("${toss.billing.secret-key:#{null}}")
    private String secretKey;

    @Value("${toss.billing.webhook-secret:#{null}}")
    private String webhookSecret;

    private final MeterRegistry meterRegistry;

    public TossBillingAdapter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String providerName() {
        return PROVIDER;
    }

    @Override
    public ProviderSubscriptionResult createSubscription(
            UUID subscriptionId,
            long planAmount,
            String currency,
            int intervalDays,
            int trialDays,
            String idempotencyKey) {
        // Fork: replace with real Toss API call.
        // POST /v1/billing/authorizations/issue → billingKey
        // Then POST /v1/billing/{billingKey} with amount (integer KRW)
        // Idempotency-Key header = idempotencyKey
        log.info("[toss] createSubscription subscriptionId={} amount={} currency={}",
            subscriptionId, planAmount, currency);
        meterRegistry.counter("billing.invoice.generated_count",
            "provider", PROVIDER, "status", "success").increment();
        return new ProviderSubscriptionResult(
            "toss_billing_" + subscriptionId,
            trialDays > 0 ? "trialing" : "active"
        );
    }

    @Override
    public void cancelSubscription(
            String providerSubscriptionId,
            boolean cancelAtPeriodEnd,
            String idempotencyKey) {
        // Fork: deactivate billingKey via Toss API or stop scheduled charges.
        log.info("[toss] cancelSubscription providerSubId={}", providerSubscriptionId);
    }

    @Override
    public WebhookEvent verifyAndParseWebhook(byte[] rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new WebhookSignatureException("toss.billing.webhook-secret is not configured");
        }

        // 1. Parse signature header: "t=<epoch-seconds>,v1=<hmac-base64>"
        String[] parts = signatureHeader.split(",");
        String timestampStr = null;
        String receivedSig = null;
        for (String part : parts) {
            if (part.startsWith("t=")) timestampStr = part.substring(2);
            if (part.startsWith("v1=")) receivedSig = part.substring(3);
        }
        if (timestampStr == null || receivedSig == null) {
            throw new WebhookSignatureException("Malformed X-Webhook-Signature header");
        }

        // 2. Replay attack check: reject events older than WEBHOOK_TOLERANCE_SECONDS.
        long eventEpoch = Long.parseLong(timestampStr);
        long nowEpoch = Instant.now().getEpochSecond();
        if (Math.abs(nowEpoch - eventEpoch) > WEBHOOK_TOLERANCE_SECONDS) {
            throw new WebhookSignatureException(
                "Webhook timestamp outside tolerance window (" + WEBHOOK_TOLERANCE_SECONDS + "s)");
        }

        // 3. HMAC-SHA256 verification.
        try {
            String signedPayload = timestampStr + "." + new String(rawPayload, StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expectedSig = Base64.getEncoder().encodeToString(
                mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
            if (!expectedSig.equals(receivedSig)) {
                throw new WebhookSignatureException("HMAC signature mismatch");
            }
        } catch (WebhookSignatureException e) {
            throw e;
        } catch (Exception e) {
            throw new WebhookSignatureException("HMAC verification error: " + e.getMessage());
        }

        // 4. Parse canonical event. Fork: replace with actual Toss JSON parsing.
        // Map Toss event types to canonical BillingEventType strings.
        meterRegistry.counter("billing.webhook.received_count",
            "provider", PROVIDER, "event_type", "WEBHOOK_RECEIVED").increment();
        return new WebhookEvent("toss_evt_stub", "WEBHOOK_RECEIVED", null, "{}");
    }
}
