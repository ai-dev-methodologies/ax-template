// AcmeOrderService.java — private fixture for private_boundary_guard [R26] fail_marker test.
// This file intentionally contains a fork-receiver identifier that the marker layer should catch.
package com.acmecorp.order;

/**
 * AcmeCorp internal order processing service.
 * This is a PRIVATE implementation — not for the public ax-template catalog.
 */
public class AcmeOrderService {
    private static final String COMPANY = "AcmeCorp";

    public String getCompanyName() {
        return COMPANY;
    }
}
