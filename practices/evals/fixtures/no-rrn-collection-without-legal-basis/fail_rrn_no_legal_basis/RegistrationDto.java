/**
 * FIXTURE: no-rrn-collection-without-legal-basis/fail_rrn_no_legal_basis
 * Demonstrates WRONG pattern: DTO with raw RRN field, no @LegalBasis annotation.
 * Rule: no-rrn-collection-without-legal-basis (Java)
 * Guard: grep -rn 'String.*rrn\|String.*주민' --include='*.java' | grep -v '@LegalBasis'
 * must find this file.
 *
 * Violates: 개인정보보호법 §24 — 고유식별정보의 처리 제한
 */
package com.example.fixture.no_rrn_collection;

// VIOLATION: DTO accepts raw RRN without @LegalBasis annotation
// This constitutes unauthorized collection under 개인정보보호법 §24
public record RegistrationDto(
    String name,
    String email,
    String rrn  // VIOLATION: 주민등록번호 field — should use CI from 본인인증 instead
) {}
