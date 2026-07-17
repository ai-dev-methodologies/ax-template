package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;

import com.ax.template.authblueprint.common.IdempotentInsert;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityTokenRegisterService {

    private final SecurityTokenRegisterRepository registers;
    private final HolderOwnershipRepository ownerships;
    private final InvestorEligibility eligibility;
    private final HolderAuthorization holderAuthorization;
    private final OnChainAnchor onChainAnchor;
    private final IdempotentInsert idempotentInsert;
    private final Clock clock;
    private final SecurityTokenIssuanceStateMachine issuanceStateMachine;

    public SecurityTokenRegisterService(SecurityTokenRegisterRepository registers,
                                        HolderOwnershipRepository ownerships,
                                        InvestorEligibility eligibility,
                                        HolderAuthorization holderAuthorization,
                                        OnChainAnchor onChainAnchor,
                                        IdempotentInsert idempotentInsert,
                                        Clock clock,
                                        SecurityTokenIssuanceStateMachine issuanceStateMachine) {
        this.registers = registers;
        this.ownerships = ownerships;
        this.eligibility = eligibility;
        this.holderAuthorization = holderAuthorization;
        this.onChainAnchor = onChainAnchor;
        this.idempotentInsert = idempotentInsert;
        this.clock = clock;
        this.issuanceStateMachine = issuanceStateMachine;
    }

    @Transactional
    public SecurityTokenRegister createToken(String tokenCode, String underlyingAssetId,
                                             SecurityType securityType, long totalUnits,
                                             String issuerHolderId, Instant lockupUntil,
                                             long holdingLimitPerInvestor) {
        if (totalUnits <= 0 || holdingLimitPerInvestor <= 0) {
            throw TokenizedSecuritiesException.invalidUnits();
        }
        if (registers.existsByUnderlyingAssetId(underlyingAssetId)) {
            throw TokenizedSecuritiesException.duplicateUnderlyingAsset();
        }
        if (registers.existsByTokenCode(tokenCode)) {
            throw TokenizedSecuritiesException.duplicateTokenCode();
        }
        try {
            // P1-64 — isolate the racy insert in a REQUIRES_NEW inner tx so the unique-constraint
            // violation aborts only that inner tx; the catch-block discrimination reads below then
            // run in this (unpoisoned) outer tx even on PostgreSQL (25P02).
            return idempotentInsert.insert(() -> registers.saveAndFlush(new SecurityTokenRegister(
                    tokenCode, underlyingAssetId, securityType, totalUnits, issuerHolderId,
                    lockupUntil, holdingLimitPerInvestor, Instant.now(clock))));
        } catch (DataIntegrityViolationException e) {
            // Race backstop (the pre-checks above catch the single-threaded path); the unique
            // constraint that fired is either token_code or underlying_asset_id — re-read to map it.
            // The re-read is safe because the failing insert ran in its own (now rolled-back) inner tx.
            if (registers.existsByUnderlyingAssetId(underlyingAssetId)) {
                throw TokenizedSecuritiesException.duplicateUnderlyingAsset();
            }
            throw TokenizedSecuritiesException.duplicateTokenCode();
        }
    }

    /**
     * ISSUE-LIFECYCLE: promote a DRAFT token to ISSUED (ADMIN-only, enforced at controller).
     * Seeds the issuer holding = totalUnits exactly once. One-way via the state machine.
     *
     * <p>ISSUE-003 (F3 closure): if issuerHolderId has no prior HolderOwnership, auto-claim it
     * for callerPrincipal in the same transaction (fail-safe: no overwrite if already claimed
     * by a different principal). A fork-receiver who claims the issuer holder BEFORE calling
     * issue() keeps full control — the auto-claim is a convenience, not a takeover.
     */
    @Transactional
    public SecurityTokenRegister issue(String tokenCode, String callerPrincipal) {
        SecurityTokenRegister register = registers.findByTokenCodeForUpdate(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);
        if (register.getIssuanceStatus() == IssuanceStatus.ISSUED) {
            throw TokenizedSecuritiesException.alreadyIssued();
        }
        issuanceStateMachine.issue(register);   // DRAFT → ISSUED (sole status mutator)
        register.seedIssuerHolding();           // conservation begins: Σ holdings == totalUnits
        // ISSUE-003: auto-claim issuerHolderId → callerPrincipal (fail-safe, no overwrite)
        if (!ownerships.existsByHolderId(register.getIssuerHolderId())) {
            ownerships.save(new HolderOwnership(
                    register.getIssuerHolderId(), callerPrincipal, Instant.now(clock)));
        }
        return registers.saveAndFlush(register);
    }

    /**
     * The compliance-gated transfer. Order: load+lock → ISSUE-001 (FIRST gate, before caller-authz) →
     * HOLDER-AUTHZ → idempotency replay → gates (units>0 → lock-up → balance → eligibility → holding-limit)
     * → atomic apply. Any gate failure throws BEFORE any mutation (fail-closed).
     */
    @Transactional
    public TransferEntry transfer(String callerPrincipal, String tokenCode, String fromHolderId,
                                  String toHolderId, long units, String transferId) {
        SecurityTokenRegister register = registers.findByTokenCodeForUpdate(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);

        // ISSUE-001 — FIRST gate (before caller-authz): only ISSUED tokens may transfer
        if (register.getIssuanceStatus() != IssuanceStatus.ISSUED) {
            throw TokenizedSecuritiesException.notIssued();
        }

        // HOLDER-AUTHZ-001 — caller must control the debited holder (fail-closed)
        if (!holderAuthorization.controls(callerPrincipal, fromHolderId)) {
            throw TokenizedSecuritiesException.notHolderController();
        }

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
        // Issuer/treasury is always eligible — buybacks bypass the investor allowlist.
        boolean recipientIsIssuer = toHolderId.equals(register.getIssuerHolderId());
        if (!recipientIsIssuer && !eligibility.isEligible(register.getId(), toHolderId)) {
            throw TokenizedSecuritiesException.ineligibleRecipient();
        }
        // TS-TRANSFER-003 — per-investor holding limit (issuer/treasury exempt)
        if (!recipientIsIssuer) {
            long after = register.unitsOf(toHolderId) + units;
            if (after > register.getHoldingLimitPerInvestor()) {
                throw TokenizedSecuritiesException.holdingLimitExceeded();
            }
        }
        // ANCHOR-001 — anchor the transfer (same transaction); ref stored immutably on entry
        String anchorRef = onChainAnchor.anchor(
                new AnchorIntent(tokenCode, transferId, fromHolderId, toHolderId, units));
        // Guard: a blank ref (e.g. from a fork impl that swallows a chain RPC failure) must
        // never silently produce an un-anchored entry — throw before any ledger mutation.
        if (anchorRef == null || anchorRef.isBlank()) {
            throw new IllegalStateException(
                    "OnChainAnchor contract violation: blank anchor ref for transferId=" + transferId);
        }
        // TS-TRANSFER-005 — all gates passed: atomic debit+credit+append, Σ conserved
        TransferEntry entry = register.applyTransfer(fromHolderId, toHolderId, units, transferId,
                Instant.now(clock), anchorRef);
        registers.saveAndFlush(register);
        return entry;
    }

    @Transactional(readOnly = true)
    public SecurityTokenRegister getToken(String tokenCode) {
        return registers.findByTokenCode(tokenCode).orElseThrow(TokenizedSecuritiesException::notFound);
    }
}
