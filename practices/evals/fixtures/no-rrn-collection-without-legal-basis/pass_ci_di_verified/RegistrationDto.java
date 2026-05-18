/**
 * FIXTURE: no-rrn-collection-without-legal-basis/pass_ci_di_verified
 * Demonstrates CORRECT pattern: DTO uses CI (Connecting Information) from KISA 본인인증.
 * Rule: no-rrn-collection-without-legal-basis (Java)
 * Guard should NOT trigger on this file.
 *
 * The field 'ci' is Connecting Information — NOT the RRN. The rule matcher excludes:
 * ci, di, verifiedIdentityNumber, connectingInfo, duplicateInfo, externalId.
 */
package com.example.fixture.no_rrn_collection;

// CORRECT: DTO uses CI token from KISA 본인인증 callback — no RRN collected
public record RegistrationDto(
    String name,
    String email,
    String ci   // CORRECT: Connecting Information from 본인인증; cross-service unique, NOT RRN
) {}
