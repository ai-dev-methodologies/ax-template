package com.ax.template.authblueprint.dsr;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reference {@link PersonalDataProvider} so the DSR access / portability / erasure
 * fan-out is exercised end-to-end by the compliance tests. A fork-receiver ships
 * one of these per real module (profile, orders, audit, …) and removes this demo.
 *
 * <p>It models a single "profile" category with a rectifiable {@code displayName}
 * and a non-rectifiable derived {@code riskScore}. A subject id containing the
 * {@code legal-hold} marker (the tests mint such an email) keeps its audit-trail
 * category under retention to demonstrate the partial-erasure manifest
 * (DSR-ERASURE-001 / GDPR Art 17(3)).
 *
 * <p>State is in-memory and per-subject so erasure is observably idempotent: the
 * second {@link #erase(String)} on an already-erased subject is a no-op that still
 * reports the same retained categories.
 */
@Component
public class DemoProfilePersonalDataProvider implements PersonalDataProvider {

    static final String MODULE = "profile";
    static final String LEGAL_HOLD_MARKER = "legal-hold";

    /** Subjects whose demo profile has been purged (erasure idempotency state). */
    private final Set<String> erased = ConcurrentHashMap.newKeySet();

    @Override
    public String moduleName() {
        return MODULE;
    }

    @Override
    public Map<String, Object> collect(String subjectId) {
        if (erased.contains(subjectId)) {
            return Map.of();
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("displayName", "Subject " + subjectId);
        data.put("locale", "en-US");
        // Derived/inferred — present in access export but NOT rectifiable and NOT
        // included by callers that scope portability to subject-provided data.
        data.put("riskScore", 0.12);
        return data;
    }

    @Override
    public List<String> rectifiableFields() {
        // Only subject-editable fields. riskScore is derived → absent → 422.
        return List.of(MODULE + ".displayName", MODULE + ".locale");
    }

    @Override
    public List<RetainedCategory> erase(String subjectId) {
        erased.add(subjectId);
        if (subjectId != null && subjectId.contains(LEGAL_HOLD_MARKER)) {
            // Art 17(3): retained for an overriding legal obligation.
            return List.of(new RetainedCategory(
                MODULE + ".audit_trail", "legal_obligation_retention"));
        }
        return List.of();
    }
}
