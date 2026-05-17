/**
 * @ax-template-meta
 * template_id: backend/dto/RequestDto
 * layer: backend-cross-cutting
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: external_canonical
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records (Final, Java 16)"
 *     url: "https://openjdk.org/jeps/395"
 *   - source_type: external
 *     citation: "OWASP Mass Assignment Cheat Sheet"
 *     url: "https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   This file is a REFERENCE PATTERN, not an actual DTO.
 *   Copy and adapt the record declaration pattern for your domain DTOs.
 *
 *   Naming convention:
 *     <Entity><Action>Request  — e.g. CreateItemRequest, UpdateItemRequest
 *     <Entity>Response         — e.g. ItemResponse, ItemDetailResponse
 */
package com.example.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reference pattern for request DTOs as Java records.
 *
 * <p>Records enforce immutability, provide {@code equals}/{@code hashCode}/{@code toString}
 * automatically, and declare the field contract on a single line.
 *
 * <p>OWASP Mass Assignment guidance: declare ONLY the fields that the client is
 * allowed to set. Never accept the entity class itself as a request body.
 *
 * <p>Usage pattern — replace with your domain fields:
 * <pre>{@code
 * public record CreateItemRequest(
 *     @NotBlank @Size(max = 255) String name,
 *     @Size(max = 2000) String description
 * ) {}
 * }</pre>
 *
 * <p>Rule reference: PRACTICES-LANG-001 (records for DTOs).
 */
public record RequestDto(
        /**
         * Example field — replace with domain-specific fields.
         * Every *Request record should validate inputs with Bean Validation annotations.
         */
        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 2000)
        String description
) {
    // Records are final and immutable by design.
    // Add custom compact constructor for cross-field validation if needed:
    //
    // public RequestDto {
    //     if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
    // }
}
