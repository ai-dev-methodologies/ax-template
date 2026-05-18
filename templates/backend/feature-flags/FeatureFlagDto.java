/**
 * @ax-template-meta
 * template_id: backend/feature-flags/FeatureFlagDto
 * layer: backend-domain
 * domain: feature-flags
 * anchors_rule: lang-records-for-dtos.md (PRACTICES-LANG-001)
 * provenance_class: internal_design
 * evidence:
 *   - source_type: external
 *     citation: "JEP 395 — Records (final, Java 16+)"
 *     url: "https://openjdk.org/jeps/395"
 * usage: |
 *   Replace 'com.example.app' with your base package.
 *   All DTOs are Java records — immutable, no boilerplate.
 *   UpdateRequest uses nullable boxed Boolean/String to distinguish absent from false/null.
 */
package com.example.app.featureflags;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * DTO records for the feature-flags domain.
 *
 * <p>spec_ref: specs/feature-flags-l0.yaml (FF-VALID-001, FF-VALID-002)
 */
public final class FeatureFlagDto {

    private FeatureFlagDto() {}

    /**
     * Request body for POST /api/v1/admin/feature-flags.
     * name must match ^[a-z][a-z0-9-]{1,62}$ (FF-VALID-001).
     */
    public record CreateRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9-]{1,62}$",
                 message = "Flag name must be lowercase, hyphen-separated, 2–63 chars")
        String name,

        @NotNull
        Boolean enabled,

        @Size(max = 500)
        String description
    ) {}

    /**
     * Request body for PATCH /api/v1/admin/feature-flags/{name}.
     * Nullable fields: only present fields are applied.
     */
    public record UpdateRequest(
        Boolean enabled,

        @Size(max = 500)
        String description
    ) {}

    /**
     * Response body for all flag-returning endpoints.
     */
    public record FlagResponse(
        String name,
        boolean enabled,
        String description,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static FlagResponse from(FeatureFlag f) {
            return new FlagResponse(
                    f.getName(),
                    f.isEnabled(),
                    f.getDescription(),
                    f.getCreatedAt(),
                    f.getUpdatedAt());
        }
    }

    /**
     * Response body for GET /api/v1/feature-flags/{name}/active.
     * Fail-closed: always present; active=false for unknown flags.
     */
    public record FlagActiveResponse(boolean active) {}
}
