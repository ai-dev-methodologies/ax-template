package fixtures.dto_records.fail_class_instead_of_record;

// FIXTURE: fail_class_instead_of_record
// EXPECTED_VIOLATION: DTO_IS_CLASS_INSTEAD_OF_RECORD
// RULE: lang-records-for-dtos.md (PRACTICES-LANG-001)
//
// This DTO violates the rule: it uses a mutable class with private fields and
// getters/setters instead of a Java record. Records enforce immutability,
// provide equals/hashCode/toString automatically, and communicate the DTO
// contract in a single line.
// A guard asserting *Request / *Response classes are records will FAIL
// against this fixture.

import jakarta.validation.constraints.NotBlank;

// VIOLATION: class with mutable state instead of record
public class ItemRequest {

    @NotBlank
    private String name;
    private String description;

    public ItemRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
