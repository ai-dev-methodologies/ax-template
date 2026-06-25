package com.ax.template.authblueprint.identityclaim;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class IdentityClaimDtos {

    private IdentityClaimDtos() {}

    public record AddRecordRequest(
        @NotBlank String claimKey,
        @NotBlank String label
    ) {}

    public record ClaimRequest(
        @NotBlank String claimKey
    ) {}

    /** Number of records actually claimed in this invocation. */
    public record ClaimResult(int claimedCount) {}

    public record RecordResponse(UUID id, String claimKey, String ownerUserId, String label) {
        public static RecordResponse from(ClaimableRecord r) {
            return new RecordResponse(r.getId(), r.getClaimKey(), r.getOwnerUserId(), r.getLabel());
        }
    }
}
