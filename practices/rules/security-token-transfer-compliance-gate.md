---
title: A tokenized-security unit transfer MUST pass every compliance gate (recipient eligibility, lock-up, per-investor holding limit, sender balance) atomically before the append-only register is mutated; any gate failure rejects with no ledger change (fail-closed)
impact: HIGH
impactDescription: "A security token is a permissioned instrument: an un-gated transfer can place units with an ineligible investor, breach a lock-up, exceed a concentration cap, or create a negative balance — each a regulatory and conservation violation. The transfer and its compliance checks must be one atomic, fail-closed operation."
tags:
  - state-machine
  - audit
  - concurrency
  - securities
  - governance
spec_ref: "specs/tokenized-securities-l0.yaml#TS-TRANSFER-001"
verification:
  type: review
  source: "backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/SecurityTokenRegisterService.java"
  pattern: "transfer() loads the register under PESSIMISTIC_WRITE, checks idempotency, then units>0 → lock-up → balance → eligibility (deny-by-default SPI) → holding-limit, and only then calls applyTransfer; any failed gate throws before mutation."
upstream:
  - "https://www.law.go.kr/법령/주식·사채등의전자등록에관한법률"
  - "https://eips.ethereum.org/EIPS/eip-3643"
  - "https://www.rfc-editor.org/rfc/rfc9110.html"
  - "https://cwe.mitre.org/data/definitions/362.html"
evidence:
  - source_type: external
    citation: "주식·사채 등의 전자등록에 관한 법률(전자증권법) — 분산원장 전자등록계좌부 기재의 권리추정력·제3자 대항력 (2026 개정, 토큰증권)"
    url: "https://www.law.go.kr/법령/주식·사채등의전자등록에관한법률"
    quote: "전자등록계좌부에 전자등록된 자는 해당 전자등록주식등에 대하여 적법한 권리를 가지는 것으로 추정한다"
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "EIP-3643: T-REX — Permissioned tokens (transfer is allowed only when the compliance contract validates the transfer for both parties)"
    url: "https://eips.ethereum.org/EIPS/eip-3643"
    quote: "The transfer of tokens is only possible if the receiver is an eligible investor"
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "RFC 9110 HTTP Semantics §15.5.21 — 422 Unprocessable Content"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html"
    quote: "The 422 (Unprocessable Content) status code indicates that the server understands the content type of the request content"
    quoted_at: "2026-06-28"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition')"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource"
    quoted_at: "2026-06-28"
decided_at: "2026-06-28"
---

## A tokenized-security transfer is a fail-closed, atomic, gated mutation

**Impact: HIGH — an un-gated transfer corrupts the legal register.**

Under the amended 전자증권법, the distributed-ledger account book is the legal source of truth for
ownership (권리추정력 / 제3자 대항력). ERC-3643 enforces the same idea on-chain: a transfer only
settles when a compliance validator approves both parties. Modelled in the backend, the transfer must
load the register under a row lock, then pass every gate **before** any holding changes, and apply the
debit + credit + append-only entry in one transaction so total units are conserved.

**Incorrect — mutate first, validate loosely, or validate the sender instead of the recipient:**
```java
from.setUnits(from.getUnits() - units);   // mutated before any gate
to.setUnits(to.getUnits() + units);
if (!eligibility.isEligible(senderId)) { /* wrong party, and too late */ }
```

**Correct — every gate throws before `applyTransfer`; recipient is the eligibility subject:**
```java
if (register.isReplay(transferId)) return register.entryOf(transferId).orElseThrow();
if (units <= 0) throw invalidUnits();
if (now.isBefore(register.getLockupUntil())) throw lockupActive();
if (register.unitsOf(fromHolderId) < units) throw insufficientUnits();
if (!eligibility.isEligible(register.getId(), toHolderId)) throw ineligibleRecipient();  // deny-by-default
if (recipientNotIssuer && register.unitsOf(toHolderId) + units > register.getHoldingLimitPerInvestor())
    throw holdingLimitExceeded();
TransferEntry entry = register.applyTransfer(fromHolderId, toHolderId, units, transferId, now);  // atomic
```

Verification: review-tier — confirm the service rejects each gate with 422 and an unchanged register,
the eligibility SPI default denies, and `Σ holdings == totalUnits` after a successful transfer.

Reference: [EIP-3643](https://eips.ethereum.org/EIPS/eip-3643) · [전자증권법](https://www.law.go.kr/법령/주식·사채등의전자등록에관한법률)
