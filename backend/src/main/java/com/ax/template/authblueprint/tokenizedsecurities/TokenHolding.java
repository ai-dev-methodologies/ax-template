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
