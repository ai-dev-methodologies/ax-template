package com.ax.template.authblueprint.activityfeed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.ax.template.authblueprint.activityfeed.ActivityDtos.ActivityEventResponse;
import com.ax.template.authblueprint.activityfeed.ActivityDtos.ActivityFeedResponse;
import com.ax.template.authblueprint.activityfeed.ActivityDtos.MarkAllReadResponse;
import com.ax.template.authblueprint.activityfeed.ActivityDtos.PublishActivityRequest;

@Service
public class ActivityService {

    private final ActivityEventRepository eventRepository;
    private final ActivityReadRepository readRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ActivityService(ActivityEventRepository eventRepository,
                           ActivityReadRepository readRepository,
                           ObjectMapper objectMapper,
                           Clock clock) {
        this.eventRepository = eventRepository;
        this.readRepository = readRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public PublishResult publish(String actorUserId, PublishActivityRequest request) {
        // ACT-PUBLISH-003 — idempotent via (actor, idempotencyKey).
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            var existing = eventRepository.findByActorUserIdAndIdempotencyKey(
                actorUserId, request.idempotencyKey());
            if (existing.isPresent()) {
                return new PublishResult(toResponse(existing.get(), actorUserId), false);
            }
        }
        Set<String> audience = (request.audienceUserIds() == null)
            ? new HashSet<>()
            : new HashSet<>(request.audienceUserIds());

        ActivityEvent event = ActivityEvent.builder()
            .actorUserId(actorUserId)
            .verb(request.verb())
            .objectType(request.objectType())
            .objectId(request.objectId())
            .subjectType(request.subjectType())
            .subjectId(request.subjectId())
            .metadataJson(serializeMetadata(request.metadata()))
            .idempotencyKey(request.idempotencyKey())
            .audienceUserIds(audience)
            .createdAt(Instant.now(clock))
            .build();
        ActivityEvent saved = eventRepository.save(event);
        return new PublishResult(toResponse(saved, actorUserId), true);
    }

    @Transactional(readOnly = true)
    public ActivityFeedResponse list(String userId, int page, int size, boolean unreadOnly) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        int clampedPage = Math.max(page, 0);
        PageRequest pageReq = PageRequest.of(clampedPage, clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ActivityEvent> events = unreadOnly
            ? eventRepository.findUnreadVisibleTo(userId, pageReq)
            : eventRepository.findVisibleTo(userId, pageReq);
        List<ActivityEventResponse> items = events.getContent().stream()
            .map(e -> toResponse(e, userId))
            .toList();
        return new ActivityFeedResponse(items, clampedPage, clampedSize, events.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ActivityEventResponse get(String userId, UUID id) {
        ActivityEvent e = eventRepository.findVisibleSingle(id, userId)
            .orElseThrow(() -> new ActivityNotFoundException(id));
        return toResponse(e, userId);
    }

    @Transactional
    public void markRead(String userId, UUID eventId) {
        // ACT-MARK-001 — visibility check first.
        ActivityEvent event = eventRepository.findVisibleSingle(eventId, userId)
            .orElseThrow(() -> new ActivityNotFoundException(eventId));
        readRepository.findByEventIdAndUserId(event.getId(), userId)
            .orElseGet(() -> readRepository.save(new ActivityRead(event.getId(), userId, Instant.now(clock))));
    }

    @Transactional
    public MarkAllReadResponse markAllRead(String userId) {
        List<UUID> unread = readRepository.findUnreadEventIdsForUser(userId);
        Instant now = Instant.now(clock);
        for (UUID eventId : unread) {
            readRepository.save(new ActivityRead(eventId, userId, now));
        }
        return new MarkAllReadResponse(unread.size());
    }

    private ActivityEventResponse toResponse(ActivityEvent event, String userId) {
        Instant readAt = readRepository.findByEventIdAndUserId(event.getId(), userId)
            .map(ActivityRead::getReadAt)
            .orElse(null);
        return ActivityEventResponse.from(event, readAt, objectMapper);
    }

    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("metadata is not JSON-serializable", ex);
        }
    }

    public record PublishResult(ActivityEventResponse response, boolean created) {}
}
