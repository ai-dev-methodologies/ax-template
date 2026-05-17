/**
 * @ax-template-meta
 * template_id: backend/dto/ResponseDto
 * layer: backend-cross-cutting
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records (Final, Java 16)"
 *     url: "https://openjdk.org/jeps/395"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   This file is a REFERENCE PATTERN, not an actual DTO.
 *   Copy and adapt the record declaration pattern for your domain response DTOs.
 *
 *   Naming convention:
 *     <Entity>Response         — summary / list item
 *     <Entity>DetailResponse   — expanded / single-entity detail
 */
package com.example.app.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Reference pattern for response DTOs as Java records.
 *
 * <p>Records provide immutability and a clean field contract.
 * Never expose entity classes directly — always project to a record DTO.
 * This prevents accidental exposure of sensitive fields and decouples the
 * API contract from the persistence model.
 *
 * <p>Usage pattern — replace with your domain fields:
 * <pre>{@code
 * public record ItemResponse(
 *     UUID id,
 *     String name,
 *     String description,
 *     Instant createdAt
 * ) {}
 * }</pre>
 *
 * <p>Rule reference: PRACTICES-LANG-001 (records for DTOs).
 */
public record ResponseDto(
        /**
         * Entity identifier — prefer UUID over sequential Long for new APIs
         * to prevent enumeration attacks.
         */
        UUID id,

        /** Example field — replace with domain-specific fields. */
        String name,

        /** Audit timestamp. */
        Instant createdAt
) {}
