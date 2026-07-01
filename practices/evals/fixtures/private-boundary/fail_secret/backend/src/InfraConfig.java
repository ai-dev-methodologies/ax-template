// InfraConfig.java — private fixture for private_boundary_guard [R26] fail_secret test.
// This file intentionally contains a raw PEM private key header that Layer 2 should catch.
package com.example.infra;

public class InfraConfig {
    // DO NOT commit real private keys. This fixture proves the guard fires.
    private static final String SIGNING_KEY =
        "-----BEGIN RSA PRIVATE KEY-----\n" +
        "MIIEowIBAAKCAQEA0Z3VS5JJcds3xHn/ygWep4PAtEsHAAAAAAAAAAAAAAAAAAAAAA\n" +
        "-----END RSA PRIVATE KEY-----";
}
