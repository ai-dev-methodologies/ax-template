package com.ax.template.authblueprint.practices;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fixture DTO for PRACTICES-VAL-002 / VAL-003 / VAL-004.
 * All user-controllable fields are whitelisted on the record + constrained with Jakarta
 * Bean Validation annotations. The custom @ValidUsername drives PRACTICES-VAL-003;
 * @NotBlank/@Email/@Size drive PRACTICES-VAL-002.
 */
public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @NotBlank @Email String email,
        @ValidUsername String username
) {}
