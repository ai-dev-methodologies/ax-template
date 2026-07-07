package com.ax.template.authblueprint.tokenizedsecurities;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
                            @NotBlank @Size(max = 200) String underlyingAssetId,
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
                           long heldSum, List<HoldingDto> holdings, Long version, String issuanceStatus) {
        static TokenDto of(SecurityTokenRegister r) {
            List<HoldingDto> hs = r.getHoldings().stream().map(HoldingDto::of).toList();
            long sum = hs.stream().mapToLong(HoldingDto::units).sum();
            return new TokenDto(r.getTokenCode(), r.getSecurityType(), r.getTotalUnits(),
                    r.getIssuerHolderId(), r.getLockupUntil(), r.getHoldingLimitPerInvestor(),
                    sum, hs, r.getVersion(), r.getIssuanceStatus().name());
        }
    }

    public record TransferResultDto(String tokenCode, String transferId, String fromHolderId,
                                    String toHolderId, long units, String anchorRef) {
        static TransferResultDto of(String tokenCode, TransferEntry e) {
            return new TransferResultDto(tokenCode, e.getTransferId(), e.getFromHolderId(),
                    e.getToHolderId(), e.getUnits(), e.getAnchorRef());
        }
    }

    private final SecurityTokenRegisterService service;
    private final AnchorReconciliationService anchorReconciliation;

    public SecurityTokenRegisterController(SecurityTokenRegisterService service,
                                           AnchorReconciliationService anchorReconciliation) {
        this.service = service;
        this.anchorReconciliation = anchorReconciliation;
    }

    @PostMapping("/api/security-tokens")
    public ResponseEntity<TokenDto> create(@Valid @RequestBody CreateReq req) {
        SecurityTokenRegister r = service.createToken(req.tokenCode(), req.underlyingAssetId(),
                req.securityType(), req.totalUnits(), req.issuerHolderId(), req.lockupUntil(),
                req.holdingLimitPerInvestor());
        return ResponseEntity.status(HttpStatus.CREATED).body(TokenDto.of(r));
    }

    // HOLDER-AUTHZ-001: callerPrincipal bound to the JWT principal (authentication.getName()).
    // A fork replaces HolderAuthorization with on-chain identity (ERC-3643 ONCHAINID).
    @PostMapping("/api/security-tokens/{tokenCode}/transfers")
    public TransferResultDto transfer(@PathVariable String tokenCode, @Valid @RequestBody TransferReq req,
                                      Authentication authentication) {
        TransferEntry e = service.transfer(authentication.getName(), tokenCode,
                req.fromHolderId(), req.toHolderId(), req.units(), req.transferId());
        return TransferResultDto.of(tokenCode, e);
    }

    @GetMapping("/api/security-tokens/{tokenCode}")
    public TokenDto get(@PathVariable String tokenCode) {
        return TokenDto.of(service.getToken(tokenCode));
    }

    /**
     * ISSUE-002 — promote a DRAFT token to ISSUED; seeds issuer holding (ADMIN-only, one-way).
     * ISSUE-003 — auto-claims issuerHolderId for the calling admin principal (fail-safe).
     * Gate order: ISSUE-001 (issuance-state) fires before HOLDER-AUTHZ on transfers —
     * intentional semi-public disclosure; see F5 in dogfood-ledger/sto-generic-seams-iter1.md
     * for fork guidance on private-placement tokens.
     */
    @PostMapping("/api/security-tokens/{tokenCode}/issue")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public TokenDto issue(@PathVariable String tokenCode, Authentication authentication) {
        return TokenDto.of(service.issue(tokenCode, authentication.getName()));
    }

    /** ANCHOR-002 — compare register entries against the anchor view; report any divergence as breaks. */
    @GetMapping("/api/security-tokens/{tokenCode}/reconcile")
    public ReconcileResult reconcile(@PathVariable String tokenCode) {
        return anchorReconciliation.reconcile(tokenCode);
    }

    @ExceptionHandler(TokenizedSecuritiesException.class)
    public ResponseEntity<ProblemDetail> handle(TokenizedSecuritiesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
