package com.ax.template.authblueprint.tokenizedsecurities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        for (String f : new String[]{"id", "tokenCode", "underlyingAssetId", "securityType", "totalUnits",
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
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V080__create_tokenized_securities.sql")) {
            assertThat(in).as("V080__create_tokenized_securities.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("uq_transfer_entry_transfer_id");
            assertThat(sql).contains("units >= 0");
            assertThat(sql).contains("uq_eligible_investor");
            // REG-ISSUE-001 — asset-level double-registration is backstopped at the DB layer
            // (independent of the service pre-check): one underlying asset → one security.
            assertThat(sql).contains("uq_security_token_underlying_asset");
        }
        // ISSUE-002 — V083 must backstop the enum at the DB layer (mirrors V080 @Check pattern)
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V083__add_issuance_status.sql")) {
            assertThat(in).as("V083__add_issuance_status.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).as("V083 must constrain issuance_status to the two valid enum values")
                    .contains("issuance_status IN ('DRAFT', 'ISSUED')");
        }
    }

    // HOLDER-AUTHZ-001/002 — HolderOwnership immutability + no public setter + migration unique index
    @Test @Tag("HOLDER-AUTHZ-001") @Tag("HOLDER-AUTHZ-002")
    void violation_holderOwnershipImmutable_noPublicSetter_migrationHasUniqueIndex() throws Exception {
        Column holderCol = HolderOwnership.class.getDeclaredField("holderId").getAnnotation(Column.class);
        assertThat(holderCol).as("holderId must carry @Column").isNotNull();
        assertThat(holderCol.updatable()).as("holderId must be immutable").isFalse();

        Column ownerCol = HolderOwnership.class.getDeclaredField("ownerPrincipal").getAnnotation(Column.class);
        assertThat(ownerCol).as("ownerPrincipal must carry @Column").isNotNull();
        assertThat(ownerCol.updatable()).as("ownerPrincipal must be immutable").isFalse();

        for (Method m : HolderOwnership.class.getMethods()) {
            assertThat(m.getName()).as("HolderOwnership must expose no public setter").doesNotStartWith("set");
        }

        try (InputStream in = getClass().getResourceAsStream("/db/migration/V081__create_holder_ownership.sql")) {
            assertThat(in).as("V081__create_holder_ownership.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("uq_holder_ownership_holder");
        }
    }

    // HOLDER-AUTHZ-002 — fail-closed falsification: orElse(false) branch is non-vacuous
    // Changing orElse(false)→orElse(true) would break this (mirrors violation_eligibilityDefaultIsFailClosed)
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("HOLDER-AUTHZ-002")
    void violation_holderAuthorizationDefaultIsFailClosed() {
        HolderOwnershipRepository repo = org.mockito.Mockito.mock(HolderOwnershipRepository.class);
        org.mockito.Mockito.when(repo.findByHolderId(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.empty());
        assertThat(new OwnershipHolderAuthorization(repo).controls("anyCaller", "unclaimedHolder"))
                .as("default impl must deny when no ownership row exists").isFalse();
    }

    // ANCHOR-001 — anchorRef is immutable on TransferEntry + migration V082 has the column
    @Test @Tag("ANCHOR-001")
    void violation_anchorRefImmutable() throws Exception {
        Column col = TransferEntry.class.getDeclaredField("anchorRef").getAnnotation(Column.class);
        assertThat(col).as("anchorRef must carry @Column").isNotNull();
        assertThat(col.updatable()).as("anchorRef must be immutable (updatable=false)").isFalse();
        assertThat(col.nullable()).as("anchorRef must be NOT NULL").isFalse();

        try (InputStream in = getClass().getResourceAsStream("/db/migration/V082__add_anchor_ref.sql")) {
            assertThat(in).as("V082__add_anchor_ref.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).as("migration must contain anchor_ref column").contains("anchor_ref");
        }
    }

    // ANCHOR-001 — blank anchorRef guard is non-vacuous: OnChainAnchor returning "" must throw
    // (deleting the isBlank() guard in SecurityTokenRegisterService would make this test fail)
    @Test @Tag("ANCHOR-001")
    void violation_blankAnchorRefGuardIsNonVacuous() {
        // Arrange: wire up the service with all gates set to PASS, anchor returns ""
        SecurityTokenRegisterRepository repo =
                org.mockito.Mockito.mock(SecurityTokenRegisterRepository.class);
        InvestorEligibility eligibility = org.mockito.Mockito.mock(InvestorEligibility.class);
        HolderAuthorization holderAuth = org.mockito.Mockito.mock(HolderAuthorization.class);
        OnChainAnchor blankAnchor = org.mockito.Mockito.mock(OnChainAnchor.class);
        Clock fixed = Clock.fixed(Instant.now(), ZoneOffset.UTC);

        String tokenCode = "TC-BLANK";
        String issuer = "ISSUER";
        String alice = "ALICE";
        String transferId = "tx-blank";

        SecurityTokenRegister reg = new SecurityTokenRegister(tokenCode, "ASSET-BLANK",
                SecurityType.TRUST_BENEFICIARY, 1000L, issuer,
                Instant.now(fixed).minusSeconds(86400), 1000L, Instant.now(fixed));

        org.mockito.Mockito.when(repo.findByTokenCodeForUpdate(tokenCode))
                .thenReturn(Optional.of(reg));
        org.mockito.Mockito.when(holderAuth.controls(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(issuer))).thenReturn(true);
        org.mockito.Mockito.when(eligibility.isEligible(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(alice))).thenReturn(true);
        org.mockito.Mockito.when(blankAnchor.anchor(org.mockito.ArgumentMatchers.any()))
                .thenReturn("");   // contract violation: blank ref

        SecurityTokenRegisterService svc =
                new SecurityTokenRegisterService(repo, eligibility, holderAuth, blankAnchor, fixed,
                        new SecurityTokenIssuanceStateMachine());

        // ISSUE-001: register must be ISSUED (not DRAFT) before transfer gate runs;
        // seedIssuerHolding so the balance gate (issuer holds totalUnits) also passes.
        reg.markIssued();
        reg.seedIssuerHolding();

        // Act + Assert: blank ref must throw before any ledger mutation
        assertThatThrownBy(() -> svc.transfer("caller", tokenCode, issuer, alice, 10L, transferId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blank anchor ref");
    }

    // ANCHOR-002 — reconcile divergence detection is non-vacuous (pure comparator unit test)
    @Test @Tag("ANCHOR-002")
    void violation_reconcileDetectsDivergence_nonVacuous() {
        // Build a minimal register (same package → package-private constructor accessible)
        SecurityTokenRegister reg = new SecurityTokenRegister("TC-VIO", "ASSET-VIO",
                SecurityType.TRUST_BENEFICIARY, 1000L, "ISSUER", Instant.now(), 1000L, Instant.now());

        // Two entries in the register (anchor refs present — ANCHOR-001 invariant satisfied)
        TransferEntry e1 = new TransferEntry(reg, "ISSUER", "ALICE", 40L, "t1", Instant.now(), "anchor:t1");
        TransferEntry e2 = new TransferEntry(reg, "ISSUER", "BOB", 20L, "t2", Instant.now(), "anchor:t2");

        // Matching anchor records
        AnchorRecord r1 = new AnchorRecord("t1", "ISSUER", "ALICE", 40L, "anchor:t1");
        AnchorRecord r2 = new AnchorRecord("t2", "ISSUER", "BOB", 20L, "anchor:t2");

        // Both sides identical → converged=true
        ReconcileResult converged = AnchorReconciliationService.reconcile(
                List.of(e1, e2), List.of(r1, r2));
        assertThat(converged.converged()).as("identical inputs must converge").isTrue();
        assertThat(converged.breaks()).isEmpty();

        // Entry-side miss: anchor missing t2 (dropped record) → converged=false, t2 in breaks
        ReconcileResult entryMiss = AnchorReconciliationService.reconcile(
                List.of(e1, e2), List.of(r1));
        assertThat(entryMiss.converged()).as("entry without anchor record must not converge").isFalse();
        assertThat(entryMiss.breaks()).as("dropped anchor transferId must be named").contains("t2");

        // Anchor-without-entry: anchor has r2 but register has only e1 → converged=false, t2 in breaks
        ReconcileResult anchorExtra = AnchorReconciliationService.reconcile(
                List.of(e1), List.of(r1, r2));
        assertThat(anchorExtra.converged()).as("extra anchor record without entry must not converge").isFalse();
        assertThat(anchorExtra.breaks()).as("orphan anchor transferId must be named").contains("t2");

        // Units mismatch: r2 with wrong units → converged=false, t2 in breaks
        AnchorRecord r2Mismatch = new AnchorRecord("t2", "ISSUER", "BOB", 999L, "anchor:t2");
        ReconcileResult unitsMismatch = AnchorReconciliationService.reconcile(
                List.of(e1, e2), List.of(r1, r2Mismatch));
        assertThat(unitsMismatch.converged()).as("units mismatch must not converge").isFalse();
        assertThat(unitsMismatch.breaks()).as("units-mismatched transferId must be named").contains("t2");
    }

    // ISSUE-002 — state machine is one-way (sole status mutator); no public status setter on register
    @Test @Tag("TOKENIZED_SECURITIES") @Tag("ISSUE-002")
    void violation_issuanceStateMachineIsOneWaySoleMutator() throws Exception {
        // ONLY the issue edge — no un-issue / reset / revoke method on the state machine
        long publicEdges = java.util.Arrays.stream(SecurityTokenIssuanceStateMachine.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && !m.isSynthetic())
                .count();
        assertThat(publicEdges)
                .as("SecurityTokenIssuanceStateMachine must declare exactly one public method (issue)").isEqualTo(1L);
        SecurityTokenIssuanceStateMachine.class.getDeclaredMethod("issue", SecurityTokenRegister.class);

        // SecurityTokenRegister must expose no public setter for issuanceStatus
        for (Method m : SecurityTokenRegister.class.getMethods()) {
            assertThat(m.getName())
                    .as("SecurityTokenRegister must expose no public setIssuanceStatus")
                    .doesNotStartWith("setIssuanceStatus");
        }

        // markIssued is package-private — not public (ensures state machine is the sole mutator seam)
        Method markIssued = SecurityTokenRegister.class.getDeclaredMethod("markIssued");
        assertThat(Modifier.isPublic(markIssued.getModifiers()))
                .as("markIssued must be package-private, not public").isFalse();
        assertThat(Modifier.isProtected(markIssued.getModifiers()))
                .as("markIssued must not be protected (subclass escape)").isFalse();
    }

    // TS-TRANSFER-007 — the only shipped eligibility impl is fail-closed (delegates to an existence check)
    @Test @Tag("TS-TRANSFER-007")
    void violation_eligibilityDefaultIsFailClosed() {
        assertThat(InvestorEligibility.class.isAssignableFrom(AllowlistInvestorEligibility.class)).isTrue();
        EligibleInvestorRepository repo = org.mockito.Mockito.mock(EligibleInvestorRepository.class);
        org.mockito.Mockito.when(repo.existsByRegisterIdAndHolderId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(false);
        assertThat(new AllowlistInvestorEligibility(repo).isEligible(java.util.UUID.randomUUID(), "nobody"))
                .as("default impl must deny when no grant row exists").isFalse();
    }
}
