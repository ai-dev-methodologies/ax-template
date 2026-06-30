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
