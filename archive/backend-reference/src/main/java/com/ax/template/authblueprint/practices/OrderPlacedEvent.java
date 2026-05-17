package com.ax.template.authblueprint.practices;

import java.time.Instant;

public record OrderPlacedEvent(String orderId, long amountCents, Instant placedAt) {}
