package com.ax.template.authblueprint.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * R81 — escape-hatch marker for stored-error columns whose sanitize
 * happens UPSTREAM of the entity boundary (typically in a service-layer
 * caller that passes the already-sanitized value into the entity setter
 * or constructor).
 *
 * <p>Anchors {@code practices/rules/server-side-stored-error-sanitize.md}
 * (R61) and is enforced by
 * {@code practices/evals/stored_error_column_sanitize_guard.sh} (43rd
 * hard guard). The guard scans every entity with a stored-error
 * {@code @Column(name = "last_error" | "error_message" | "failure_reason")}
 * declaration and requires either:
 *
 * <ol>
 *   <li>the same entity file contains a call to
 *       {@link AuditPiiHelper#sanitizeReason(String)} (the entity
 *       self-sanitizes — preferred for new code), OR</li>
 *   <li>the field is annotated {@code @PiiSanitized(reason = "...")} —
 *       this annotation, documenting that sanitization occurs in an
 *       upstream caller.</li>
 * </ol>
 *
 * <p>Using this annotation is a deliberate signal that the entity is
 * NOT the sanitize boundary; reviewers must verify the upstream call.
 *
 * <p>Source retention only — this annotation is a static-analysis
 * marker, never introspected at runtime.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface PiiSanitized {

    /**
     * Where the actual sanitize happens. Example:
     * {@code "ScheduledTaskService.runOne — sanitizeReason called before history.markFailure(msg)"}.
     */
    String reason();
}
