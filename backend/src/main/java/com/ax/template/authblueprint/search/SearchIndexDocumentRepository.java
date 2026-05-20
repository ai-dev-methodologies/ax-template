package com.ax.template.authblueprint.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link SearchIndexDocument}.
 * <p>
 * SEARCH-AUTHZ-002 — every read path filters on {@code tenantId} so cross-tenant
 * leakage is impossible at the SQL layer.
 */
public interface SearchIndexDocumentRepository extends JpaRepository<SearchIndexDocument, UUID> {

    Optional<SearchIndexDocument> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * SEARCH-QUERY-001/002 — case-insensitive substring search scoped by
     * tenantId. Korean substrings work because {@code LIKE '%강남%'} is a byte
     * (UTF-8) substring match — no tokenizer required for catalog default
     * (PostgreSQL FTS / Meilisearch can be substituted via {@link SearchBackend}).
     */
    @Query("select d from SearchIndexDocument d " +
        "where d.tenantId = :tenantId " +
        "and lower(d.content) like lower(concat('%', :q, '%')) " +
        "and (:domain is null or d.domain = :domain) " +
        "order by d.indexedAt desc")
    Page<SearchIndexDocument> searchByContent(
        @Param("tenantId") String tenantId,
        @Param("q") String q,
        @Param("domain") String domain,
        Pageable pageable);
}
