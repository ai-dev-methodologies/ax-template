package com.ax.template.authblueprint.queryguard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;

import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * query-field-allowlist-l0 demonstrating resource: the rows a list/search endpoint pages over.
 * Its FOUR exposed fields — name, createdAt, status, priceMinor — are the ONLY sortable/filterable
 * surface (QueryGuardService binds them via {@link QueryFieldAllowlist}); every other property
 * (e.g. {@link #internalNotes}, which a caller MUST NOT be able to sort/filter on or read) is
 * unrepresentable in a sort/filter param by construction (QUERY-ALLOWLIST-KEYSTONE-001).
 *
 * <p>Identity columns are immutable ({@code updatable=false}); the entity is its own sole
 * mutator (package-private hooks only, no public setters); {@code @Version} guards concurrent
 * writes. The column is named {@code price_minor} (never {@code price}/{@code value}) — a
 * money amount in minor units (cents), and {@code value}/{@code order}/{@code limit} are SQL
 * reserved words this domain avoids.
 */
@AggregateRoot
@Entity
@Table(name = "catalog_items")
@Check(constraints = "price_minor >= 0")
public class CatalogItem {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CatalogItemStatus status;

    /** A money amount in MINOR units (cents) — never named `price` so the public/internal split is explicit. */
    @Column(name = "price_minor", nullable = false)
    private long priceMinor;

    /**
     * An internal-only field deliberately NOT in the sort/filter allowlist — the keystone target:
     * a client must not be able to sort/filter on (or order by) it. It is also {@code @JsonIgnore}
     * so it never reaches a DTO.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "internal_notes", length = 500)
    private String internalNotes;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CatalogItem() {}

    public CatalogItem(UUID id, String name, CatalogItemStatus status, long priceMinor,
                       String internalNotes, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.priceMinor = priceMinor;
        this.internalNotes = internalNotes;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public CatalogItemStatus getStatus() { return status; }
    public long getPriceMinor() { return priceMinor; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
