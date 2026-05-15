---
title: Transport DTOs (*Request / *Response) must be Java records
impact: MEDIUM
impactDescription: "Immutability + equals/hashCode/toString + one-line field contract, zero boilerplate"
tags:
  - lang
  - records
  - dto
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-LANG-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-LANG-001
upstream:
  - "https://openjdk.org/jeps/395"
evidence:
  - upstream_id: jep-395-records
    section: "JEP 395 — Records (Final)"
    quote: "record"
  - source_type: external
    citation: "JEP 395 — Records (Final, Java 16)"
    url: "https://openjdk.org/jeps/395"
---

## Transport DTOs (*Request / *Response) must be Java records

**Impact: MEDIUM — Immutability + equals/hashCode/toString + one-line field contract, zero boilerplate**

A transport DTO has one job: carry a fixed set of fields across the wire. A `record` declares that in a single line and produces immutability, `equals` / `hashCode` / `toString`, and clean serialization for free. The classic 80-line `class` with private fields + 8 getters + setters + manual `equals` is pure boilerplate that survives only because IDE templates exist. Java has had records since 16; there is no longer a good reason for a hand-rolled transport class. (Domain entities with behavior or with mutable invariants are NOT the target of this rule — they may legitimately be classes.)

**Incorrect — hand-rolled DTO with mutable fields and boilerplate accessors:**

```java
public class UserCreateRequest {
    private String name;
    private String email;
    public UserCreateRequest() {}
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getEmail() { return email; }
    public void setEmail(String e) { this.email = e; }
    // equals, hashCode, toString — usually wrong or missing
}
```

**Correct — record:**

```java
public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 50) String name,
        @NotBlank @Email String email
) {}
```

Verification: `./gradlew testPractices --tests "*RecordsForDtos*"` runs an ArchUnit rule that picks every `*Request` and `*Response` class under `practices/` and asserts each is a record.

Reference: [JEP 395 — Records (Final)](https://openjdk.org/jeps/395)
