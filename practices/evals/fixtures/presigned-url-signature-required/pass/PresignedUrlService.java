/**
 * FIXTURE: presigned-url-signature-required/pass
 * Demonstrates CORRECT pattern: presigned URL is HMAC-signed before returning.
 * Server verifies the HMAC on every download request, preventing URL forgery.
 */
package com.example.fixture.presigned_url;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class PresignedUrlService {

    private final S3Presigner presigner;
    private final byte[] hmacSecret;
    private static final String BUCKET = "my-bucket";
    private static final String HMAC_ALG = "HmacSHA256";

    public PresignedUrlService(S3Presigner presigner,
                               @Value("${storage.hmac-secret}") String hmacSecret) {
        this.presigner = presigner;
        this.hmacSecret = hmacSecret.getBytes(StandardCharsets.UTF_8);
    }

    // CORRECT: HMAC signature appended as a query parameter.
    // The download endpoint verifies the signature before streaming the object.
    public String generateDownloadUrl(String objectKey) throws Exception {
        long expires = Instant.now().plusSeconds(900).getEpochSecond();
        String payload = objectKey + ":" + expires;
        String sig = hmacSign(payload);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(900))
                .getObjectRequest(getObjectRequest)
                .build();
        URL s3Url = presigner.presignGetObject(presignRequest).url();

        // Append HMAC sig + expiry so the proxy layer can verify authenticity
        return s3Url + "&sig=" + sig + "&exp=" + expires;
    }

    private String hmacSign(String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALG);
        mac.init(new SecretKeySpec(hmacSecret, HMAC_ALG));
        return Base64.getUrlEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
