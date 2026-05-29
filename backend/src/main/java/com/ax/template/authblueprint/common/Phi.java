package com.ax.template.authblueprint.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IMW4 (IDW4 dogfood 2026-05-30) — explicit PROTECTED HEALTH INFORMATION (PHI) tag.
 *
 * <p>Place {@code @Phi} on the entity field / DTO record component / getter that
 * actually carries PHI (a patient's diagnosis, medication, vitals, free-text
 * clinical note, RRN-adjacent medical identifier, etc.). This annotation is the
 * single, explicit, intent-bearing tag that two regulated-data guards key on:
 *
 * <ul>
 *   <li>{@code practices/evals/audit_on_read_guard.sh} — a read method
 *       (a {@code @Transactional} / {@code @Service} method whose return type, or
 *       a type it exposes, carries a {@code @Phi}-tagged member) that does NOT
 *       reference {@code AuditLogService.record} on that path is a violation.
 *       HIPAA §164.312(b) "audit controls": access to PHI must be recorded.</li>
 *   <li>{@code practices/evals/phi_in_logs_guard.sh} — a
 *       {@code log.{info,debug,warn,error,trace}(...)} statement whose arguments
 *       include a getter of a {@code @Phi}-tagged field is a violation. Raw PHI
 *       must never reach a log aggregator (ELK / Splunk / CloudWatch); use
 *       {@link AuditPiiHelper#piiHash(String)} for a correlation token instead.</li>
 * </ul>
 *
 * <h2>Why a tag and not a name heuristic (load-bearing)</h2>
 * The guards deliberately key on {@code @Phi} ONLY — never on field/getter NAMES.
 * A name heuristic ("anything called {@code getDiagnosis} / {@code getName} / …")
 * would false-positive across the whole reference workload: {@code getName()},
 * {@code getEmail()}, {@code getReason()} appear in dozens of non-PHI domains
 * (auth, billing, tags, …). By requiring an explicit {@code @Phi} the guards are
 * precise (zero false positives on a tree with no PHI) and forward-enforcing:
 * they fire only the moment a fork-receiver tags a real PHI member, which is
 * exactly when the HIPAA audit-on-read + no-raw-PHI-in-logs obligations attach.
 *
 * <h2>Where to place it</h2>
 * <ul>
 *   <li>On a JPA {@code @Entity} field:
 *       <pre>{@code @Phi @Column(name = "diagnosis") private String diagnosis;}</pre></li>
 *   <li>On a DTO record component:
 *       <pre>{@code public record EncounterView(Long id, @Phi String diagnosis) {}}</pre></li>
 *   <li>On a getter (when the field itself cannot be annotated):
 *       <pre>{@code @Phi public String getDiagnosis() { return diagnosis; }}</pre></li>
 *   <li>On the TYPE, when EVERY member is PHI (e.g. a clinical-note projection):
 *       <pre>{@code @Phi public record ClinicalNote(String body) {}}</pre></li>
 * </ul>
 *
 * <p>{@link RetentionPolicy#SOURCE} only — this is a static-analysis marker
 * consumed by the two bash guards above, never introspected at runtime, so it
 * adds zero bytecode and zero runtime dependency. {@link ElementType#FIELD} +
 * {@link ElementType#METHOD} (getters) + {@link ElementType#TYPE} (whole-PHI
 * projections) are the placement targets the guards scan for.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Phi {

    /**
     * Optional free-text classification of the PHI category (e.g.
     * {@code "diagnosis"}, {@code "medication"}, {@code "clinical-note"}).
     * Documentation only — the guards do not branch on it.
     */
    String value() default "";
}
