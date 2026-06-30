package com.ax.template.authblueprint.tokenizedsecurities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

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
@Table(name = "security_token_registers",
       uniqueConstraints = @UniqueConstraint(name = "uq_security_token_underlying_asset",
                                             columnNames = {"underlying_asset_id"}))
// entity↔migration parity (mirrors V077 chk_security_token_units): supply + per-investor cap are positive.
@Check(constraints = "total_units > 0 AND holding_limit_per_investor > 0")
public class SecurityTokenRegister {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "token_code", nullable = false, updatable = false, length = 100, unique = true)
    private String tokenCode;

    @Column(name = "underlying_asset_id", nullable = false, updatable = false, length = 200)
    private String underlyingAssetId;

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

    /** ISSUE-LIFECYCLE: DRAFT until issue() promotes to ISSUED (one-way, via SecurityTokenIssuanceStateMachine). */
    @Enumerated(EnumType.STRING)
    @Column(name = "issuance_status", nullable = false, length = 20)
    private IssuanceStatus issuanceStatus;

    @OneToMany(mappedBy = "register", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = jakarta.persistence.FetchType.EAGER)
    private List<TokenHolding> holdings = new ArrayList<>();

    @OneToMany(mappedBy = "register", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransferEntry> entries = new ArrayList<>();

    protected SecurityTokenRegister() {}

    SecurityTokenRegister(String tokenCode, String underlyingAssetId, SecurityType securityType,
                          long totalUnits, String issuerHolderId, Instant lockupUntil,
                          long holdingLimitPerInvestor, Instant createdAt) {
        this.id = UUID.randomUUID();
        this.tokenCode = tokenCode;
        this.underlyingAssetId = underlyingAssetId;
        this.securityType = securityType;
        this.totalUnits = totalUnits;
        this.issuerHolderId = issuerHolderId;
        this.lockupUntil = lockupUntil;
        this.holdingLimitPerInvestor = holdingLimitPerInvestor;
        this.createdAt = createdAt;
        // ISSUE-LIFECYCLE: token begins in DRAFT; issuer holding is seeded at issue() time.
        this.issuanceStatus = IssuanceStatus.DRAFT;
    }

    /**
     * Package-private: called ONLY by SecurityTokenIssuanceStateMachine.issue().
     * Promotes the token from DRAFT to ISSUED (one-way).
     */
    void markIssued() {
        this.issuanceStatus = IssuanceStatus.ISSUED;
    }

    /**
     * Package-private: seeds the issuer holding with the full supply.
     * Called ONLY by SecurityTokenRegisterService.issue() after the state machine marks ISSUED.
     * Conservation begins at issuance: Σ holdings == totalUnits immediately after this call.
     */
    void seedIssuerHolding() {
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
     * Sole mutation seam. Caller (service) MUST have already passed every compliance gate
     * AND obtained an anchorRef from OnChainAnchor.anchor() (ANCHOR-001).
     * Debits sender, credits recipient (creating the holding if absent), appends one immutable entry.
     * Conserves Σ units (a debit and an equal credit).
     */
    TransferEntry applyTransfer(String fromHolderId, String toHolderId, long units,
                                String transferId, Instant at, String anchorRef) {
        TokenHolding from = holdingOf(fromHolderId)
                .orElseThrow(() -> new IllegalStateException("sender holding must exist — gate bug"));
        from.setUnits(from.getUnits() - units);
        TokenHolding to = holdingOf(toHolderId).orElseGet(() -> {
            TokenHolding created = new TokenHolding(this, toHolderId, 0L);
            holdings.add(created);
            return created;
        });
        to.setUnits(to.getUnits() + units);
        TransferEntry entry = new TransferEntry(this, fromHolderId, toHolderId, units,
                transferId, at, anchorRef);
        entries.add(entry);
        return entry;
    }

    public UUID getId() { return id; }
    public String getTokenCode() { return tokenCode; }
    public String getUnderlyingAssetId() { return underlyingAssetId; }
    public SecurityType getSecurityType() { return securityType; }
    public long getTotalUnits() { return totalUnits; }
    public String getIssuerHolderId() { return issuerHolderId; }
    public Instant getLockupUntil() { return lockupUntil; }
    public long getHoldingLimitPerInvestor() { return holdingLimitPerInvestor; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public IssuanceStatus getIssuanceStatus() { return issuanceStatus; }
    public List<TokenHolding> getHoldings() { return List.copyOf(holdings); }
    public List<TransferEntry> getEntries() { return List.copyOf(entries); }
}
