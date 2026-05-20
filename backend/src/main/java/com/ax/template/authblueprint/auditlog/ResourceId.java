package com.ax.template.authblueprint.auditlog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method parameter whose value is the resource ID for the audit log entry.
 * <p>
 * Trace: blueprints/audit-log-manifest.yaml#aop_wiring.resource_id_extraction.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ResourceId {
}
