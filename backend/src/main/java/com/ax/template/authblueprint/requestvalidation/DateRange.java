package com.ax.template.authblueprint.requestvalidation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * VALIDATION-CONSTRAINT-001 — a CROSS-FIELD rule expressed as a class-level custom
 * constraint (NOT hand-coded inside the handler): {@code startDate} MUST be strictly before
 * {@code endDate}. The validator is {@link DateRangeValidator}; its stable machine code is
 * the constraint simple name {@code DateRange} (consumed by VALIDATION-ERROR-001).
 *
 * <p>Anchored to Jakarta Bean Validation 3.0 §3 (custom ConstraintValidator).
 * Spec: specs/request-validation-l0.yaml#VALIDATION-CONSTRAINT-001.
 */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateRange {

    String message() default "startDate must be before endDate";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
