package com.ax.template.authblueprint.featureflags;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * Request and response payloads for the feature-flags admin and public APIs.
 * <p>
 * Trace:
 * <ul>
 *   <li>FF-VALID-001 — name @Pattern.</li>
 *   <li>FF-VALID-002 — description @Size(max = 500).</li>
 *   <li>FF-EVAL-001/002 — {@link Evaluation} response shape {@code {active: boolean}}.</li>
 *   <li>blueprints/feature-flags-manifest.yaml#crud — CRUD response shape.</li>
 * </ul>
 */
public final class FeatureFlagDto {

    private FeatureFlagDto() {}

    /**
     * FF-VALID-001 — {@code ^[a-z][a-z0-9-]{1,62}$} (2..63 chars, lowercase + hyphen,
     * must start with a letter).
     */
    public static final String NAME_PATTERN = "^[a-z][a-z0-9-]{1,62}$";

    /** Public eval response shape (FF-EVAL-001/002). */
    public record Evaluation(boolean active) {}

    /** Create request body (POST /api/v1/admin/feature-flags). */
    public record CreateRequest(
        @NotBlank
        @Pattern(regexp = NAME_PATTERN,
                 message = "name must match " + NAME_PATTERN)
        String name,

        @NotNull
        Boolean enabled,

        @Size(max = 500, message = "description must be <= 500 characters")
        String description
    ) {}

    /** Patch request body (PATCH /api/v1/admin/feature-flags/{name}). */
    public record UpdateRequest(
        Boolean enabled,

        @Size(max = 500, message = "description must be <= 500 characters")
        String description
    ) {}

    /** Admin-facing flag detail. */
    public record FlagResponse(
        String name,
        boolean enabled,
        String description,
        Instant createdAt,
        Instant updatedAt
    ) {
        public static FlagResponse from(FeatureFlag flag) {
            return new FlagResponse(
                flag.getName(),
                flag.isEnabled(),
                flag.getDescription(),
                flag.getCreatedAt(),
                flag.getUpdatedAt());
        }
    }

    /** Page envelope for {@code GET /api/v1/admin/feature-flags}. */
    public record FlagPage(
        List<FlagResponse> content,
        int page,
        int size,
        long totalElements
    ) {}
}
