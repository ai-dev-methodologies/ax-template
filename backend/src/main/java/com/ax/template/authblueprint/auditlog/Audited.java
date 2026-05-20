package com.ax.template.authblueprint.auditlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method as audited.
 * <p>
 * Trace:
 * <ul>
 *   <li>AUDIT-RECORD-001 — entry-point for the aspect that records the audit log row</li>
 *   <li>blueprints/audit-log-manifest.yaml#aop_wiring</li>
 * </ul>
 *
 * <p>The intercepted method's resource ID is resolved by
 * {@link AuditLoggingAspect} via (in order): a parameter annotated with
 * {@link ResourceId}, an {@code id} field on the first argument, or
 * {@code "unknown"}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {

    /** Action verb — e.g. {@code "CREATE"}, {@code "UPDATE"}, {@code "DELETE"}. */
    String action();

    /** Resource type — e.g. {@code "payment"}, {@code "user"}. */
    String resourceType();
}
