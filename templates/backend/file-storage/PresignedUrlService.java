// @ax-template-meta: template_id=backend/file-storage/PresignedUrlService layer=backend domain=file-storage
// evidence: FILE-SEC-001 (internal storage path never exposed; presigned URL only)
package com.ax.template.authblueprint.filestorage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * PresignedUrlService — generates time-limited download tokens for file access.
 *
 * <p>For local storage: issues an HMAC-SHA256 signed token that encodes fileId + expiry.
 * The download controller verifies the token before serving the file.
 *
 * <p>For S3 storage: delegate to the AWS SDK S3Presigner instead of this class.
 * This implementation is intentionally simple for the local-storage reference workload.
 *
 * <p>FILE-SEC-001: The storage key (S3 key / filesystem path) is NEVER included in the
 * generated URL. Only the fileId + signature are embedded; the storage key is resolved
 * server-side from the DB when the token is presented.
 *
 * <p>Fork instructions:
 * <ol>
 *   <li>Set FILE_STORAGE_SIGNING_KEY env variable (min 32 bytes of entropy).</li>
 *   <li>For S3: replace this bean with an S3Presigner-backed implementation.</li>
 *   <li>Adjust TTL via file-storage.presigned-url.ttl-seconds (default 300).</li>
 * </ol>
 */
@Service
public class PresignedUrlService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String signingKey;
    private final int ttlSeconds;
    private final String baseUrl;

    public PresignedUrlService(
            @Value("${file-storage.signing-key:dev-signing-key-change-in-prod}") String signingKey,
            @Value("${file-storage.presigned-url.ttl-seconds:300}") int ttlSeconds,
            @Value("${file-storage.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.signingKey = signingKey;
        this.ttlSeconds = ttlSeconds;
        this.baseUrl = baseUrl;
    }

    /**
     * Generates a presigned download URL for the given file.
     *
     * <p>Format: {@code {baseUrl}/api/files/{fileId}/download?token={base64(fileId:expiry:sig)}}
     *
     * @param fileId the file UUID
     * @return presigned URL valid for ttlSeconds
     */
    public String generatePresignedUrl(UUID fileId) {
        long expiresEpochSeconds = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = fileId + ":" + expiresEpochSeconds;
        String signature = hmacSha256(payload);
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((payload + ":" + signature).getBytes(StandardCharsets.UTF_8));
        return baseUrl + "/api/files/" + fileId + "/download?token=" + token;
    }

    /**
     * Validates a presigned download token.
     *
     * @param fileId the file UUID from the path
     * @param token  the token query parameter
     * @return true if the token is valid and not expired
     */
    public boolean isTokenValid(UUID fileId, String token) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(token);
            String[] parts = new String(decoded, StandardCharsets.UTF_8).split(":");
            if (parts.length != 3) return false;

            String tokenFileId = parts[0];
            long expiresEpochSeconds = Long.parseLong(parts[1]);
            String tokenSig = parts[2];

            if (!fileId.toString().equals(tokenFileId)) return false;
            if (Instant.now().getEpochSecond() > expiresEpochSeconds) return false;

            String expectedSig = hmacSha256(tokenFileId + ":" + expiresEpochSeconds);
            return constantTimeEquals(expectedSig, tokenSig);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 initialization failed", e);
        }
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
