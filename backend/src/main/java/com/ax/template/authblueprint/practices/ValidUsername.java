package com.ax.template.authblueprint.practices;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Jakarta Bean Validation constraint for PRACTICES-VAL-003.
 * Accepts lowercase ASCII alphanumeric + underscore, length 3-20.
 * The validator implementation is {@link ValidUsernameValidator}.
 */
@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {

    String message() default "must be 3-20 lowercase letters, digits, or underscore";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
