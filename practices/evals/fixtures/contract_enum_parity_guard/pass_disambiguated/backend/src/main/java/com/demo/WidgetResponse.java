package com.demo;

/**
 * FIXTURE (P3-87) — the response type the Widget schema serializes.
 *
 * It names WidgetStatus and NOT WidgetPhase, which is what makes it able to
 * corroborate WHICH of the two same-set enums the `status` block binds. This is
 * the fact the manifest author does not write: the DTO on disk either mentions
 * this enum or it does not.
 */
public record WidgetResponse(String id, WidgetStatus status) {}
