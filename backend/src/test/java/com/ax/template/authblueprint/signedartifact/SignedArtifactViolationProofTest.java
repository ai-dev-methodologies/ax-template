package com.ax.template.authblueprint.signedartifact;

import jakarta.persistence.Column;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for signed-artifact-l0. Structural assertions a deliberate break cannot pass
 * silently: the issuance record is immutable append-only (no setter, every column
 * @Column(updatable=false)) with NO delete path anywhere; the service signs with an EC/ES256 key
 * (never HMAC — no {@code javax.crypto.Mac}/{@code SecretKeySpec} import in the signing path);
 * and the verifier resolves BOTH the algorithm and the key from server-side configuration keyed
 * by kid rather than trusting the token's own header.
 */
@Tag("SIGNEDARTIFACT")
class SignedArtifactViolationProofTest {

    // ── SIGNED-ASYM-001 — the issuance record is immutable, no setter, no delete path ──
    @Test @Tag("SIGNED-ASYM-001")
    void violation_recordImmutable_noSetter_noDeletePath() throws Exception {
        for (Method m : SignedArtifact.class.getMethods()) {
            assertThat(m.getName()).as("SignedArtifact must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "subjectRef", "contentHash", "kid", "alg", "jws", "issuedAt"}) {
            Column col = SignedArtifact.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("SignedArtifact." + f + " must be immutable").isFalse();
        }
        for (Method m : SignedArtifactRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).as("SignedArtifactRepository declares no delete method")
                .doesNotContain("delete");
        }
        for (String src : new String[]{"SignedArtifactService", "SignedArtifactController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "signedartifact", src + ".java"));
            assertThat(text).as(src + " must contain no delete call")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }
    }

    // ── SIGNED-ASYM-001 — the key provider is EC (asymmetric); no symmetric HMAC anywhere in the signing path ──
    @Test @Tag("SIGNED-ASYM-001")
    void violation_signingKeyIsAsymmetric_noHmacInIssuePath() throws Exception {
        String keyProvider = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "signedartifact", "SignedArtifactKeyProvider.java"));
        assertThat(keyProvider).as("the key pair is EC (asymmetric), never a symmetric secret")
            .contains("KeyPairGenerator.getInstance(\"EC\")");
        assertThat(keyProvider).doesNotContain("SecretKeySpec").doesNotContain("HmacSHA");

        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "signedartifact", "SignedArtifactService.java"));
        assertThat(service).as("issuance signs with ECDSASigner (asymmetric), never HMAC")
            .contains("new ECDSASigner(").doesNotContain("SecretKeySpec").doesNotContain("HmacSHA");
        assertThat(service).as("the server pins a single asymmetric algorithm — ES256")
            .contains("JWSAlgorithm.ES256");
    }

    // ── SIGNED-ALG-ALLOWLIST-001 — alg + key resolved from server config keyed by kid, not the token's own header ──
    @Test @Tag("SIGNED-ALG-ALLOWLIST-001")
    void violation_verifierResolvesAlgAndKeyFromServerConfig_notFromTokenHeader() throws Exception {
        String service = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "signedartifact", "SignedArtifactService.java"));
        // alg:none carries an EMPTY signature segment that JWSObject.parse itself refuses, so it
        // MUST be caught by inspecting the raw header BEFORE the token ever reaches JWSObject —
        // a SEPARATE enforcement point from the post-parse comparison below.
        assertThat(service).as("alg:none is rejected from the RAW header, before JWSObject.parse is even called")
            .contains("headerAlgOf(compactJws)").contains("\"none\".equalsIgnoreCase(");
        int noneCheckAt = service.indexOf("headerAlgOf(compactJws)");
        int parseAt = service.indexOf("JWSObject.parse(compactJws)");
        assertThat(noneCheckAt).as("the raw-header alg:none check runs BEFORE JWSObject.parse")
            .isPositive().isLessThan(parseAt);

        // kid is checked against the server's OWN key id before any alg/signature check
        assertThat(service).as("the kid must resolve to the server's OWN published key")
            .contains("keyProvider.keyId().equals(kid)");
        // the presented alg is compared to the FIXED server-configured algorithm, not used to
        // select a verifier implementation — this is the SECOND, independent enforcement point:
        // it rejects any parseable-but-wrong-alg forgery (HS256 confusion, ES384 mismatch, ...)
        // that made it past JWSObject.parse.
        assertThat(service).as("the presented alg is compared to the server's fixed configured algorithm")
            .contains("SERVER_ALGORITHM.equals(presentedAlg)");
        // the verifying key comes from the key provider (server config), never parsed from the
        // token's own header/claims.
        assertThat(service).as("the verifying key comes from server-side config, not the token")
            .contains("new ECDSAVerifier(keyProvider.verifyingKey())");
    }
}
