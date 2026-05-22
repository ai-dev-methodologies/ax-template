package com.ax.template.authblueprint.activityfeed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        Map<String, Object> metadata,
        @Size(max = 128) String idempotencyKey
    ) {}

    public record ActivityEventResponse(
        UUID id,
        String actorUserId,
        String verb,
        String objectType,
        String objectId,
        String subjectType,
        String subjectId,
        Map<String, Object> metadata,
        Set<String> audienceUserIds,
        Instant createdAt,
        Instant readAt
    ) {

        public static ActivityEventResponse from(ActivityEvent e, Instant readAt, ObjectMapper mapper) {
            Map<String, Object> meta = parseMeta(e.getMetadataJson(), mapper);
            return new ActivityEventResponse(
                e.getId(),
                e.getActorUserId(),
                e.getVerb(),
                e.getObjectType(),
                e.getObjectId(),
                e.getSubjectType(),
                e.getSubjectId(),
                meta,
                e.getAudienceUserIds(),
                e.getCreatedAt(),
                readAt
            );
        }

        private static Map<String, Object> parseMeta(String json, ObjectMapper mapper) {
            if (json == null || json.isBlank()) return Map.of();
            try {
                return mapper.readValue(json, MAP_TYPE);
            } catch (JsonProcessingException ex) {
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
