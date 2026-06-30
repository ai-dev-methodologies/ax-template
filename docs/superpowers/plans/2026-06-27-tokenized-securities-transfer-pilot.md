# Tokenized-Securities — TRANSFER Pilot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a backend-only `tokenized-securities` domain to ax-template whose TRANSFER family
binarily enforces the STO compliance-gated transfer invariant — a security-token transfer that
fails eligibility / lock-up / holding-limit / balance is atomically rejected (fail-closed); only a
fully-gated transfer mutates the append-only register, conserving total units.

**Architecture:** One feature package `tokenizedsecurities`. Aggregate root `SecurityTokenRegister`
is the legal account book for one issued token; it owns `TokenHolding` (per-holder balances) and
`TransferEntry` (append-only log) as aggregate members. A second small aggregate root
`EligibleInvestor` backs an `InvestorEligibility` SPI seam (fail-closed default-deny) that a fork
later replaces with real KYC. A transfer mutates exactly ONE aggregate (the register) inside one
`@Transactional`, under a `PESSIMISTIC_WRITE` lock, after all compliance gates pass. No blockchain,
no ERC-3643 — only the chain-agnostic invariants. The fork adds the on-chain adapter on top.

**Tech Stack:** Spring Boot 3.2 + Jakarta Persistence (Hibernate `@Check`) + PostgreSQL/H2 + Flyway +
RFC 9457 ProblemDetail + RestAssured (black-box tests) + the project's `/ax-scaffold` + `/ax-plan`
domain-creation flow.

**Source spec:** `docs/superpowers/specs/2026-06-27-tokenized-securities-strategy-design.md` (§5 TRANSFER family).

---

## Naming (locked — keep byte-identical across all tasks)

| Thing | Value |
|---|---|
| Feature package | `com.ax.template.authblueprint.tokenizedsecurities` |
| Domain kebab (specs/allowlist) | `tokenized-securities` |
| Spec file | `specs/tokenized-securities-l0.yaml` |
| Class `@Tag` | `TOKENIZED_SECURITIES` |
| Gradle task | `testTokenizedSecurities` → `includeTags("TOKENIZED_SECURITIES")` |
| Item ids / method `@Tag` | `TS-TRANSFER-001` … `TS-TRANSFER-007` |
| HTTP base path | `/api/security-tokens` |
| Migration | `V076__create_tokenized_securities.sql` (next free number; verify before writing) |

**HTTP status contract:** create=201, transfer success / idempotent replay=200, grant=201,
not-found=404, duplicate tokenCode=409, **every compliance-gate rejection (eligibility / lock-up /
holding-limit / insufficient-units / invalid-units)=422** (RFC 9110 §15.5.21 — well-formed request,
transfer invariant forbids it).

---

## File Structure

**Create (main):**
- `backend/src/main/java/.../tokenizedsecurities/SecurityType.java` — enum
- `backend/src/main/java/.../tokenizedsecurities/SecurityTokenRegister.java` — `@AggregateRoot`
- `backend/src/main/java/.../tokenizedsecurities/TokenHolding.java` — `@AggregateMember`
- `backend/src/main/java/.../tokenizedsecurities/TransferEntry.java` — `@AggregateMember`
- `backend/src/main/java/.../tokenizedsecurities/EligibleInvestor.java` — `@AggregateRoot`
- `backend/src/main/java/.../tokenizedsecurities/SecurityTokenRegisterRepository.java`
- `backend/src/main/java/.../tokenizedsecurities/EligibleInvestorRepository.java`
- `backend/src/main/java/.../tokenizedsecurities/InvestorEligibility.java` — SPI
- `backend/src/main/java/.../tokenizedsecurities/AllowlistInvestorEligibility.java` — default impl
- `backend/src/main/java/.../tokenizedsecurities/SecurityTokenRegisterService.java` — sole mutator
- `backend/src/main/java/.../tokenizedsecurities/EligibleInvestorService.java`
- `backend/src/main/java/.../tokenizedsecurities/SecurityTokenRegisterController.java`
- `backend/src/main/java/.../tokenizedsecurities/EligibleInvestorController.java`
- `backend/src/main/java/.../tokenizedsecurities/TokenizedSecuritiesException.java`
- `backend/src/main/resources/db/migration/V076__create_tokenized_securities.sql`

**Modify:**
- `specs/tokenized-securities-l0.yaml` (created by scaffold, filled in Task 2)
- `practices/evals/trio_integrity_allowlist.yaml` (ax-plan adds entry)
- `backend/build.gradle.kts` (ax-plan adds task)
- `backend/src/main/java/.../security/SecurityConfig.java` (Task 11)
- `practices/rules/security-token-transfer-compliance-gate.md` (create, Task 13)

**Create (test):**
- `backend/src/test/java/.../tokenizedsecurities/TokenizedSecuritiesTestSupport.java`
- `backend/src/test/java/.../tokenizedsecurities/TokenizedSecuritiesComplianceTest.java`
- `backend/src/test/java/.../tokenizedsecurities/TokenizedSecuritiesViolationProofTest.java`

---

## Task 1: Scaffold the empty Spec Trio, convert to backend_only

**Files:** Create skeleton via script; then prune frontend artifacts.

- [ ] **Step 1: Run the scaffold (dry-run first)**

```bash
cd /Users/kyjin/dev/own/ai-dev-methodologies/ai-dev-methodologies-hq/workspace/ax-template
bash skills/ax-scaffold/scripts/new-domain.sh tokenized-securities --dry-run
bash skills/ax-scaffold/scripts/new-domain.sh tokenized-securities
```
Expected: exit 0; emits `specs/tokenized-securities-l0.yaml`, `specs/tokenized-securities-frontend-l0.yaml`,
`contracts/tokenized-securities-openapi.yaml`, `contracts/tokenized-securities-ui.yaml`,
`blueprints/tokenized-securities-manifest.yaml`, `blueprints/tokenized-securities-ui-manifest.yaml`,
`templates/L4/tokenized-securities/.gitkeep` — each spec carries `items: []` + `# TODO: Add`.

- [ ] **Step 2: Convert to backend_only (delete the frontend trio)**

```bash
rm specs/tokenized-securities-frontend-l0.yaml \
   contracts/tokenized-securities-ui.yaml \
   blueprints/tokenized-securities-ui-manifest.yaml
```

- [ ] **Step 3: Confirm the skeleton is detected**

Run: `bash skills/ax-plan/scripts/check-plan-complete.sh tokenized-securities`
Expected: FAIL with `PLAN_INCOMPLETE` (spec still has the `# TODO: Add` marker) — this confirms the
skeleton exists and is correctly blocked until Task 2 fills it.

- [ ] **Step 4: Commit**

```bash
git add specs/tokenized-securities-l0.yaml contracts/tokenized-securities-openapi.yaml \
        blueprints/tokenized-securities-manifest.yaml templates/L4/tokenized-securities/
git commit -m "chore(tokenized-securities): scaffold backend_only Spec Trio skeleton"
```

---

## Task 2: Fill the spec (7 TRANSFER items), run ax-plan to emit RED stubs + wiring

**Files:** `specs/tokenized-securities-l0.yaml`, then ax-plan writes the allowlist entry, the Gradle
task, and the 1:1 RED `@Tag` test stubs.

- [ ] **Step 1: Write the full spec**

Replace `specs/tokenized-securities-l0.yaml` with:

```yaml
version: "0.1.0"
scope: "tokenized-securities-l0 — chain-agnostic STO compliance-gated transfer: a security-token transfer that fails eligibility / lock-up / holding-limit / balance is atomically rejected (fail-closed); only a fully-gated transfer mutates the append-only register, conserving total units. ERC-3643's on-chain compliance validator, modelled in the backend."
stack: "Spring Boot 3.2.x + Jakarta Persistence (Hibernate @Check) + Flyway + RFC 9457 Problem Details + RestAssured"
standard: "전자증권법(주식·사채 등의 전자등록에 관한 법률) 개정 — 분산원장 계좌부 권리추정력·제3자대항력 + 자본시장법 적격투자자 + ERC-3643/EIP-3643 transfer compliance validator + RFC 9110 §15.5.21 (422) + CWE-362 (concurrency)"
domain_mode: backend_only
introduced_at: "2026-06-27 tokenized-securities TRANSFER pilot (Phase 0 / Approach 1 — invariant modelling, no chain)"

items:
  - id: "TS-TRANSFER-001"
    chapter: "Recipient eligibility is a hard transfer gate — an ungranted recipient is rejected, register unchanged"
    verification:
      mechanism: rule
      ref: security-token-transfer-compliance-gate
    requirement: >
      Every unit transfer MUST verify the RECIPIENT's eligibility via the InvestorEligibility SPI before
      any ledger mutation. A recipient without an eligibility grant MUST be rejected with HTTP 422 and the
      register (all holdings + entry log) MUST be byte-for-byte unchanged.
    test_method: "RestAssured — transfer to an ungranted holder → 422; GET token shows holdings + Σ unchanged"
    verification_type: "positive"
    applicable: true
    notes: "The defining STO invariant — a security token is a permissioned token; transfer ⇒ compliance check."

  - id: "TS-TRANSFER-002"
    chapter: "Lock-up — a transfer before the register's lockupUntil is rejected, register unchanged"
    requirement: >
      A transfer requested at an instant before the register's lockupUntil MUST be rejected with HTTP 422
      and leave the register unchanged. A transfer at/after lockupUntil proceeds to the remaining gates.
    test_method: "RestAssured — token A lockupUntil in the future → transfer 422; token B lockupUntil in the past → gate passes"
    verification_type: "positive"
    applicable: true
    notes: "전자증권법/자본시장법 전매제한(lock-up). Time compared to injected Clock."

  - id: "TS-TRANSFER-003"
    chapter: "Holding limit — a transfer making the recipient exceed holdingLimitPerInvestor is rejected"
    requirement: >
      If the recipient's post-transfer balance would exceed the register's holdingLimitPerInvestor, the
      transfer MUST be rejected with HTTP 422, register unchanged. The issuer holder (issuerHolderId) is
      exempt from the limit (treasury). The limit is enforced on the recipient side only.
    test_method: "RestAssured — limit=100; transfer 60 then 60 to the same investor → second is 422; investor balance stays 60"
    verification_type: "positive"
    applicable: true
    notes: "Per-investor concentration cap; issuer/treasury exempt (ERC-3643 agent exemption analogue)."

  - id: "TS-TRANSFER-004"
    chapter: "Balance — a transfer exceeding the sender's units is rejected; no negative balance is representable"
    requirement: >
      A transfer of more units than the sender holds MUST be rejected with HTTP 422, register unchanged.
      A negative holding balance MUST be structurally impossible (entity + DB @Check units >= 0).
    test_method: "RestAssured — sender holds 10, transfer 11 → 422, balances unchanged; ViolationProofTest asserts @Check"
    verification_type: "positive"
    applicable: true
    notes: "Conservation precondition — you cannot move units you do not have."

  - id: "TS-TRANSFER-005"
    chapter: "Atomic settlement — a fully-gated transfer debits + credits in one tx and appends exactly one immutable entry; Σ holdings == totalUnits"
    requirement: >
      When all gates pass, the transfer MUST in a single transaction (a) debit the sender, (b) credit the
      recipient (creating the holding if absent), and (c) append exactly one immutable TransferEntry. After
      the transfer the sum of all holding units MUST equal the register's totalUnits (conservation).
    test_method: "RestAssured — transfer 40 issuer→A; assert fromUnitsAfter, toUnitsAfter, and Σ holdings == totalUnits"
    verification_type: "positive"
    applicable: true
    notes: "분산원장 계좌부 = append-only legal book; the transfer is the only mutation."

  - id: "TS-TRANSFER-006"
    chapter: "Idempotency — replaying the same transferId does not re-mutate the register"
    requirement: >
      A transfer carries a caller-supplied transferId, unique within the register. Replaying a transfer
      with an already-applied transferId MUST return HTTP 200 with the original outcome and MUST NOT change
      any balance or append a second entry.
    test_method: "RestAssured — POST transfer twice with same transferId; balances after #2 == after #1"
    verification_type: "positive"
    applicable: true
    notes: "At-least-once callers (a chain re-org / retry) must not double-apply. Unique(register_id, transfer_id) backstops races."

  - id: "TS-TRANSFER-007"
    chapter: "Fail-closed — eligibility is deny-by-default; only an explicit admin grant enables a recipient"
    requirement: >
      The InvestorEligibility SPI default MUST be deny: with no eligibility grant, a recipient is ineligible
      (TS-TRANSFER-001 path). An ADMIN grant for (register, holder) MUST flip the recipient to eligible so a
      subsequent transfer to it succeeds. A non-admin grant attempt MUST be rejected (403).
    test_method: "RestAssured — pre-grant transfer → 422; non-admin grant → 403; admin grant → 201; post-grant transfer → 200"
    verification_type: "positive"
    applicable: true
    notes: "The SPI seam a fork replaces with on-chain ONCHAINID / KYC. Default-deny is the safe posture."
```

- [ ] **Step 2: Run ax-plan to bind the spec (allowlist + Gradle task + RED stubs)**

```bash
bash skills/ax-plan/scripts/check-plan-complete.sh tokenized-securities   # expect: still needs binding
bash skills/ax-plan/scripts/emit-red-stubs.sh tokenized-securities
```
Then ensure the allowlist + Gradle task exist (ax-plan does this; if the script does not, add them
manually as in Steps 3–4 below). Expected after binding:
`bash skills/ax-plan/scripts/check-plan-complete.sh tokenized-securities` → exit 0 `PLAN_COMPLETE`.

- [ ] **Step 3: Verify/add the allowlist entry**

Confirm `practices/evals/trio_integrity_allowlist.yaml` contains under `domains:`:
```yaml
  tokenized-securities: backend_only  # Phase 0 STO TRANSFER pilot — compliance-gated transfer; no UI in scope
```
If absent, add that line.

- [ ] **Step 4: Verify/add the Gradle task**

Confirm `backend/build.gradle.kts` contains (place it adjacent to the other `tasks.register<Test>` blocks):
```kotlin
tasks.register<Test>("testTokenizedSecurities") {
    useJUnitPlatform {
        includeTags("TOKENIZED_SECURITIES")
    }
}
```

- [ ] **Step 5: Confirm RED stubs compile-and-fail**

The emit step created `TokenizedSecuritiesComplianceTest.java` (7 `@Test @Tag("TS-TRANSFER-00N")`
stubs) and a `TokenizedSecuritiesViolationProofTest.java` stub, both `@Tag("TOKENIZED_SECURITIES")`.
Run: `cd backend && ./gradlew testTokenizedSecurities`
Expected: FAIL (stubs assert false / are empty) — RED baseline established.

- [ ] **Step 6: Commit**

```bash
git add specs/tokenized-securities-l0.yaml practices/evals/trio_integrity_allowlist.yaml \
        backend/build.gradle.kts backend/src/test/java/com/ax/template/authblueprint/tokenizedsecurities/ \
        docs/blueprints/tokenized-securities/
git commit -m "feat(tokenized-securities): bind spec (7 TRANSFER items) + allowlist + gradle task + RED stubs"
```

---

## Task 3: `SecurityType` enum

**Files:** Create `.../tokenizedsecurities/SecurityType.java`

- [ ] **Step 1: Write the enum**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

/** 자본시장법상 토큰화 대상 증권 유형 (조각투자 두 갈래). */
public enum SecurityType {
    /** 비금전신탁 수익증권 — 기초자산(채권·대출채권 등)을 신탁 후 발행. */
    TRUST_BENEFICIARY,
    /** 투자계약증권 — 기초자산 공유지분 양도 후 발행. */
    INVESTMENT_CONTRACT
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/SecurityType.java
git commit -m "feat(tokenized-securities): SecurityType enum (수익증권/투자계약증권)"
```

---

## Task 4: Aggregate-member entities — `TokenHolding`, `TransferEntry`

**Files:** Create `TokenHolding.java`, `TransferEntry.java`

- [ ] **Step 1: Write `TokenHolding`**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/** 한 발행 증권의 보유자별 잔고. 계좌부(SecurityTokenRegister)의 구성요소. */
@AggregateMember(root = SecurityTokenRegister.class)
@Entity
@Table(name = "token_holdings",
        uniqueConstraints = @UniqueConstraint(name = "uq_token_holding_holder",
                columnNames = {"register_id", "holder_id"}))
@Check(constraints = "units >= 0")
public class TokenHolding {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "register_id", nullable = false, updatable = false)
    private SecurityTokenRegister register;

    @Column(name = "holder_id", nullable = false, updatable = false, length = 200)
    private String holderId;

    @Column(name = "units", nullable = false)
    private long units;

    protected TokenHolding() {}

    TokenHolding(SecurityTokenRegister register, String holderId, long units) {
        this.id = UUID.randomUUID();
        this.register = register;
        this.holderId = holderId;
        this.units = units;
    }

    /** Sole-mutator hook — called only by SecurityTokenRegister.applyTransfer. */
    void setUnits(long units) { this.units = units; }

    public UUID getId() { return id; }
    public String getHolderId() { return holderId; }
    public long getUnits() { return units; }
}
```

Note: `setUnits` is intentionally package-private (the `ViolationProofTest` asserts it is not public).
`getMethods()` would flag a public `setUnits`; keeping it package-private keeps "no public setter" true.

- [ ] **Step 2: Write `TransferEntry`**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateMember;

/** Append-only 이전 기록 — 한 번 기록되면 불변. */
@AggregateMember(root = SecurityTokenRegister.class)
@Entity
@Table(name = "transfer_entries",
        uniqueConstraints = @UniqueConstraint(name = "uq_transfer_entry_transfer_id",
                columnNames = {"register_id", "transfer_id"}))
public class TransferEntry {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "register_id", nullable = false, updatable = false)
    private SecurityTokenRegister register;

    @Column(name = "from_holder_id", nullable = false, updatable = false, length = 200)
    private String fromHolderId;

    @Column(name = "to_holder_id", nullable = false, updatable = false, length = 200)
    private String toHolderId;

    @Column(name = "units", nullable = false, updatable = false)
    private long units;

    @Column(name = "transfer_id", nullable = false, updatable = false, length = 200)
    private String transferId;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected TransferEntry() {}

    TransferEntry(SecurityTokenRegister register, String fromHolderId, String toHolderId,
                  long units, String transferId, Instant recordedAt) {
        this.id = UUID.randomUUID();
        this.register = register;
        this.fromHolderId = fromHolderId;
        this.toHolderId = toHolderId;
        this.units = units;
        this.transferId = transferId;
        this.recordedAt = recordedAt;
    }

    public UUID getId() { return id; }
    public String getFromHolderId() { return fromHolderId; }
    public String getToHolderId() { return toHolderId; }
    public long getUnits() { return units; }
    public String getTransferId() { return transferId; }
    public Instant getRecordedAt() { return recordedAt; }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/TokenHolding.java \
        backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/TransferEntry.java
git commit -m "feat(tokenized-securities): TokenHolding + TransferEntry aggregate members"
```

---

## Task 5: Aggregate root `SecurityTokenRegister` (owns the gates + conservation)

**Files:** Create `SecurityTokenRegister.java`

- [ ] **Step 1: Write the root entity**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * 한 발행 증권의 분산원장 계좌부 — 권리의 single source of truth.
 * 이전(transfer)은 이 aggregate를 통해서만, 단일 트랜잭션으로 일어난다.
 */
@AggregateRoot
@Entity
@Table(name = "security_token_registers")
public class SecurityTokenRegister {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token_code", nullable = false, updatable = false, length = 100, unique = true)
    private String tokenCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "security_type", nullable = false, updatable = false, length = 30)
    private SecurityType securityType;

    @Column(name = "total_units", nullable = false, updatable = false)
    private long totalUnits;

    @Column(name = "issuer_holder_id", nullable = false, updatable = false, length = 200)
    private String issuerHolderId;

    @Column(name = "lockup_until", nullable = false, updatable = false)
    private Instant lockupUntil;

    @Column(name = "holding_limit_per_investor", nullable = false, updatable = false)
    private long holdingLimitPerInvestor;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "register", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TokenHolding> holdings = new ArrayList<>();

    @OneToMany(mappedBy = "register", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferEntry> entries = new ArrayList<>();

    protected SecurityTokenRegister() {}

    SecurityTokenRegister(String tokenCode, SecurityType securityType, long totalUnits,
                          String issuerHolderId, Instant lockupUntil, long holdingLimitPerInvestor,
                          Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tokenCode = tokenCode;
        this.securityType = securityType;
        this.totalUnits = totalUnits;
        this.issuerHolderId = issuerHolderId;
        this.lockupUntil = lockupUntil;
        this.holdingLimitPerInvestor = holdingLimitPerInvestor;
        this.createdAt = createdAt;
        // seed: issuer holds the full supply (treasury, limit-exempt)
        this.holdings.add(new TokenHolding(this, issuerHolderId, totalUnits));
    }

    Optional<TokenHolding> holdingOf(String holderId) {
        return holdings.stream().filter(h -> h.getHolderId().equals(holderId)).findFirst();
    }

    long unitsOf(String holderId) {
        return holdingOf(holderId).map(TokenHolding::getUnits).orElse(0L);
    }

    boolean isReplay(String transferId) {
        return entries.stream().anyMatch(e -> e.getTransferId().equals(transferId));
    }

    Optional<TransferEntry> entryOf(String transferId) {
        return entries.stream().filter(e -> e.getTransferId().equals(transferId)).findFirst();
    }

    /**
     * Sole mutation seam. Caller (service) MUST have already passed every compliance gate.
     * Debits sender, credits recipient (creating the holding if absent), appends one immutable entry.
     * Conserves Σ units (a debit and an equal credit).
     */
    TransferEntry applyTransfer(String fromHolderId, String toHolderId, long units,
                                String transferId, Instant at) {
        TokenHolding from = holdingOf(fromHolderId)
                .orElseThrow(() -> new IllegalStateException("sender holding must exist — gate bug"));
        from.setUnits(from.getUnits() - units);
        TokenHolding to = holdingOf(toHolderId).orElseGet(() -> {
            TokenHolding created = new TokenHolding(this, toHolderId, 0L);
            holdings.add(created);
            return created;
        });
        to.setUnits(to.getUnits() + units);
        TransferEntry entry = new TransferEntry(this, fromHolderId, toHolderId, units, transferId, at);
        entries.add(entry);
        return entry;
    }

    public UUID getId() { return id; }
    public String getTokenCode() { return tokenCode; }
    public SecurityType getSecurityType() { return securityType; }
    public long getTotalUnits() { return totalUnits; }
    public String getIssuerHolderId() { return issuerHolderId; }
    public Instant getLockupUntil() { return lockupUntil; }
    public long getHoldingLimitPerInvestor() { return holdingLimitPerInvestor; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TokenHolding> getHoldings() { return List.copyOf(holdings); }
    public List<TransferEntry> getEntries() { return List.copyOf(entries); }
}
```

- [ ] **Step 2: Compile-check**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL (entities compile; no migration exercised yet).

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/SecurityTokenRegister.java
git commit -m "feat(tokenized-securities): SecurityTokenRegister aggregate root (gates seam + conservation)"
```

---

## Task 6: `EligibleInvestor` aggregate root (the SPI-backing allowlist)

**Files:** Create `EligibleInvestor.java`

- [ ] **Step 1: Write the entity**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * (register, holder) 적격 부여 레코드. SecurityTokenRegister를 id로 참조(DDD — object pointer 아님).
 * fork에서는 이 allowlist를 on-chain ONCHAINID / KYC 결과로 대체.
 */
@AggregateRoot
@Entity
@Table(name = "eligible_investors",
        uniqueConstraints = @UniqueConstraint(name = "uq_eligible_investor",
                columnNames = {"register_id", "holder_id"}))
public class EligibleInvestor {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "register_id", nullable = false, updatable = false)
    private UUID registerId;

    @Column(name = "holder_id", nullable = false, updatable = false, length = 200)
    private String holderId;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    protected EligibleInvestor() {}

    EligibleInvestor(UUID registerId, String holderId, Instant grantedAt) {
        this.id = UUID.randomUUID();
        this.registerId = registerId;
        this.holderId = holderId;
        this.grantedAt = grantedAt;
    }

    public UUID getId() { return id; }
    public UUID getRegisterId() { return registerId; }
    public String getHolderId() { return holderId; }
    public Instant getGrantedAt() { return grantedAt; }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/EligibleInvestor.java
git commit -m "feat(tokenized-securities): EligibleInvestor aggregate root (SPI allowlist, ref-by-id)"
```

---

## Task 7: Repositories

**Files:** Create `SecurityTokenRegisterRepository.java`, `EligibleInvestorRepository.java`

- [ ] **Step 1: Write `SecurityTokenRegisterRepository`**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityTokenRegisterRepository extends JpaRepository<SecurityTokenRegister, UUID> {

    Optional<SecurityTokenRegister> findByTokenCode(String tokenCode);

    boolean existsByTokenCode(String tokenCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM SecurityTokenRegister r WHERE r.tokenCode = :tokenCode")
    Optional<SecurityTokenRegister> findByTokenCodeForUpdate(@Param("tokenCode") String tokenCode);
}
```

- [ ] **Step 2: Write `EligibleInvestorRepository`**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibleInvestorRepository extends JpaRepository<EligibleInvestor, UUID> {

    boolean existsByRegisterIdAndHolderId(UUID registerId, String holderId);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/SecurityTokenRegisterRepository.java \
        backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/EligibleInvestorRepository.java
git commit -m "feat(tokenized-securities): repositories (PESSIMISTIC_WRITE finder + eligibility lookup)"
```

---

## Task 8: `InvestorEligibility` SPI + fail-closed default impl + `TokenizedSecuritiesException`

**Files:** Create `InvestorEligibility.java`, `AllowlistInvestorEligibility.java`, `TokenizedSecuritiesException.java`

- [ ] **Step 1: Write the SPI**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.UUID;

/**
 * 수취인 적격성 판정 seam. 기본 구현은 deny-by-default allowlist.
 * fork는 이를 on-chain ONCHAINID / KYC 어댑터로 교체한다.
 */
public interface InvestorEligibility {
    /** @return true ONLY when the holder is positively eligible for this register; absence ⇒ false. */
    boolean isEligible(UUID registerId, String holderId);
}
```

- [ ] **Step 2: Write the default impl**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** Fail-closed default — eligible only if an explicit grant row exists. */
@Component
public class AllowlistInvestorEligibility implements InvestorEligibility {

    private final EligibleInvestorRepository grants;

    public AllowlistInvestorEligibility(EligibleInvestorRepository grants) {
        this.grants = grants;
    }

    @Override
    public boolean isEligible(UUID registerId, String holderId) {
        return grants.existsByRegisterIdAndHolderId(registerId, holderId);
    }
}
```

- [ ] **Step 3: Write the exception (HTTP status carried on the exception, mirrors `ThresholdException`)**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import org.springframework.http.HttpStatus;

public class TokenizedSecuritiesException extends RuntimeException {

    private final HttpStatus status;
    private final String type;
    private final String code;

    private TokenizedSecuritiesException(HttpStatus status, String type, String code, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String type() { return type; }
    public String code() { return code; }

    public static TokenizedSecuritiesException notFound() {
        return new TokenizedSecuritiesException(HttpStatus.NOT_FOUND,
                "urn:problem:not-found", "RESOURCE_NOT_FOUND", "Security token not found");
    }

    public static TokenizedSecuritiesException duplicateTokenCode() {
        return new TokenizedSecuritiesException(HttpStatus.CONFLICT,
                "urn:problem:ts-duplicate-token", "TS_DUPLICATE_TOKEN",
                "A security token with this code already exists");
    }

    public static TokenizedSecuritiesException ineligibleRecipient() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-ineligible-recipient", "TS_INELIGIBLE_RECIPIENT",
                "Recipient is not an eligible investor for this security token");
    }

    public static TokenizedSecuritiesException lockupActive() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-lockup-active", "TS_LOCKUP_ACTIVE",
                "The security token is within its lock-up period; transfers are not permitted yet");
    }

    public static TokenizedSecuritiesException holdingLimitExceeded() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-holding-limit-exceeded", "TS_HOLDING_LIMIT_EXCEEDED",
                "Transfer would push the recipient over the per-investor holding limit");
    }

    public static TokenizedSecuritiesException insufficientUnits() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-insufficient-units", "TS_INSUFFICIENT_UNITS",
                "Sender does not hold enough units for this transfer");
    }

    public static TokenizedSecuritiesException invalidUnits() {
        return new TokenizedSecuritiesException(HttpStatus.UNPROCESSABLE_ENTITY,
                "urn:problem:ts-invalid-units", "TS_INVALID_UNITS",
                "units must be a positive whole number; totalUnits/limit must be > 0");
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/InvestorEligibility.java \
        backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/AllowlistInvestorEligibility.java \
        backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/TokenizedSecuritiesException.java
git commit -m "feat(tokenized-securities): InvestorEligibility SPI (fail-closed) + domain exception"
```

---

## Task 9: Services — `SecurityTokenRegisterService` (the gated sole mutator) + `EligibleInvestorService`

**Files:** Create `SecurityTokenRegisterService.java`, `EligibleInvestorService.java`

- [ ] **Step 1: Write `SecurityTokenRegisterService`**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityTokenRegisterService {

    private final SecurityTokenRegisterRepository registers;
    private final InvestorEligibility eligibility;
    private final Clock clock;

    public SecurityTokenRegisterService(SecurityTokenRegisterRepository registers,
                                        InvestorEligibility eligibility, Clock clock) {
        this.registers = registers;
        this.eligibility = eligibility;
        this.clock = clock;
    }

    @Transactional
    public SecurityTokenRegister createToken(String tokenCode, SecurityType securityType, long totalUnits,
                                             String issuerHolderId, Instant lockupUntil,
                                             long holdingLimitPerInvestor) {
        if (totalUnits <= 0 || holdingLimitPerInvestor <= 0) {
            throw TokenizedSecuritiesException.invalidUnits();
        }
        if (registers.existsByTokenCode(tokenCode)) {
            throw TokenizedSecuritiesException.duplicateTokenCode();
        }
        try {
            return registers.saveAndFlush(new SecurityTokenRegister(
                    tokenCode, securityType, totalUnits, issuerHolderId, lockupUntil,
                    holdingLimitPerInvestor, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {
            throw TokenizedSecuritiesException.duplicateTokenCode();
        }
    }

    /**
     * The compliance-gated transfer. Order: load+lock → idempotency replay → gates
     * (units>0 → lock-up → balance → eligibility → holding-limit) → atomic apply.
     * Any gate failure throws BEFORE any mutation (fail-closed).
     */
    @Transactional
    public TransferEntry transfer(String tokenCode, String fromHolderId, String toHolderId,
                                  long units, String transferId) {
        SecurityTokenRegister register = registers.findByTokenCodeForUpdate(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);

        // TS-TRANSFER-006 — idempotent replay: same transferId returns the original entry, no re-mutation
        if (register.isReplay(transferId)) {
            return register.entryOf(transferId).orElseThrow(TokenizedSecuritiesException::notFound);
        }
        // TS-TRANSFER-004 (precondition) — units must be a positive whole number
        if (units <= 0) {
            throw TokenizedSecuritiesException.invalidUnits();
        }
        // TS-TRANSFER-002 — lock-up
        if (Instant.now(clock).isBefore(register.getLockupUntil())) {
            throw TokenizedSecuritiesException.lockupActive();
        }
        // TS-TRANSFER-004 — sender balance
        if (register.unitsOf(fromHolderId) < units) {
            throw TokenizedSecuritiesException.insufficientUnits();
        }
        // TS-TRANSFER-001 / TS-TRANSFER-007 — recipient eligibility (fail-closed, deny-by-default)
        if (!eligibility.isEligible(register.getId(), toHolderId)) {
            throw TokenizedSecuritiesException.ineligibleRecipient();
        }
        // TS-TRANSFER-003 — per-investor holding limit (issuer/treasury exempt)
        if (!toHolderId.equals(register.getIssuerHolderId())) {
            long after = register.unitsOf(toHolderId) + units;
            if (after > register.getHoldingLimitPerInvestor()) {
                throw TokenizedSecuritiesException.holdingLimitExceeded();
            }
        }
        // TS-TRANSFER-005 — all gates passed: atomic debit+credit+append, Σ conserved
        TransferEntry entry = register.applyTransfer(fromHolderId, toHolderId, units, transferId,
                Instant.now(clock));
        registers.saveAndFlush(register);
        return entry;
    }

    @Transactional(readOnly = true)
    public SecurityTokenRegister getToken(String tokenCode) {
        return registers.findByTokenCode(tokenCode).orElseThrow(TokenizedSecuritiesException::notFound);
    }
}
```

- [ ] **Step 2: Write `EligibleInvestorService` (separate aggregate; one write per method)**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibleInvestorService {

    private final EligibleInvestorRepository grants;
    private final SecurityTokenRegisterRepository registers;
    private final Clock clock;

    public EligibleInvestorService(EligibleInvestorRepository grants,
                                   SecurityTokenRegisterRepository registers, Clock clock) {
        this.grants = grants;
        this.registers = registers;
        this.clock = clock;
    }

    /** Reads the register (one aggregate) to resolve its id, then writes ONE EligibleInvestor. */
    @Transactional
    public EligibleInvestor grant(String tokenCode, String holderId) {
        SecurityTokenRegister register = registers.findByTokenCode(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);
        if (grants.existsByRegisterIdAndHolderId(register.getId(), holderId)) {
            return grants.findAll().stream()
                    .filter(g -> g.getRegisterId().equals(register.getId()) && g.getHolderId().equals(holderId))
                    .findFirst().orElseThrow(TokenizedSecuritiesException::notFound);
        }
        try {
            return grants.saveAndFlush(new EligibleInvestor(register.getId(), holderId, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {
            // concurrent duplicate grant — idempotent
            return grants.findAll().stream()
                    .filter(g -> g.getRegisterId().equals(register.getId()) && g.getHolderId().equals(holderId))
                    .findFirst().orElseThrow(TokenizedSecuritiesException::notFound);
        }
    }
}
```

Note: a `Clock` bean must exist. The reference domains inject `java.time.Clock`; if no `@Bean Clock`
is present app-wide, add one to an existing `@Configuration` (e.g. a `CommonClockConfig`) — check with
`grep -rn "Clock clock" backend/src/main/java | head` and `grep -rn "Clock.systemUTC\|@Bean.*Clock" backend/src/main/java`.
If absent, add `@Bean Clock clock() { return Clock.systemUTC(); }` to `common`.

- [ ] **Step 3: Compile-check**

Run: `cd backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/SecurityTokenRegisterService.java \
        backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/EligibleInvestorService.java
git commit -m "feat(tokenized-securities): gated transfer service (sole mutator) + eligibility grant service"
```

---

## Task 10: Controllers (thin) + migration

**Files:** Create `SecurityTokenRegisterController.java`, `EligibleInvestorController.java`,
`V076__create_tokenized_securities.sql`

- [ ] **Step 1: Confirm the next migration number**

Run: `ls backend/src/main/resources/db/migration | sort | tail -3`
Use the next free `V###`. This plan assumes **V076**; adjust the filename + `@Tag` migration assertion
in Task 12 if a higher number already exists.

- [ ] **Step 2: Write `SecurityTokenRegisterController`**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityTokenRegisterController {

    public record CreateReq(@NotBlank @Size(max = 100) String tokenCode,
                            @NotNull SecurityType securityType,
                            @NotNull @Positive Long totalUnits,
                            @NotBlank @Size(max = 200) String issuerHolderId,
                            @NotNull Instant lockupUntil,
                            @NotNull @Positive Long holdingLimitPerInvestor) {}

    public record TransferReq(@NotBlank @Size(max = 200) String fromHolderId,
                              @NotBlank @Size(max = 200) String toHolderId,
                              @NotNull @Positive Long units,
                              @NotBlank @Size(max = 200) String transferId) {}

    public record HoldingDto(String holderId, long units) {
        static HoldingDto of(TokenHolding h) { return new HoldingDto(h.getHolderId(), h.getUnits()); }
    }

    public record TokenDto(String tokenCode, SecurityType securityType, long totalUnits,
                           String issuerHolderId, Instant lockupUntil, long holdingLimitPerInvestor,
                           long heldSum, List<HoldingDto> holdings, Long version) {
        static TokenDto of(SecurityTokenRegister r) {
            List<HoldingDto> hs = r.getHoldings().stream().map(HoldingDto::of).toList();
            long sum = hs.stream().mapToLong(HoldingDto::units).sum();
            return new TokenDto(r.getTokenCode(), r.getSecurityType(), r.getTotalUnits(),
                    r.getIssuerHolderId(), r.getLockupUntil(), r.getHoldingLimitPerInvestor(),
                    sum, hs, r.getVersion());
        }
    }

    public record TransferResultDto(String tokenCode, String transferId, String fromHolderId,
                                    String toHolderId, long units) {
        static TransferResultDto of(String tokenCode, TransferEntry e) {
            return new TransferResultDto(tokenCode, e.getTransferId(), e.getFromHolderId(),
                    e.getToHolderId(), e.getUnits());
        }
    }

    private final SecurityTokenRegisterService service;

    public SecurityTokenRegisterController(SecurityTokenRegisterService service) { this.service = service; }

    @PostMapping("/api/security-tokens")
    public ResponseEntity<TokenDto> create(@Valid @RequestBody CreateReq req) {
        SecurityTokenRegister r = service.createToken(req.tokenCode(), req.securityType(),
                req.totalUnits(), req.issuerHolderId(), req.lockupUntil(), req.holdingLimitPerInvestor());
        return ResponseEntity.status(HttpStatus.CREATED).body(TokenDto.of(r));
    }

    @PostMapping("/api/security-tokens/{tokenCode}/transfers")
    public TransferResultDto transfer(@PathVariable String tokenCode, @Valid @RequestBody TransferReq req) {
        TransferEntry e = service.transfer(tokenCode, req.fromHolderId(), req.toHolderId(),
                req.units(), req.transferId());
        return TransferResultDto.of(tokenCode, e);
    }

    @GetMapping("/api/security-tokens/{tokenCode}")
    public TokenDto get(@PathVariable String tokenCode) {
        return TokenDto.of(service.getToken(tokenCode));
    }

    @ExceptionHandler(TokenizedSecuritiesException.class)
    public ResponseEntity<ProblemDetail> handle(TokenizedSecuritiesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
```

- [ ] **Step 3: Write `EligibleInvestorController` (admin-only grant)**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EligibleInvestorController {

    public record GrantReq(@NotBlank @Size(max = 200) String holderId) {}
    public record GrantDto(String tokenCode, String holderId) {}

    private final EligibleInvestorService service;

    public EligibleInvestorController(EligibleInvestorService service) { this.service = service; }

    @PostMapping("/api/security-tokens/{tokenCode}/eligible-investors")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<GrantDto> grant(@PathVariable String tokenCode, @Valid @RequestBody GrantReq req) {
        EligibleInvestor g = service.grant(tokenCode, req.holderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(new GrantDto(tokenCode, g.getHolderId()));
    }

    @ExceptionHandler(TokenizedSecuritiesException.class)
    public ResponseEntity<ProblemDetail> handle(TokenizedSecuritiesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
```

- [ ] **Step 4: Write the Flyway migration**

`backend/src/main/resources/db/migration/V076__create_tokenized_securities.sql`:
```sql
-- tokenized-securities TRANSFER pilot — realizes specs/tokenized-securities-l0.yaml
-- Chain-agnostic STO compliance-gated transfer. No blockchain; backend invariant model.

CREATE TABLE security_token_registers (
    id                          UUID         NOT NULL PRIMARY KEY,
    token_code                  VARCHAR(100) NOT NULL,
    security_type               VARCHAR(30)  NOT NULL,   -- TRUST_BENEFICIARY | INVESTMENT_CONTRACT
    total_units                 BIGINT       NOT NULL,
    issuer_holder_id            VARCHAR(200) NOT NULL,
    lockup_until                TIMESTAMP    NOT NULL,
    holding_limit_per_investor  BIGINT       NOT NULL,
    version                     BIGINT       NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP    NOT NULL,
    CONSTRAINT chk_security_token_units CHECK (total_units > 0 AND holding_limit_per_investor > 0)
);
CREATE UNIQUE INDEX uq_security_token_code ON security_token_registers (token_code);

CREATE TABLE token_holdings (
    id           UUID         NOT NULL PRIMARY KEY,
    register_id  UUID         NOT NULL REFERENCES security_token_registers (id),
    holder_id    VARCHAR(200) NOT NULL,
    units        BIGINT       NOT NULL,
    CONSTRAINT chk_token_holding_units CHECK (units >= 0)
);
CREATE UNIQUE INDEX uq_token_holding_holder ON token_holdings (register_id, holder_id);

CREATE TABLE transfer_entries (
    id             UUID         NOT NULL PRIMARY KEY,
    register_id    UUID         NOT NULL REFERENCES security_token_registers (id),
    from_holder_id VARCHAR(200) NOT NULL,
    to_holder_id   VARCHAR(200) NOT NULL,
    units          BIGINT       NOT NULL,
    transfer_id    VARCHAR(200) NOT NULL,
    recorded_at    TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_transfer_entry_transfer_id ON transfer_entries (register_id, transfer_id);

CREATE TABLE eligible_investors (
    id           UUID         NOT NULL PRIMARY KEY,
    register_id  UUID         NOT NULL,
    holder_id    VARCHAR(200) NOT NULL,
    granted_at   TIMESTAMP    NOT NULL
);
CREATE UNIQUE INDEX uq_eligible_investor ON eligible_investors (register_id, holder_id);
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/SecurityTokenRegisterController.java \
        backend/src/main/java/com/ax/template/authblueprint/tokenizedsecurities/EligibleInvestorController.java \
        backend/src/main/resources/db/migration/V076__create_tokenized_securities.sql
git commit -m "feat(tokenized-securities): thin controllers + Flyway migration"
```

---

## Task 11: SecurityConfig — register the matchers (avoid the new-domain 403)

**Files:** Modify `backend/src/main/java/.../security/SecurityConfig.java`

- [ ] **Step 1: Add the request matchers**

Inside the `.authorizeHttpRequests(authz -> authz ...)` lambda, **before** the final
`.anyRequest().denyAll()`, add:
```java
// TOKENIZED_SECURITIES (tokenized-securities-l0): any authenticated user creates a token,
// transfers units (compliance-gated), and reads it; eligibility grant is ADMIN-only
// (enforced by @PreAuthorize on EligibleInvestorController).
.requestMatchers("/api/security-tokens/**").authenticated()
```
(`@PreAuthorize("hasAuthority('ROLE_ADMIN')")` on the grant method further restricts the
sub-resource — method security must be enabled app-wide; it already is for the admin domains.)

- [ ] **Step 2: Confirm method security is active**

Run: `grep -rn "EnableMethodSecurity\|EnableGlobalMethodSecurity" backend/src/main/java/com/ax/template/authblueprint/security/`
Expected: a match (so `@PreAuthorize` is honored). If absent, the grant test's 403 will not fire — but
the project's admin domains already rely on it, so it is present.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/ax/template/authblueprint/security/SecurityConfig.java
git commit -m "feat(tokenized-securities): SecurityConfig matchers for /api/security-tokens/**"
```

---

## Task 12: ViolationProofTest (structural negatives — MANDATORY guard)

**Files:** Replace the stub `.../tokenizedsecurities/TokenizedSecuritiesViolationProofTest.java`

- [ ] **Step 1: Write the full test**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("TOKENIZED_SECURITIES")
class TokenizedSecuritiesViolationProofTest {

    // TS-TRANSFER-004 — no negative balance is representable (entity @Check)
    @Test @Tag("TS-TRANSFER-004")
    void violation_holdingCarriesNonNegativeCheck() {
        Check check = TokenHolding.class.getAnnotation(Check.class);
        assertThat(check).as("TokenHolding must carry @Check(units >= 0)").isNotNull();
        assertThat(check.constraints().replaceAll("\\s+", " ")).contains("units >= 0");
    }

    // TS-TRANSFER-005 — TransferEntry is append-only: every column immutable, no public setter
    @Test @Tag("TS-TRANSFER-005")
    void violation_transferEntryIsImmutable() throws Exception {
        for (Method m : TransferEntry.class.getMethods()) {
            assertThat(m.getName()).as("TransferEntry must expose no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"register", "fromHolderId", "toHolderId", "units", "transferId", "recordedAt"}) {
            Column col = TransferEntry.class.getDeclaredField(f).getAnnotation(Column.class);
            if (col == null) {  // register uses @JoinColumn
                jakarta.persistence.JoinColumn jc = TransferEntry.class.getDeclaredField(f)
                        .getAnnotation(jakarta.persistence.JoinColumn.class);
                assertThat(jc).as(f + " must carry @JoinColumn").isNotNull();
                assertThat(jc.updatable()).as("TransferEntry." + f + " must be immutable").isFalse();
            } else {
                assertThat(col.updatable()).as("TransferEntry." + f + " must be immutable").isFalse();
            }
        }
    }

    // TS-TRANSFER-005 — holding balance has no PUBLIC setter (mutation only via the aggregate seam)
    @Test @Tag("TS-TRANSFER-005")
    void violation_holdingHasNoPublicUnitsSetter() throws Exception {
        for (Method m : TokenHolding.class.getMethods()) {
            assertThat(m.getName()).as("TokenHolding must expose no public setter").doesNotStartWith("set");
        }
        Method setUnits = TokenHolding.class.getDeclaredMethod("setUnits", long.class);
        assertThat(Modifier.isPublic(setUnits.getModifiers()))
                .as("TokenHolding.setUnits must be package-private (sole-mutator seam)").isFalse();
        assertThat(Modifier.isProtected(setUnits.getModifiers()))
                .as("TokenHolding.setUnits must not be protected (subclass escape)").isFalse();
    }

    // TS-TRANSFER-005 — register is versioned + identity/issuance columns immutable
    @Test @Tag("TS-TRANSFER-005")
    void violation_registerVersionedAndImmutableColumns() throws Exception {
        for (Method m : SecurityTokenRegister.class.getMethods()) {
            assertThat(m.getName()).as("SecurityTokenRegister must expose no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "tokenCode", "securityType", "totalUnits",
                "issuerHolderId", "lockupUntil", "holdingLimitPerInvestor", "createdAt"}) {
            Column col = SecurityTokenRegister.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("SecurityTokenRegister." + f + " must be immutable").isFalse();
        }
        assertThat(SecurityTokenRegister.class.getDeclaredField("version").isAnnotationPresent(Version.class))
                .as("SecurityTokenRegister.version must carry @Version").isTrue();
    }

    // TS-TRANSFER-006 — idempotency is backstopped by a unique (register_id, transfer_id) index in the migration
    // TS-TRANSFER-001 — eligibility default is deny (the only impl checks an explicit grant row)
    @Test @Tag("TS-TRANSFER-006") @Tag("TS-TRANSFER-001")
    void violation_migrationBackstopsIdempotencyAndNonNegativeBalance() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V076__create_tokenized_securities.sql")) {
            assertThat(in).as("V076__create_tokenized_securities.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("uq_transfer_entry_transfer_id");
            assertThat(sql).contains("units >= 0");
            assertThat(sql).contains("uq_eligible_investor");
        }
    }

    // TS-TRANSFER-007 — the only shipped eligibility impl is fail-closed (delegates to an existence check)
    @Test @Tag("TS-TRANSFER-007")
    void violation_eligibilityDefaultIsFailClosed() {
        assertThat(InvestorEligibility.class.isAssignableFrom(AllowlistInvestorEligibility.class)).isTrue();
        // the default impl returns the existence of a grant row — absence ⇒ false (deny-by-default)
        boolean[] probed = {false};
        InvestorEligibility failClosed = new AllowlistInvestorEligibility(new StubGrants(probed));
        assertThat(failClosed.isEligible(java.util.UUID.randomUUID(), "nobody")).isFalse();
        assertThat(probed[0]).as("default impl must consult the grant store").isTrue();
    }

    /** Minimal stub proving the default impl's deny path consults the store and returns false on absence. */
    static final class StubGrants implements EligibleInvestorRepository {
        private final boolean[] probed;
        StubGrants(boolean[] probed) { this.probed = probed; }
        @Override public boolean existsByRegisterIdAndHolderId(java.util.UUID r, String h) { probed[0] = true; return false; }
        // remaining JpaRepository methods are unused in this structural probe
        @Override public <S extends EligibleInvestor> S save(S e) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> java.util.List<S> saveAll(Iterable<S> e) { throw new UnsupportedOperationException(); }
        @Override public java.util.Optional<EligibleInvestor> findById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        @Override public java.util.List<EligibleInvestor> findAll() { throw new UnsupportedOperationException(); }
        @Override public java.util.List<EligibleInvestor> findAllById(Iterable<java.util.UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { throw new UnsupportedOperationException(); }
        @Override public void deleteById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        @Override public void delete(EligibleInvestor e) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllById(Iterable<? extends java.util.UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends EligibleInvestor> e) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { throw new UnsupportedOperationException(); }
        @Override public java.util.List<EligibleInvestor> findAll(org.springframework.data.domain.Sort sort) { throw new UnsupportedOperationException(); }
        @Override public org.springframework.data.domain.Page<EligibleInvestor> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> S saveAndFlush(S e) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> java.util.List<S> saveAllAndFlush(Iterable<S> e) { throw new UnsupportedOperationException(); }
        @Override public void flush() { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<EligibleInvestor> e) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<java.util.UUID> ids) { throw new UnsupportedOperationException(); }
        @Override public EligibleInvestor getOne(java.util.UUID id) { throw new UnsupportedOperationException(); }
        @Override public EligibleInvestor getById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        @Override public EligibleInvestor getReferenceById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> java.util.List<S> findAll(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> java.util.List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> long count(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor> boolean exists(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        @Override public <S extends EligibleInvestor, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { throw new UnsupportedOperationException(); }
    }
}
```

> If the `StubGrants` boilerplate is undesirable, replace `violation_eligibilityDefaultIsFailClosed`
> with a Mockito stub: `EligibleInvestorRepository repo = mock(...); when(repo.existsBy...).thenReturn(false);`
> and assert `new AllowlistInvestorEligibility(repo).isEligible(randomUUID(), "x")` is false. Mockito is
> already on the test classpath (used by other domains). Prefer the mock if available.

- [ ] **Step 2: Commit**

```bash
git add backend/src/test/java/com/ax/template/authblueprint/tokenizedsecurities/TokenizedSecuritiesViolationProofTest.java
git commit -m "test(tokenized-securities): ViolationProofTest — immutable entry, no-public-setter, fail-closed SPI, migration backstops"
```

---

## Task 13: ComplianceTest (RestAssured black-box) — fill the 7 RED stubs

**Files:** `.../tokenizedsecurities/TokenizedSecuritiesTestSupport.java` (create),
`.../tokenizedsecurities/TokenizedSecuritiesComplianceTest.java` (replace stub)

- [ ] **Step 1: Write the test support (token auth via live endpoints)**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import static io.restassured.RestAssured.given;

import java.util.UUID;

import io.restassured.RestAssured;

public final class TokenizedSecuritiesTestSupport {

    private TokenizedSecuritiesTestSupport() {}

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    public static void useRandomPort(int port) { RestAssured.port = port; }
}
```

- [ ] **Step 2: Write the compliance test**

```java
package com.ax.template.authblueprint.tokenizedsecurities;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("TOKENIZED_SECURITIES")
class TokenizedSecuritiesComplianceTest {

    @LocalServerPort int port;
    String member;
    String admin;

    @BeforeEach
    void setup() {
        TokenizedSecuritiesTestSupport.useRandomPort(port);
        member = TokenizedSecuritiesTestSupport.obtainToken(TokenizedSecuritiesTestSupport.freshEmail("ts-member"), "MEMBER");
        admin = TokenizedSecuritiesTestSupport.obtainToken(TokenizedSecuritiesTestSupport.freshEmail("ts-admin"), "ADMIN");
    }

    // ---- helpers -------------------------------------------------------------
    private String createToken(String issuer, long total, Instant lockupUntil, long limit) {
        String code = "TS-" + UUID.randomUUID();
        given().auth().oauth2(member).contentType(ContentType.JSON)
            .body("{\"tokenCode\":\"" + code + "\",\"securityType\":\"TRUST_BENEFICIARY\",\"totalUnits\":" + total
                + ",\"issuerHolderId\":\"" + issuer + "\",\"lockupUntil\":\"" + lockupUntil + "\""
                + ",\"holdingLimitPerInvestor\":" + limit + "}")
        .when().post("/api/security-tokens")
        .then().statusCode(201);
        return code;
    }

    private void grant(String code, String holder) {
        given().auth().oauth2(admin).contentType(ContentType.JSON)
            .body("{\"holderId\":\"" + holder + "\"}")
        .when().post("/api/security-tokens/" + code + "/eligible-investors")
        .then().statusCode(201);
    }

    private io.restassured.response.Response transfer(String code, String from, String to, long units, String tid) {
        return given().auth().oauth2(member).contentType(ContentType.JSON)
            .body("{\"fromHolderId\":\"" + from + "\",\"toHolderId\":\"" + to + "\",\"units\":" + units
                + ",\"transferId\":\"" + tid + "\"}")
        .when().post("/api/security-tokens/" + code + "/transfers");
    }

    private long heldSum(String code) {
        return given().auth().oauth2(member).when().get("/api/security-tokens/" + code)
            .then().statusCode(200).extract().jsonPath().getLong("heldSum");
    }

    private long unitsOf(String code, String holder) {
        return given().auth().oauth2(member).when().get("/api/security-tokens/" + code)
            .then().statusCode(200).extract().jsonPath()
            .getLong("holdings.find { it.holderId == '" + holder + "' }?.units ?: 0");
    }

    private final Instant past = Instant.now().minus(1, ChronoUnit.DAYS);
    private final Instant future = Instant.now().plus(1, ChronoUnit.DAYS);

    // ---- TS-TRANSFER-001 — recipient eligibility is a hard gate --------------
    @Test @Tag("TS-TRANSFER-001")
    void ungrantedRecipient_isRejected_422_registerUnchanged() {
        String code = createToken("ISSUER", 1000, past, 1000);
        long before = heldSum(code);
        transfer(code, "ISSUER", "ALICE", 10, "t1").then().statusCode(422).body("code", equalTo("TS_INELIGIBLE_RECIPIENT"));
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(before);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isZero();
    }

    // ---- TS-TRANSFER-002 — lock-up -----------------------------------------
    @Test @Tag("TS-TRANSFER-002")
    void transferDuringLockup_isRejected_422() {
        String code = createToken("ISSUER", 1000, future, 1000);
        grant(code, "ALICE");
        transfer(code, "ISSUER", "ALICE", 10, "t1").then().statusCode(422).body("code", equalTo("TS_LOCKUP_ACTIVE"));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isZero();
    }

    // ---- TS-TRANSFER-003 — holding limit ------------------------------------
    @Test @Tag("TS-TRANSFER-003")
    void transferExceedingHoldingLimit_isRejected_422() {
        String code = createToken("ISSUER", 1000, past, 100);
        grant(code, "ALICE");
        transfer(code, "ISSUER", "ALICE", 60, "t1").then().statusCode(200);
        transfer(code, "ISSUER", "ALICE", 60, "t2").then().statusCode(422).body("code", equalTo("TS_HOLDING_LIMIT_EXCEEDED"));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isEqualTo(60);
    }

    // ---- TS-TRANSFER-004 — sender balance -----------------------------------
    @Test @Tag("TS-TRANSFER-004")
    void transferExceedingSenderBalance_isRejected_422() {
        String code = createToken("ISSUER", 1000, past, 1000);
        grant(code, "ALICE");
        grant(code, "BOB");
        transfer(code, "ISSUER", "ALICE", 10, "t1").then().statusCode(200);
        transfer(code, "ALICE", "BOB", 11, "t2").then().statusCode(422).body("code", equalTo("TS_INSUFFICIENT_UNITS"));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "BOB")).isZero();
    }

    // ---- TS-TRANSFER-005 — atomic settlement + conservation -----------------
    @Test @Tag("TS-TRANSFER-005")
    void fullyGatedTransfer_debitsCreditsAtomically_conservesTotal() {
        String code = createToken("ISSUER", 1000, past, 1000);
        grant(code, "ALICE");
        transfer(code, "ISSUER", "ALICE", 40, "t1").then().statusCode(200)
            .body("fromHolderId", equalTo("ISSUER")).body("toHolderId", equalTo("ALICE")).body("units", equalTo(40));
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ISSUER")).isEqualTo(960);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isEqualTo(40);
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);  // conservation
    }

    // ---- TS-TRANSFER-006 — idempotency --------------------------------------
    @Test @Tag("TS-TRANSFER-006")
    void replayingSameTransferId_doesNotReMutate() {
        String code = createToken("ISSUER", 1000, past, 1000);
        grant(code, "ALICE");
        transfer(code, "ISSUER", "ALICE", 40, "t1").then().statusCode(200);
        transfer(code, "ISSUER", "ALICE", 40, "t1").then().statusCode(200);  // replay
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isEqualTo(40);   // not 80
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ISSUER")).isEqualTo(960);
        org.assertj.core.api.Assertions.assertThat(heldSum(code)).isEqualTo(1000);
    }

    // ---- TS-TRANSFER-007 — fail-closed default + admin grant flips it --------
    @Test @Tag("TS-TRANSFER-007")
    void eligibilityIsDenyByDefault_adminGrantEnables_nonAdminGrantForbidden() {
        String code = createToken("ISSUER", 1000, past, 1000);
        // pre-grant: deny-by-default
        transfer(code, "ISSUER", "ALICE", 10, "t1").then().statusCode(422).body("code", equalTo("TS_INELIGIBLE_RECIPIENT"));
        // non-admin grant attempt → 403
        given().auth().oauth2(member).contentType(ContentType.JSON).body("{\"holderId\":\"ALICE\"}")
            .when().post("/api/security-tokens/" + code + "/eligible-investors").then().statusCode(403);
        // admin grant → 201, then transfer succeeds
        grant(code, "ALICE");
        transfer(code, "ISSUER", "ALICE", 10, "t2").then().statusCode(200);
        org.assertj.core.api.Assertions.assertThat(unitsOf(code, "ALICE")).isEqualTo(10);
    }
}
```

- [ ] **Step 3: Run the domain test (expect GREEN)**

Run: `cd backend && ./gradlew testTokenizedSecurities`
Expected: BUILD SUCCESSFUL — all 7 compliance items + the ViolationProofTest pass.
If a test fails, fix the **implementation** (not the test) per superpowers:systematic-debugging, unless
the test encodes a wrong expectation.

- [ ] **Step 4: Commit**

```bash
git add backend/src/test/java/com/ax/template/authblueprint/tokenizedsecurities/TokenizedSecuritiesTestSupport.java \
        backend/src/test/java/com/ax/template/authblueprint/tokenizedsecurities/TokenizedSecuritiesComplianceTest.java
git commit -m "test(tokenized-securities): RestAssured ComplianceTest — 7 TRANSFER gates GREEN"
```

---

## Task 14: Evidence-anchored rule

**Files:** Create `practices/rules/security-token-transfer-compliance-gate.md`

- [ ] **Step 1: Write the rule (4 external evidence anchors)**

> Before writing, re-fetch each `quote` from its `url` and set `quoted_at` to today, so
> `evidence_quote_spotcheck_guard` (advisory) and any live audit pass. The quotes below are the
> anchors; verify the exact substring on the live page and adjust if the source wording differs.

```markdown
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
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "EIP-3643: T-REX — Permissioned tokens (transfer is allowed only when the compliance contract validates the transfer for both parties)"
    url: "https://eips.ethereum.org/EIPS/eip-3643"
    quote: "The transfer of tokens is only possible if the receiver is an eligible investor"
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "RFC 9110 HTTP Semantics §15.5.21 — 422 Unprocessable Content"
    url: "https://www.rfc-editor.org/rfc/rfc9110.html"
    quote: "The 422 (Unprocessable Content) status code indicates that the server understands the content type of the request content"
    quoted_at: "2026-06-27"
  - source_type: external
    citation: "CWE-362: Concurrent Execution using Shared Resource with Improper Synchronization ('Race Condition')"
    url: "https://cwe.mitre.org/data/definitions/362.html"
    quote: "The product contains a concurrent code sequence that requires temporary, exclusive access to a shared resource"
    quoted_at: "2026-06-27"
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
```

- [ ] **Step 2: Run the evidence gate locally (this commit touches `practices/`)**

Run: `bash practices/evals/evidence_guard.sh` (or the rule-scoped invocation the repo uses)
Expected: PASS (each evidence object has non-empty citation/url/quote/quoted_at). Fix any flagged field.

- [ ] **Step 3: Commit**

```bash
git add practices/rules/security-token-transfer-compliance-gate.md
git commit -m "feat(practices): rule — tokenized-security transfer compliance gate (4 external anchors)"
```

---

## Task 15: Full verification (R25 — the Iron Law)

**Files:** none (verification only)

- [ ] **Step 1: Fast iteration check**

Run: `bash verify/quick-verify.sh testTokenizedSecurities`
Expected: compile + structural-pregate (testPractices) + testTokenizedSecurities + guards all green.

- [ ] **Step 2: Domain green + all guards**

```bash
cd backend && ./gradlew testTokenizedSecurities
cd .. && bash practices/evals/run-all-guards.sh
```
Expected: domain BUILD SUCCESSFUL; guards report all green (incl. `l4_domain_reachability_guard`,
`aggregate_tagging_completeness_guard`, `entity_migration_guard`, `trio_integrity_guard`,
`test_tag_naming_convention_guard`, `spec_scaffold_unfilled_guard`).

- [ ] **Step 3: R25 completion gate**

Run: `bash practices/scripts/verify-completion.sh`
Expected: exit 0 (full regression + audit). If exit 1, apply the printed `fix_playbook` and re-run.
Do NOT declare done until this passes at HEAD.

- [ ] **Step 4: Update the CLAUDE.md per-domain status table + docs**

Add a row to the build/test matrix in `CLAUDE.md`:
```
| `./gradlew testTokenizedSecurities` | GREEN | 7/7 PASS (Phase 0 STO TRANSFER pilot — compliance-gated transfer: eligibility/lock-up/holding-limit/balance fail-closed + atomic conservation + idempotency; backend-only, chain-agnostic; fork adds ERC-3643 adapter) |
```
Bump any disk-true counts the `doc_headline_count_guard` enforces (Java rule count, domain count,
guard count if a guard was added — none was here). Re-run `verify-completion.sh` after doc edits
(it scans the working tree).

- [ ] **Step 5: Final commit**

```bash
git add CLAUDE.md docs/
git commit -m "docs(tokenized-securities): record TRANSFER pilot GREEN in status matrix"
```

---

## Self-Review (run before handing off)

**Spec coverage** (design §5 TRANSFER family → tasks):
- Eligibility gate (001) → Task 8 SPI + Task 9 service + Task 13 test ✓
- Lock-up (002) → Task 9 + Task 13 ✓
- Holding limit (003) → Task 9 (issuer-exempt) + Task 13 ✓
- Balance / no-negative (004) → Task 4 `@Check` + Task 9 + Task 10 migration + Task 12 ✓
- Atomic settlement + conservation (005) → Task 5 `applyTransfer` + Task 13 ✓
- Idempotency (006) → Task 5 `isReplay` + Task 10 unique index + Task 13 ✓
- Fail-closed default + admin grant (007) → Task 8 default impl + Task 10 grant controller + Task 12 + Task 13 ✓
- Mandatory artifacts (entity/repo/service/controller/state/migration/ComplianceTest/ViolationProofTest/gradle task/SecurityConfig) all present ✓
- Evidence-anchored rule (4 external anchors) → Task 14 ✓
- domain_mode backend_only + allowlist + R25 → Tasks 1, 2, 15 ✓

**Note on the state machine:** the pilot register has no status lifecycle (issuance lifecycle =
REGISTER family, iter 2), so no state-machine artifact is required; the §4 guards do not mandate one.
This is an intentional scope boundary, not a gap.

**Type consistency:** `applyTransfer(String,String,long,String,Instant)`, `isReplay(String)`,
`entryOf(String)`, `unitsOf(String)`, `holdingOf(String)`, `isEligible(UUID,String)`,
`existsByRegisterIdAndHolderId(UUID,String)`, `findByTokenCodeForUpdate(String)` — referenced
identically across Tasks 5/7/8/9/12. HTTP codes consistent (create 201, transfer 200, grant 201,
gate-reject 422, not-found 404, dup 409, non-admin grant 403).

**Open risk to watch during execution:**
1. `Clock` bean may not exist app-wide (Task 9 Step 2 checks; add `Clock.systemUTC()` bean if missing).
2. `/ax-plan` script behavior may differ from the assumed `emit-red-stubs.sh`; if the scripts don't
   auto-add the allowlist entry or Gradle task, Task 2 Steps 3–4 add them manually (idempotent).
3. RestAssured GroovyPath `holdings.find{...}` syntax (Task 13 `unitsOf`) — if the JsonPath engine
   rejects it, replace with a Java-side fetch of the `holdings` list and stream-filter.
4. Migration number V076 — verify with Task 10 Step 1; bump filename + the Task 12 assertion together.
```
