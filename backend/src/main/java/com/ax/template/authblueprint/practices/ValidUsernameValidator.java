package com.ax.template.authblueprint.practices;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * ConstraintValidator implementation for {@link ValidUsername}.
 * Null is treated as valid — the @NotBlank / @NotNull constraints are responsible for
 * rejecting nulls. This validator only checks shape when a value is present.
 */
public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || PATTERN.matcher(value).matches();
    }
}
