package com.ax.template.authblueprint.tagcategorization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTOs for the tag-categorization domain — kept in a single file to avoid file sprawl. */
public final class TagDtos {

    private TagDtos() {}

    public record CreateTagRequest(
        @NotBlank @Size(max = 64) String name,
        UUID parentTagId,
        @Size(max = 16) String color
    ) {}

    public record UpdateTagRequest(
        @Size(max = 64) String name,
        @Size(max = 16) String color
    ) {}

    public record AttachTagRequest(
        @NotBlank @Size(max = 64) String entityType,
        @NotBlank @Size(max = 255) String entityId
    ) {}

    public record TagResponse(
        UUID id,
        String name,
        String slug,
        UUID parentTagId,
        String color,
        Instant createdAt,
        String createdByUserId
    ) {
        public static TagResponse from(Tag t) {
            return new TagResponse(
                t.getId(),
                t.getName(),
                t.getSlug(),
                t.getParentTagId(),
                t.getColor(),
                t.getCreatedAt(),
                t.getCreatedByUserId()
            );
        }
    }

    public record TagListResponse(List<TagResponse> items, long totalElements) {}

    public record TagAttachmentResponse(
        UUID id,
        UUID tagId,
        String entityType,
        String entityId,
        Instant attachedAt,
        String attachedByUserId
    ) {
        public static TagAttachmentResponse from(TagAttachment a) {
            return new TagAttachmentResponse(
                a.getId(),
                a.getTagId(),
                a.getEntityType(),
                a.getEntityId(),
                a.getAttachedAt(),
                a.getAttachedByUserId()
            );
        }
    }
}
