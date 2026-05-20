package com.ax.template.authblueprint.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body for POST /api/notifications (admin-only).
 */
public record NotificationSendRequest(
    @NotNull String recipientUserId,
    @NotBlank @Size(max = 64) String type,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String body,
    @Size(max = 1024) String link
) {}
