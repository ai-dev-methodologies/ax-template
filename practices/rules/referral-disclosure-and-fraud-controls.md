---
title: A referral program MUST disclose the material connection, attribute trackably, and guard against self-referral / multi-account / refund-reversal fraud
impact: HIGH
impactDescription: "A referral reward paid without disclosing the referrer's material connection is a deceptive endorsement (FTC 16 CFR 255.5; 공정거래법 부당고객유인); one with no attribution window double-pays or misattributes; one with no fraud controls is drained by self-referral, multi-account farming, and rewards paid on orders later refunded. Each gap is either a legal-disclosure breach or direct financial loss."
tags:
  - referral
  - endorsement-disclosure
  - fraud-prevention
  - attribution
  - ftc
  - privacy
spec_ref: "specs/referral-program-l0.yaml#REFERRAL-DISCLOSURE-001"
verification:
  type: review
  source: "specs/referral-program-l0.yaml#REFERRAL-DISCLOSURE-001"
  pattern: "A referral program MUST issue trackable referral codes bound to a referrer and traceable to the referred signup/order (REFERRAL-CODE-001). Attribution MUST use a declared window and multi-touch policy (first/last-touch) so a conversion maps to exactly one rewardable referral, no double-credit (REFERRAL-ATTRIBUTION-001). Reward distribution follows a declared policy (who gets what, when it vests) (REFERRAL-REWARD-001). The referral/endorsement relationship MUST be disclosed clearly and conspicuously where it is not reasonably expected by the audience — the FTC material-connection rule (and 공정거래법 부당고객유인 / 표시광고법) (REFERRAL-DISCLOSURE-001). Fraud controls MUST block self-referral, detect multi-account/duplicate-device farming, and REVERSE (claw back) a reward when the qualifying order is later refunded/charged-back (REFERRAL-FRAUD-001). Referral-tracking PII is processed under a lawful basis and minimized (REFERRAL-PRIVACY-001, composes consent/PII handling). Reject a reward paid without disclosure, attribution that can double-credit one conversion, a reward that survives a refund of its qualifying order, and a program that pays self-referrals."
upstream:
  - "https://www.law.cornell.edu/cfr/text/16/255.5"
  - "https://www.law.go.kr/법령/독점규제및공정거래에관한법률"
evidence:
  - source_type: external
    citation: "FTC 16 CFR § 255.5 — Disclosure of material connections"
    url: "https://www.law.cornell.edu/cfr/text/16/255.5"
    quote: "When there exists a connection between the endorser and the seller of the advertised product that might materially affect the weight or credibility of the endorsement, and that connection is not reasonably expected by the audience, such connection must be disclosed clearly and conspicuously."
    quoted_at: "2026-06-06"
decided_at: "2026-06-06"
---

## A referral program MUST disclose the material connection, attribute trackably, and guard against fraud

**Impact: HIGH — A referral program sits on two fault lines: legal disclosure and financial fraud. On disclosure, the FTC's material-connection rule is explicit — *when there exists a connection between the endorser and the seller of the advertised product that might materially affect the weight or credibility of the endorsement, and that connection is not reasonably expected by the audience, such connection must be disclosed clearly and conspicuously* (16 CFR § 255.5; the Korean equivalent is 공정거래법 부당고객유인 / 표시·광고법). A reward paid for an undisclosed endorsement is a deceptive practice. On fraud, a program with no anti-abuse controls is a money tap: self-referrals, multi-account farming, and rewards paid on orders that are then refunded drain it directly. Both axes must be handled.**

There are six load-bearing requirements — the items of `specs/referral-program-l0.yaml`, all governed by this rule.

**1. Trackable referral code (REFERRAL-CODE-001).** Each referrer gets a code bound to their identity; a signup/order made through it is traceable back to that referrer for attribution and audit.

**2. Attribution window + multi-touch policy (REFERRAL-ATTRIBUTION-001).** Attribution uses a declared window (e.g. 30 days) and a declared multi-touch rule (first- or last-touch) so a single conversion maps to exactly ONE rewardable referral — never double-credited across two referrers or two touches.

**3. Reward distribution policy (REFERRAL-REWARD-001).** Who receives what (referrer / referee / both), the amount, and when it vests are a declared policy — not ad-hoc per payout.

**4. Material-connection disclosure (REFERRAL-DISCLOSURE-001).** The referral/endorsement relationship is disclosed *clearly and conspicuously* wherever the audience would not reasonably expect it — satisfying FTC § 255.5 and the Korean 부당고객유인 / 표시광고법 disclosure duties.

**5. Fraud controls (REFERRAL-FRAUD-001).** Self-referral is blocked (a user cannot refer themselves); multi-account / duplicate-device farming is detected and rejected; and a reward is REVERSED (clawed back) when its qualifying order is later refunded or charged back — a reward must not outlive the revenue that justified it.

**6. Tracking PII (REFERRAL-PRIVACY-001).** Referral-tracking data (who referred whom, devices, IPs) is processed under a lawful basis, minimized, and retained no longer than needed. Composes the consent / PII-handling discipline.

**Incorrect — pays on signup with no self-referral check, no refund reversal, no disclosure:**

```java
void onSignup(String refCode, String newUserId) {
    String referrer = codes.resolve(refCode);
    rewards.grant(referrer, REWARD);            // VIOLATION: no self-referral guard (referrer could == newUserId)
    // VIOLATION: granted on signup, never reversed if the qualifying order is refunded (REFERRAL-FRAUD-001)
    // VIOLATION: no material-connection disclosure recorded/surfaced (REFERRAL-DISCLOSURE-001)
}
```

**Correct — self-referral blocked, attribution windowed, reward vests on a non-refunded order, reversible:**

```java
void onQualifyingOrder(String refCode, String buyerId, Order order) {
    String referrer = codes.resolve(refCode);
    if (referrer.equals(buyerId)) return;                       // block self-referral (REFERRAL-FRAUD-001)
    if (fraud.isMultiAccount(referrer, buyerId, order)) return; // multi-account/device farming
    Attribution a = attribution.resolveSingle(refCode, buyerId, WINDOW); // one conversion → one referral
    if (a == null) return;
    rewards.grantPending(referrer, REWARD, order.id());         // vests only on a non-refunded order (REFERRAL-REWARD-001)
}
@EventListener void onRefund(OrderRefunded e) {
    rewards.reverseForOrder(e.orderId());                       // claw back on refund (REFERRAL-FRAUD-001)
}
// disclosure of the referral relationship is surfaced clearly+conspicuously per FTC §255.5 (REFERRAL-DISCLOSURE-001).
```

Verification: review-tier. Referral integrity is a financial + legal property with no compile-time signal — a naive program pays out and looks fine until it is farmed or audited for disclosure. Verify by review against `specs/referral-program-l0.yaml`: codes are trackable to a referrer; attribution is windowed and single-credit; reward policy is declared; the material connection is disclosed clearly and conspicuously; self-referral is blocked, multi-account detected, and rewards reversed on refund; tracking PII is lawful and minimized. When a fork-receiver wires real ITs (self-referral rejected; reward reversed on refund; double-touch credited once), this rule's verification may be upgraded from review to gradle_task+tag.

Reference: [FTC 16 CFR § 255.5 — Disclosure of material connections](https://www.law.cornell.edu/cfr/text/16/255.5)
