package com.ax.template.authblueprint.register;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * monotone-register-l0 cumulative register (meter / odometer / counter). {@code anchor} is the latest
 * reading and only ever advances on a NORMAL read; it may be reset below its prior value ONLY via a
 * governed ROLLOVER/EXCHANGE read. {@code modulus} is the wrap ceiling (readings live in [0, modulus)).
 * {@code scopeKey}/{@code modulus}/{@code createdAt} are immutable; {@code anchor} moves ONLY via the
 * package-private {@link #advanceAnchor} (no public setter — {@link RegisterService} is the sole mutator,
 * always under a PESSIMISTIC_WRITE row lock). Column is {@code anchor_value} ("anchor" risks reserved-word
 * DDL). {@code @Version} backstops.
 */
@Entity
@Table(name = "registers")
// REG-MONOTONE-001 — anchor stays within [0, modulus); modulus positive. LIVE under ddl-auto.
@Check(constraints = "anchor_value >= 0 AND modulus > 0 AND anchor_value < modulus")
public class Register {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope_key", nullable = false, updatable = false, length = 200, unique = true)
    private String scopeKey;

    /** Wrap ceiling — readings are in [0, modulus). Exact BigDecimal (lang-bigdecimal-for-money). */
    @Column(name = "modulus", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal modulus;

    @Column(name = "anchor_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal anchor;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Register() {}

    public Register(UUID id, String scopeKey, BigDecimal modulus, BigDecimal anchor, Instant createdAt) {
        this.id = id;
        this.scopeKey = scopeKey;
        this.modulus = modulus;
        this.anchor = anchor;
        this.createdAt = createdAt;
    }

    /** Sole-mutator hook — set the anchor to a newly-read value (NORMAL advance, or governed reset). */
    void advanceAnchor(BigDecimal read) {
        this.anchor = read;
    }

    public UUID getId() { return id; }
    public String getScopeKey() { return scopeKey; }
    public BigDecimal getModulus() { return modulus; }
    public BigDecimal getAnchor() { return anchor; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
