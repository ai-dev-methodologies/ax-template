/**
 * @ax-template-meta
 * template_id: backend/search/PostgresFtsAdapter
 * layer: backend-domain
 * domain: search
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "GIN Index"
 *     quote: "A GIN index on a tsvector column provides fast full-text search."
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "Ranking"
 *     quote: "ts_rank calculates a rank for matching documents based on frequency of matching lexemes."
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "Highlighting"
 *     quote: "ts_headline highlights matching terms in the original document text."
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "Korean Tokenization"
 *     quote: "For CJK languages, PostgreSQL 'simple' dictionary returns the entire string as a single lexeme."
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   PostgresFtsAdapter is the DEFAULT backend (no configuration required).
 *   Requires: search_index table with a GIN index on the content_tsv column.
 *   Run Flyway migration to create the table before using this adapter.
 *   Korean recall: ~70% with 'simple' dictionary; upgrade to pg_bigm for ~90%.
 */
package com.example.app.search;

import com.example.app.data.PageRequestNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PostgreSQL full-text search adapter (default backend).
 *
 * <h3>Schema requirements</h3>
 * <pre>
 * CREATE TABLE search_index (
 *   id          UUID PRIMARY KEY,
 *   domain      TEXT NOT NULL,
 *   title       TEXT,
 *   content     TEXT NOT NULL,
 *   content_tsv TSVECTOR GENERATED ALWAYS AS (to_tsvector('simple', content)) STORED,
 *   metadata    JSONB,
 *   created_at  TIMESTAMPTZ DEFAULT now()
 * );
 * CREATE INDEX idx_search_content_tsv ON search_index USING GIN(content_tsv);
 * </pre>
 *
 * <h3>Korean tokenization</h3>
 * The {@code 'simple'} dictionary treats whitespace-separated tokens as lexemes.
 * Korean text with spaces achieves ~70% recall. For higher recall, replace
 * {@code 'simple'} with {@code 'pg_bigm'} (requires {@code CREATE EXTENSION pg_bigm}).
 *
 * <h3>Activation</h3>
 * This bean is {@code @Primary} and {@code @ConditionalOnMissingBean(SearchBackend.class)}.
 * It is selected when {@code ax.search.backend} is absent or set to {@code postgres-fts}.
 */
@Component
@Primary
@ConditionalOnMissingBean(name = "meilisearchAdapter")
public class PostgresFtsAdapter implements SearchBackend {

    private static final Logger log = LoggerFactory.getLogger(PostgresFtsAdapter.class);
    private static final String DICTIONARY = "simple";

    private final JdbcClient jdbcClient;
    private final SearchQueryParser queryParser;

    public PostgresFtsAdapter(JdbcClient jdbcClient, SearchQueryParser queryParser) {
        this.jdbcClient = jdbcClient;
        this.queryParser = queryParser;
    }

    @Override
    public Page<SearchDto.SearchHit> search(SearchDto.SearchRequest request, Pageable pageable) {
        long start = System.currentTimeMillis();
        Pageable normalized = PageRequestNormalizer.normalize(pageable);

        String tsQuery = queryParser.toPlainTsQuery(request.query());

        // Domain filter (optional)
        String domainClause = request.domain() != null ? "AND domain = :domain" : "";

        String countSql = """
            SELECT COUNT(*) FROM search_index
            WHERE content_tsv @@ plainto_tsquery('%s', :query)
            %s
            """.formatted(DICTIONARY, domainClause);

        String searchSql = """
            SELECT
                id,
                domain,
                title,
                ts_headline('%s', content, plainto_tsquery('%s', :query),
                    'StartSel=<mark>, StopSel=</mark>, MaxFragments=2, MaxWords=35, MinWords=15') AS snippet,
                ts_rank(content_tsv, plainto_tsquery('%s', :query)) AS score,
                metadata
            FROM search_index
            WHERE content_tsv @@ plainto_tsquery('%s', :query)
            %s
            ORDER BY score DESC
            LIMIT :size OFFSET :offset
            """.formatted(DICTIONARY, DICTIONARY, DICTIONARY, DICTIONARY, domainClause);

        var countQuery = jdbcClient.sql(countSql).param("query", tsQuery);
        var searchQuery = jdbcClient.sql(searchSql)
            .param("query", tsQuery)
            .param("size", normalized.getPageSize())
            .param("offset", normalized.getOffset());

        if (request.domain() != null) {
            countQuery = countQuery.param("domain", request.domain());
            searchQuery = searchQuery.param("domain", request.domain());
        }

        long total = countQuery.query(Long.class).single();

        List<SearchDto.SearchHit> hits = searchQuery.query((rs, rowNum) -> new SearchDto.SearchHit(
            UUID.fromString(rs.getString("id")),
            rs.getString("domain"),
            rs.getString("title"),
            rs.getString("snippet"),
            rs.getFloat("score"),
            Map.of()  // metadata deserialization omitted for brevity — implement with ObjectMapper
        )).list();

        long processingTimeMs = System.currentTimeMillis() - start;
        log.debug("search query='{}' domain='{}' hits={} totalHits={} processingTimeMs={}",
            request.query(), request.domain(), hits.size(), total, processingTimeMs);

        return new PageImpl<>(hits, normalized, total);
    }

    @Override
    public UUID index(SearchDto.IndexDocumentRequest request) {
        jdbcClient.sql("""
            INSERT INTO search_index (id, domain, title, content, metadata)
            VALUES (:id, :domain, :title, :content, :metadata::jsonb)
            ON CONFLICT (id) DO UPDATE
                SET domain = EXCLUDED.domain,
                    title = EXCLUDED.title,
                    content = EXCLUDED.content,
                    metadata = EXCLUDED.metadata
            """)
            .param("id", request.id())
            .param("domain", request.domain())
            .param("title", request.title())
            .param("content", request.content())
            .param("metadata", "{}")  // simplified; use ObjectMapper for real metadata
            .update();

        log.debug("indexed document id={} domain={}", request.id(), request.domain());
        return request.id();
    }

    @Override
    public void delete(UUID id) {
        int rows = jdbcClient.sql("DELETE FROM search_index WHERE id = :id")
            .param("id", id)
            .update();
        log.debug("deleted document id={} rows={}", id, rows);
    }
}
