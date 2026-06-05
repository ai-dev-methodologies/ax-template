package com.ax.template.authblueprint.requestvalidation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * VALIDATION-CONSTRAINT-001 cross-field validator for {@link DateRange}: {@code startDate}
 * MUST be strictly before {@code endDate}. Null components are left to the field-level
 * {@code @NotNull} constraints (this validator only judges the relationship), and on failure
 * it re-targets the violation to the {@code endDate} node so the reported pointer is
 * {@code /endDate} rather than the whole object.
 *
 * <p>Spec: specs/request-validation-l0.yaml#VALIDATION-CONSTRAINT-001.
 */
public class DateRangeValidator implements ConstraintValidator<DateRange, CreateOrderRequest> {

    @Override
    public boolean isValid(CreateOrderRequest value, ConstraintValidatorContext context) {
        if (value == null || value.startDate() == null || value.endDate() == null) {
            return true; // @NotNull on the components handles the null case
        }
        if (value.startDate().isBefore(value.endDate())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endDate")
                .addConstraintViolation();
        return false;
    }
}
