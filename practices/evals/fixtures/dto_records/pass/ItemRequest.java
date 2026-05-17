package fixtures.dto_records.pass;

// FIXTURE: pass
// PATTERN: request DTO as a Java record — PASSES PRACTICES-LANG-001

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// CORRECT: Java record — immutable, single-line field contract, Bean Validation annotations
public record ItemRequest(
        @NotBlank @Size(max = 255) String name,
        @Size(max = 2000) String description
) {}
