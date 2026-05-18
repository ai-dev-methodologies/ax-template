/**
 * FIXTURE: presigned-url-signature-required/fail_no_signature
 * Demonstrates WRONG pattern: presigned URL generated without HMAC signature.
 * A caller who obtains the storage key can construct the URL themselves and
 * bypass all authorization checks. Guard catches: no HMAC step before URL return.
 * Violates presigned-url-signature-required rule.
 */
package com.example.fixture.presigned_url;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;
import java.net.URL;

@Service
public class PresignedUrlService {

    private final S3Presigner presigner;
    private static final String BUCKET = "my-bucket";

    public PresignedUrlService(S3Presigner presigner) {
        this.presigner = presigner;
    }

    // VIOLATION: URL returned directly from S3 presigner without an HMAC
    // envelope signature. An attacker who guesses the storage key can call
    // S3 directly without ever touching this service.
    public String generateDownloadUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        URL presignedUrl = presigner.presignGetObject(presignRequest).url();
        // BUG: no HMAC signature wrapping — URL authenticity not verified server-side
        return presignedUrl.toString();
    }
}
