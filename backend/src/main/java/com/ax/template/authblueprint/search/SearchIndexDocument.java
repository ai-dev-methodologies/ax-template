package com.ax.template.authblueprint.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Search index document — keyed by ({@code id}, {@code tenantId}).
 * <p>
 * Trace:
 * <ul>
 *   <li>SEARCH-AUTHZ-002 — every read filters on {@code tenantId = caller}</li>
 *   <li>SEARCH-INDEX-001 — persisted via {@link SearchIndexService#index(String, String, String, String, String)}</li>
 *   <li>SEARCH-INDEX-002 — removed via {@link SearchIndexService#delete(UUID, String)}</li>
 *   <li>SEARCH-QUERY-001/002 — content is the searchable substring</li>
 * </ul>
 * Manifest: {@code blueprints/search-manifest.yaml}.
 */
@AggregateRoot
@Entity
@Table(
    name = "search_index_documents",
    indexes = {
        @Index(name = "ix_search_tenant_domain",
               columnList = "tenant_id,domain"),
        @Index(name = "ix_search_tenant_created",
               columnList = "tenant_id,indexed_at")
    }
)
public class SearchIndexDocument {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Caller-scoped tenant — equals {@code Authentication#getName()} for the local impl. */
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    /** Optional logical domain (e.g. {@code payment}, {@code notification}). */
    @Column(name = "domain", length = 64)
    private String domain;

    /** Searchable body text — what the {@code LIKE} / FTS query targets. */
    @Column(name = "content", nullable = false, length = 4000)
    private String content;

    /** Free-form JSON-encoded metadata; opaque to the search engine. */
    @Column(name = "metadata", length = 4000)
    private String metadata;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt;

    /** Required by JPA. */
    protected SearchIndexDocument() {}

    private SearchIndexDocument(Builder b) {
        this.id = (b.id != null) ? b.id : UUID.randomUUID();
        this.tenantId = b.tenantId;
        this.domain = b.domain;
        this.content = b.content;
        this.metadata = b.metadata;
        this.indexedAt = (b.indexedAt != null) ? b.indexedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public String getTenantId() { return tenantId; }
    public String getDomain() { return domain; }
    public String getContent() { return content; }
    public String getMetadata() { return metadata; }
    public Instant getIndexedAt() { return indexedAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String tenantId;
        private String domain;
        private String content;
        private String metadata;
        private Instant indexedAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder tenantId(String v) { this.tenantId = v; return this; }
        public Builder domain(String v) { this.domain = v; return this; }
        public Builder content(String v) { this.content = v; return this; }
        public Builder metadata(String v) { this.metadata = v; return this; }
        public Builder indexedAt(Instant v) { this.indexedAt = v; return this; }

        public SearchIndexDocument build() { return new SearchIndexDocument(this); }
    }
}
