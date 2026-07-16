package com.ax.template.authblueprint.activityfeed;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ActivityDtos {

    private ActivityDtos() {}

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public record PublishActivityRequest(
        @NotBlank @Size(max = 64) String verb,
        @NotBlank @Size(max = 64) String objectType,
        @NotBlank @Size(max = 255) String objectId,
        @Size(max = 64) String subjectType,
        @Size(max = 255) String subjectId,
        @Size(max = 100) List<String> audienceUserIds,
        @Size(max = 50) Map<String, Object> metadata,
        @Size(max = 128) String idempotencyKey
    ) {}

    /**
     * R52 — backend-contract wave 1 (closes R44 P2-F7 audience peer leak):
     * audienceUserIds was previously serialized in the response payload,
     * exposing every recipient of the event to every other recipient via
     * DevTools Network tab. The catalog now returns only a boolean
     * indicating whether the calling user is in the audience — the actual
     * id set stays server-internal.
     */
    public record ActivityEventResponse(
        UUID id,
        String actorUserId,
        String verb,
        String objectType,
        String objectId,
        String subjectType,
        String subjectId,
        Map<String, Object> metadata,
        boolean youAreInAudience,
        Instant createdAt,
        Instant readAt
    ) {

        public static ActivityEventResponse from(
                ActivityEvent e, String callerUserId, Instant readAt, ObjectMapper mapper) {
            Map<String, Object> meta = parseMeta(e.getMetadataJson(), mapper);
            Set<String> audience = e.getAudienceUserIds();
            boolean inAudience = callerUserId != null
                && audience != null
                && audience.contains(callerUserId);
            return new ActivityEventResponse(
                e.getId(),
                e.getActorUserId(),
                e.getVerb(),
                e.getObjectType(),
                e.getObjectId(),
                e.getSubjectType(),
                e.getSubjectId(),
                meta,
                inAudience,
                e.getCreatedAt(),
                readAt
            );
        }

        private static Map<String, Object> parseMeta(String json, ObjectMapper mapper) {
            if (json == null || json.isBlank()) return Map.of();
            try {
                return mapper.readValue(json, MAP_TYPE);
            } catch (JacksonException ex) {
                return Map.of();
            }
        }
    }

    public record ActivityFeedResponse(
        List<ActivityEventResponse> items,
        int page,
        int size,
        long totalElements
    ) {}

    public record MarkAllReadResponse(long markedCount) {}
}
