---
title: Encode domain-specific shape in @Constraint + ConstraintValidator
impact: MEDIUM
impactDescription: "Keeps the rule on the field, not in service code; composes with built-ins"
tags:
  - validation
  - custom-constraint
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-003"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-003
upstream:
  - "https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/"
evidence:
  - upstream_id: hibernate-validator
    section: "Hibernate Validator — defining a custom ConstraintValidator"
    quote: "ConstraintValidator"
  - source_type: external
    citation: "Hibernate Validator Reference — Custom constraints"
    url: "https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/#section-creating-custom-constraint"
---

## Encode domain-specific shape in @Constraint + ConstraintValidator

**Impact: MEDIUM — Keeps the rule on the field, not in service code; composes with built-ins**

Built-in constraints cover the common 80%, but every project has shapes the spec does not — `@ValidUsername`, `@ValidIsbn`, `@ValidUkPostcode`. Re-implementing them inside services (`if (!username.matches(pattern)) throw ...`) duplicates the rule at every call site. Defining a `@Constraint` annotation with a `ConstraintValidator` makes the rule field-local, composable with built-ins, and consumable everywhere a validation annotation is — DTOs, method parameters, record components.

**Incorrect — imperative regex check inside the service:**

```java
public User register(String username, ...) {
    if (!username.matches("^[a-z0-9_]{3,20}$")) {
        throw new IllegalArgumentException("bad username");
    }
    ...
}
```

**Correct — custom @ValidUsername annotation + ConstraintValidator:**

```java
@Documented
@Constraint(validatedBy = ValidUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUsername {
    String message() default "must be 3-20 lowercase letters, digits, or underscore";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class ValidUsernameValidator implements ConstraintValidator<ValidUsername, String> {
    private static final Pattern P = Pattern.compile("^[a-z0-9_]{3,20}$");
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        return value == null || P.matcher(value).matches();
    }
}
```

Verification: `./gradlew testPractices --tests "*CustomConstraint*"` asserts invalid usernames (`BAD-USERNAME`, `ab`) are rejected with `errors.field` containing `username`, and that a valid username (`bob_1`) is accepted.

Reference: [Hibernate Validator — Creating custom constraints](https://docs.jboss.org/hibernate/validator/8.0/reference/en-US/html_single/#section-creating-custom-constraint)
