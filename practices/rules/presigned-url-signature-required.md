---
title: "File-storage presigned URLs must include an HMAC server signature before returning to callers"
rule_id: presigned-url-signature-required
impact: HIGH
impactDescription: "An unsigned presigned URL can be constructed by anyone who knows the storage key, bypassing all authorization checks in the application layer"
tags:
  - file-storage
  - security
  - presigned-url
  - hmac
  - authorization
provenance_class: internal_design
protects_template_id: templates/backend/file-storage/PresignedUrlService.java
failing_fixture_path: practices/evals/fixtures/presigned-url-signature-required/fail_no_signature/
spec_ref: "specs/spring-practices-l0.yaml#PRACTICES-CORE-001"
verification:
  type: review
  notes: "Every PresignedUrlService.generateDownloadUrl / generateUploadUrl must compute HMAC over (objectKey + expiry) and append sig + exp query parameters."
evidence:
  - source_type: external
    citation: "AWS S3 Developer Guide — Presigned URLs: if a request is made by using the temporary security credentials of an IAM role, the presigned URL expires when the credentials used to sign the URL expire"
    url: "https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "OWASP Cheat Sheet — Insecure Direct Object References (IDOR): all resource access must verify authorization at the application layer, not just at the storage layer"
    url: "https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html"
    quoted_at: "2026-05-18"
  - source_type: external
    citation: "RFC 2104 — HMAC: Keyed-Hashing for Message Authentication: HMAC verifies both the data integrity and the authenticity of a message; a presigned URL signs its parameters + expiry with a server-held HMAC key so any tampering invalidates the signature"
    url: "https://www.rfc-editor.org/rfc/rfc2104"
    quoted_at: "2026-06-02"
decided_at: "2026-05-18"
---

## File-storage presigned URLs must include an HMAC server signature before returning to callers

**Impact: HIGH — A presigned URL without an HMAC envelope can be constructed by any caller who observes or guesses the storage key. The S3 presigned URL alone only proves the caller knew the AWS credentials at signing time — it does not prove the application authorised the specific user.**

S3 presigned URLs embed AWS credentials and expire after a configured duration. However, they bypass the application's own authorization layer: a caller who obtains the `objectKey` can construct a functionally equivalent presigned URL themselves by re-signing with the same AWS credentials (if they are leaked) or by extending the expiry. Adding an HMAC signature over `(objectKey + expiry)` with an application-controlled secret provides a server-side authenticity check that the download endpoint can verify before proxying or redirecting.

**Incorrect — presigned URL returned directly without HMAC:**

```java
@Service
public class PresignedUrlService {

    public String generateDownloadUrl(String objectKey) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(BUCKET).key(objectKey).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(get).build();
        // VIOLATION: raw S3 URL returned — no HMAC envelope
        return presigner.presignGetObject(req).url().toString();
    }
}
```

**Correct — HMAC signature appended as query parameters:**

```java
@Service
public class PresignedUrlService {

    private final byte[] hmacSecret;

    public String generateDownloadUrl(String objectKey) throws Exception {
        long expires = Instant.now().plusSeconds(900).getEpochSecond();
        String payload = objectKey + ":" + expires;
        String sig = hmacSign(payload);                    // HmacSHA256(key, payload)

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(BUCKET).key(objectKey).build();
        GetObjectPresignRequest req = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(900))
                .getObjectRequest(get).build();
        String s3Url = presigner.presignGetObject(req).url().toString();

        // CORRECT: ?sig=<hmac>&exp=<epoch> allows server-side verification
        return s3Url + "&sig=" + sig + "&exp=" + expires;
    }

    private String hmacSign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
        return Base64.getUrlEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }
}
```

## Why this matters

Without the HMAC envelope, the download flow has no application-layer authorization gate: the S3 presigned URL is sufficient to retrieve the object. If the `objectKey` is guessable (sequential IDs, predictable patterns), an attacker can download arbitrary files. With HMAC:

1. The download endpoint verifies `sig = HMAC(objectKey + exp)` before proxying.
2. If the signature is invalid or expired, the request is rejected with 403 before touching S3.
3. The HMAC secret is application-controlled — rotating it invalidates all outstanding URLs.

This pattern applies to both download (GET presigned) and upload (PUT presigned) URLs.

## Failing fixture

See: `practices/evals/fixtures/presigned-url-signature-required/fail_no_signature/PresignedUrlService.java` — `generateDownloadUrl` returns the raw S3 presigned URL without HMAC. No `sig` or `exp` parameters in the returned URL.

Reference: [AWS S3 Developer Guide — Using presigned URLs](https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html)

Reference: [OWASP — Insecure Direct Object Reference Prevention](https://cheatsheetseries.owasp.org/cheatsheets/Insecure_Direct_Object_Reference_Prevention_Cheat_Sheet.html)
