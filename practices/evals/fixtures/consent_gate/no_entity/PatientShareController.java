package com.example.noconsent;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * INERT fixture: a data-sharing endpoint exists, but this tree has NOT adopted the
 * consent ledger (no ConsentRecord @Entity). The guard is forward-enforcing — it
 * only fires once a fork-receiver adopts consent — so it MUST exit 0 here even
 * though the sharing method does not reference ConsentGate.
 */
@RestController
public class PatientShareController {

    @PostMapping("/api/patients/{id}/share")
    public void shareWithThirdParty(String subjectId) {
        dispatch(subjectId);
    }

    private void dispatch(String subjectId) {
        // ... forward ...
    }
}
