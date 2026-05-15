---
title: Bind HTTP payloads to a whitelist DTO, never directly to an entity
impact: HIGH
impactDescription: "Prevents privilege escalation via mass-assignment of protected entity fields"
tags:
  - validation
  - dto
  - mass-assignment
  - security
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-VAL-001"
verification:
  gradle_task: testPractices
  tag: PRACTICES-VAL-001
upstream:
  - "https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html"
evidence:
  - upstream_id: owasp-mass-assignment
    section: Mass Assignment definition and remediation
    quote: Mass Assignment - OWASP Cheat Sheet Series Skip to content OWASP Cheat Sheet Series Mass Assignment Initializing search OWASP/CheatSheetSeries OWASP Cheat Sheet Series OWASP/CheatSheetSeries Introduction Index Alphabetical Index ASVS Index
  - upstream_id: cwe-915
    section: CWE-915 — Improperly Controlled Modification of Dynamically-Determined Object Attributes
    quote: 'CWE - CWE-915: Improperly Controlled Modification of Dynamically-Determined Object Attributes (4.20) Common Weakness Enumeration A community-developed list of SW &amp; HW weaknesses that can become vulnerabilities Home &gt; CWE List &gt; CWE-915: Improper'
  - upstream_id: spring-mvc-modelattribute
    section: '@ModelAttribute / data binding'
    quote: sources Validation, Data Binding, and Type Conversion Validation Using Spring&#8217;s Validator Interface Data Binding Resolving Error Codes to Error Messages Spring Type Conversion Spring Field Formatting Configuring a Global Date and Time Format Java Bean Va
  - source_type: external
    citation: 'OWASP Mass Assignment Cheat Sheet'
    url: 'https://cheatsheetseries.owasp.org/cheatsheets/Mass_Assignment_Cheat_Sheet.html'
  - source_type: external
    citation: 'CWE-915: Improperly Controlled Modification of Dynamically-Determined Object Attributes'
    url: 'https://cwe.mitre.org/data/definitions/915.html'
---

## Bind HTTP payloads to a whitelist DTO, never directly to an entity

**Impact: HIGH — Prevents privilege escalation via mass-assignment of protected entity fields**

If a controller accepts `@RequestBody Entity entity`, every JSON field maps onto the entity by name. An attacker who adds `"role":"ADMIN"` to an otherwise-legitimate update body silently escalates privilege. The remedy: bind to a DTO whose fields enumerate only the user-controllable inputs. Protected fields (role, enabled, owner, internal IDs) are absent from the DTO and therefore cannot be set, regardless of what the attacker sends.

**Incorrect — direct entity binding lets the client set anything:**

```java
@PutMapping("/users/{id}")
public User update(@PathVariable Long id, @RequestBody User user) {
    user.setId(id);
    return userRepo.save(user); // attacker controls role, enabled, etc.
}
```

**Correct — bind to a whitelist DTO, then copy only safe fields:**

```java
public class UserUpdateDto {
    @NotBlank public String name;
    @Email public String email;
    // no role, no enabled — by design
}

@PutMapping("/users/{id}")
public User update(@PathVariable Long id, @RequestBody @Valid UserUpdateDto dto) {
    User u = userRepo.findById(id).orElseThrow();
    u.setName(dto.name);
    u.setEmail(dto.email);
    return userRepo.save(u);   // role / enabled keep server-controlled values
}
```

Verification: `./gradlew testPractices --tests "*MassAssignment*"` asserts that direct entity binding propagates the attacker's `role` field, while DTO-mediated binding preserves the server default `USER`.

Reference: [Spring MVC — @ModelAttribute method arguments](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/modelattrib-method-args.html)
