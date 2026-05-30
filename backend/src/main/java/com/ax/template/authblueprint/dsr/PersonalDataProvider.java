package com.ax.template.authblueprint.dsr;

import java.util.List;
import java.util.Map;

/**
 * SPI a module implements to participate in subject-access (DSR-ACCESS-001),
 * portability (DSR-PORTABILITY-001) and erasure (DSR-ERASURE-001) fan-out.
 *
 * <p>{@link DsrService} discovers every {@code PersonalDataProvider} bean and
 * aggregates / erases across them, so a new module joins the DSR contract simply
 * by publishing a bean — no change to the DSR core.
 *
 * <p>Implementations MUST be side-effect-free on the read paths
 * ({@link #collect(String)} / {@link #rectifiableFields()}) and idempotent on
 * {@link #erase(String)} (re-erasing an already-erased subject is a no-op that
 * still reports its retained categories).
 *
 * <p>The read hook is named {@code collect} (not {@code export}) deliberately: a
 * DSR access / portability request is the data SUBJECT exercising their own Art
 * 15/20 right over their own data — it is gathering the subject's data for the
 * subject, NOT a third-party disclosure that would require a separate purpose grant.
 */
public interface PersonalDataProvider {

    /** Stable, bounded module identifier (the access bundle / CSV category key). */
    String moduleName();

    /**
     * Personal data this module holds for the subject, as a structured map
     * (subject-provided + observed data only — NOT derived/inferred profiling).
     * Empty map when the module holds nothing for the subject.
     */
    Map<String, Object> collect(String subjectId);

    /**
     * Field paths in this module the subject MAY rectify (DSR-RECTIFY-001 allowlist).
     * A path is {@code moduleName + "." + field}. Anything not listed is treated as
     * derived / authority-of-record and rejected with 422.
     */
    default List<String> rectifiableFields() {
        return List.of();
    }

    /**
     * Erase (soft-delete → purge) the subject's data in this module.
     *
     * @return the categories this module RETAINED under a legal-hold / retention
     *         obligation (empty when fully erased). A non-empty result drives the
     *         partial-erasure manifest (DSR-ERASURE-001, GDPR Art 17(3)).
     */
    List<RetainedCategory> erase(String subjectId);

    /** A category retained despite an erasure request, with its retention basis. */
    record RetainedCategory(String category, String legalBasis) {}
}
