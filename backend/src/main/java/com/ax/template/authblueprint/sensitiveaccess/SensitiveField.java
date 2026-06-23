package com.ax.template.authblueprint.sensitiveaccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * sensitive-read-audit-l0 — the GENERIC sensitive-data marker (SENSITIVE-READ-001).
 *
 * <p>Place {@code @SensitiveField} on the entity field / DTO record component / getter that
 * carries a governed sensitive datum (a PII identifier, a payment-card / bank-account number,
 * a KYC identifier, an HR compensation figure, a contact value, …). It is the domain-agnostic
 * generalization of {@code common/@Phi}: where {@code @Phi} keys ONLY clinical PHI (and wires
 * the HIPAA audit-on-read guard), {@code @SensitiveField} attaches the SAME audit-on-read
 * obligation to ANY sensitive field — the moment a fork-receiver tags a field, reading its raw
 * value through the service becomes an audited event that MUST append an immutable
 * {@link SensitiveAccessLog} row (who / when / what / why) before the value is returned.
 *
 * <h2>Why a tag and not a name heuristic</h2>
 * Mirrors {@code common/@Phi}'s rationale: keying on an explicit, intent-bearing tag (never on a
 * field NAME like {@code getSsn} / {@code getCardNumber}) keeps the audit obligation precise and
 * forward-enforcing — it fires exactly when a real sensitive field is tagged, with zero false
 * positives on a tree that tags nothing.
 *
 * <p>NIST SP 800-53 Rev 5 AU-3 (Content of Audit Records) is the standard the tag serves: an
 * access to the tagged value must produce a record establishing what / when / who / why.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface SensitiveField {

    /** The audit field name recorded on the access-log row (what was read). */
    String value();
}
