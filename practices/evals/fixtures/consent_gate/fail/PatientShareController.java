package com.example.share;

import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FAIL fixture (the IDW4 adversarial probe): a data-sharing endpoint that forwards
 * a subject's records to a third party WITHOUT checking the purpose grant — an
 * un-gated purpose sharing. consent_gate_guard MUST exit 1.
 */
@RestController
public class PatientShareController {

    @PostMapping("/api/patients/{id}/share")
    public void shareWithThirdParty(String subjectId, List<ConsentRecord> ledger) {
        // No ConsentGate.requireConsent on this sharing path — the deviation.
        dispatch(subjectId);
    }

    private void dispatch(String subjectId) {
        // ... forward to the third party ...
    }
}
