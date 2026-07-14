package com.ax.template.authblueprint.piecewisedeadband;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * piecewise-deadband-l0 config root: a bounded domain {@code [domainStart, domainEnd)} tiled by ordered
 * {@link DeadbandSegment} members (PWDB-SEGMENT-001). The config is IMMUTABLE after creation — there is no
 * update endpoint, no setter, and every column is {@code @Column(updatable=false)}; a change to the
 * obligation curve is a NEW config with its own key, so every historical {@link DeadbandEvaluation} stays
 * attributable to the exact curve it was evaluated against.
 */
@AggregateRoot
@Entity
@Table(name = "deadband_configs")
public class DeadbandConfig {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "config_key", nullable = false, updatable = false, length = 200, unique = true)
    private String configKey;

    @Column(name = "domain_start", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal domainStart;

    @Column(name = "domain_end", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal domainEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DeadbandConfig() {}

    public DeadbandConfig(UUID id, String configKey, BigDecimal domainStart, BigDecimal domainEnd,
                          Instant createdAt) {
        this.id = id;
        this.configKey = configKey;
        this.domainStart = domainStart;
        this.domainEnd = domainEnd;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getConfigKey() { return configKey; }
    public BigDecimal getDomainStart() { return domainStart; }
    public BigDecimal getDomainEnd() { return domainEnd; }
    public Instant getCreatedAt() { return createdAt; }
}
