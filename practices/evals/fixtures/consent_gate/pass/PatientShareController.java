package com.example.share;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PASS fixture: a data-sharing endpoint that GATES on the purpose grant before
 * forwarding the records to a third party. consent_gate_guard MUST exit 0.
 */
@RestController
public class PatientShareController {

    @PostMapping("/api/patients/{id}/share")
    public void shareWithThirdParty(String subjectId, List<ConsentRecord> ledger) {
        // CONSENT-PURPOSE-001: gate the sharing path on the specific purpose grant.
        ConsentGate.requireConsent(subjectId, "third_party_share", ledger);
        dispatch(subjectId);
    }

    private void dispatch(String subjectId) {
        // ... forward to the third party ...
    }
}
