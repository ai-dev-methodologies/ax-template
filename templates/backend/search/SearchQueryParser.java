/**
 * @ax-template-meta
 * template_id: backend/search/SearchQueryParser
 * layer: backend-domain
 * domain: search
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: upstream_id
 *     upstream_id: postgresql-fts-2026-05
 *     section: "Core Types"
 *     quote: "tsvector is a sorted list of distinct lexemes; tsquery is a normalized query with lexemes connected by Boolean operators."
 *   - source_type: external
 *     citation: "PostgreSQL 16 Docs — to_tsquery: input must be a valid tsquery; special characters (&, |, !, :*) must be escaped from untrusted user input to prevent query injection"
 *     url: "https://www.postgresql.org/docs/16/textsearch-controls.html#TEXTSEARCH-PARSING-QUERIES"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   SearchQueryParser.toTsQuery() sanitizes user input for safe PostgreSQL to_tsquery() calls.
 *   Use plainto_tsquery() for simpler (implicit AND) semantics; to_tsquery() for phrase/boolean.
 */
package com.example.app.search;

import org.springframework.stereotype.Component;

/**
 * Sanitizes and transforms user search queries into safe PostgreSQL {@code tsquery} expressions.
 *
 * <h3>Security</h3>
 * Raw user input must not be interpolated directly into {@code to_tsquery()} calls.
 * Special tsquery operators ({@code & | ! :*}) are stripped or escaped before the
 * query is passed to the database as a parameterized value.
 *
 * <h3>Korean (한글) handling</h3>
 * The 'simple' dictionary treats each word-boundary token as a lexeme.
 * Korean text without spaces (e.g. "강남결제") is returned as a single lexeme.
 * Queries with spaces (e.g. "강남 결제") are tokenized into two lexemes joined with {@code &}.
 *
 * <h3>Usage</h3>
 * <pre>
 * String tsQuery = queryParser.toTsQuery("강남 결제");
 * // → "강남 & 결제"  (passed as a bind parameter to to_tsquery('simple', :query))
 * </pre>
 */
@Component
public class SearchQueryParser {

    /**
     * Converts a raw user query string into a safe PostgreSQL tsquery expression.
     *
     * <p>Rules:
     * <ol>
     *   <li>Trim leading/trailing whitespace.
     *   <li>Strip tsquery special characters: {@code & | ! ( ) : *}
     *   <li>Collapse multiple whitespace runs to single space.
     *   <li>Join remaining tokens with {@code &} (implicit AND semantics).
     * </ol>
     *
     * @param rawQuery the raw user-supplied search string
     * @return a safe tsquery string suitable for use as a parameterized bind value
     *         in {@code to_tsquery('simple', :query)}
     * @throws IllegalArgumentException if rawQuery is blank after sanitization
     */
    public String toTsQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }

        // Strip tsquery operator characters to prevent query injection
        String sanitized = rawQuery
            .replaceAll("[&|!():*]", " ")
            .trim()
            .replaceAll("\\s+", " ");

        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Search query contained only operator characters");
        }

        // Join tokens with & for implicit AND semantics
        String[] tokens = sanitized.split(" ");
        return String.join(" & ", tokens);
    }

    /**
     * Returns the query as a plain string for use with {@code plainto_tsquery()}.
     *
     * <p>{@code plainto_tsquery} handles whitespace-separated token AND logic natively;
     * no special characters needed. Use this when the UI sends simple keyword strings.
     *
     * @param rawQuery the raw user-supplied search string
     * @return sanitized query safe as a bind parameter to {@code plainto_tsquery('simple', :query)}
     */
    public String toPlainTsQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        // plainto_tsquery handles the text as-is; only strip null bytes and control chars
        return rawQuery.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
    }
}
