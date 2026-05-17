package com.ax.template.authblueprint.crud;

import java.time.Instant;

public record ItemResponse(String id, String title, String description,
    String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {
    
    public static ItemResponse from(ItemEntity e) {
        return new ItemResponse(e.getId().toString(), e.getTitle(), e.getDescription(),
            e.getCreatedBy(), e.getCreatedAt(), e.getUpdatedBy(), e.getUpdatedAt());
    }
}
