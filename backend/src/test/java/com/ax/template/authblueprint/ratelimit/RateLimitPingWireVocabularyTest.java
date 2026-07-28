package com.ax.template.authblueprint.ratelimit;

import io.restassured.RestAssured;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-2 round 2 — RUNTIME proof of the {@code /ratelimit/ping} status vocabulary.
 *
 * <p><b>Why this class exists.</b> {@code contracts/ratelimit-openapi.yaml} declares the
 * probe's {@code status} as {@code enum: [ok]}. Nothing in the reference workload binds
 * that block to a Java enum — the controller writes the literal inline — so
 * {@code practices/evals/contract_enum_parity_guard.sh} classifies it {@code wire_only}
 * and extracts the literal from the controller source with a regex. A cross-family
 * reviewer showed twice over that a source regex is the wrong instrument for this claim:
 * <ol>
 *   <li>first, the entry was CLASSIFICATION-ONLY, so flipping {@code "ok"} to
 *       {@code "healthy"} left contract, guard and {@code testRateLimit} all green;</li>
 *   <li>then, after the entry was made to extract literals, writing
 *       {@code return Map.of("status", "healthy".toString());} in {@link
 *       RateLimitPingController#ping()} defeated it AGAIN — the expression is not a plain
 *       literal, so the pattern skipped it and captured {@code "ok"} from the UNRELATED
 *       {@code /anon/ping} method. The endpoint emitted {@code healthy}, the guard said
 *       PASS.</li>
 * </ol>
 * The guard is now fail-closed for that shape (its {@code residue_probe} refuses to guess
 * when a producing construct holds anything but a literal), but a static extractor can
 * only ever prove things about SOURCE. What the endpoint actually puts on the wire is a
 * runtime fact, and this class asserts it as one.
 *
 * <p><b>What is asserted.</b> Both probes — {@code /api/ratelimit/ping} (rate-limited,
 * keyed by {@code X-API-Key}) and {@code /api/ratelimit/anon/ping} (RATELIMIT-5,
 * unauthenticated) — are driven over real HTTP, and the SET of {@code status} values they
 * emit is compared for EXACT EQUALITY with the vocabulary declared at
 * {@code /paths/~1ratelimit~1ping/get/responses/200/content/application~1json/schema
 * /properties/status} in {@code contracts/ratelimit-openapi.yaml}, parsed from disk. The
 * two directions are both load-bearing: a value the contract does not declare fails (the
 * reviewer's mutation), and a declared value nothing emits fails too (a contract that
 * grows a token no producer can serve). Driving BOTH methods is what makes the mutation
 * detectable — mutating one while the other keeps the contract literal is precisely the
 * shape that defeated the regex.
 *
 * <p>Precedent for reading the expectation from the contract rather than literal-ising it
 * in the test body: {@code PageEnvelopeCatalogSweepTest} (same tree). Parsing is snakeyaml
 * against the checked-in file — deterministic and offline. Path convention follows the
 * existing precedent in this test tree ({@code WebhookIdempotentTest}): gradle runs tests
 * with {@code user.dir} == {@code backend/}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// R22 ContextCache lever — same rationale as RateLimitComplianceTest: BEFORE_CLASS forces
// a fresh boot so @LocalServerPort cannot point at an evicted (dead) Tomcat under the
// heavy per-domain aggregate. A fresh context also gives this class fresh rate-limit
// buckets, so the anonymous probe below cannot inherit another class's exhausted quota.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class RateLimitPingWireVocabularyTest {

    /** The contract file whose enum block this class proves. */
    private static final Path CONTRACT = Path.of("..", "contracts", "ratelimit-openapi.yaml");

    /**
     * RFC-6901 pointer of the guarded block, spelled out so the binding in
     * {@code practices/evals/contract-enum-map.yaml} and this test name the SAME block.
     */
    private static final String POINTER =
        "/paths/~1ratelimit~1ping/get/responses/200/content/application~1json/schema/properties/status";

    @LocalServerPort
    int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
    }

    @Test
    @Tag("RATELIMIT")
    @Tag("RATELIMIT-WIRE-VOCAB-001")
    void pingProbesEmitExactlyTheContractDeclaredStatusVocabulary() throws IOException {
        Set<String> declared = declaredStatusVocabulary();
        assertThat(declared)
            .as("%s#%s must declare a non-empty vocabulary for this test to mean anything",
                CONTRACT, POINTER)
            .isNotEmpty();

        Set<String> observed = new TreeSet<>();
        observed.add(statusOf(
            given().header("X-API-Key", freshKey()).when().get("/api/ratelimit/ping"),
            "/api/ratelimit/ping"));
        observed.add(statusOf(
            given().when().get("/api/ratelimit/anon/ping"),
            "/api/ratelimit/anon/ping"));

        assertThat(observed)
            .as("RUNTIME vocabulary of the ratelimit probes must EQUAL the set declared at "
                + "%s#%s. A value the contract does not declare means a client that "
                + "switch-cases the contract meets an unknown token; a declared value no "
                + "probe emits means the contract promises something nothing serves.",
                CONTRACT, POINTER)
            .isEqualTo(declared);
    }

    /** Drives one probe and returns its {@code status} member, asserting the shape first. */
    private String statusOf(Response response, String what) {
        assertThat(response.statusCode())
            .as("%s must answer 200 for the wire vocabulary to be observable", what)
            .isEqualTo(200);
        Object status = response.jsonPath().get("status");
        assertThat(status)
            .as("%s must emit a `status` member (the contract declares it required)", what)
            .isInstanceOf(String.class);
        return (String) status;
    }

    /**
     * The declared vocabulary, read from {@link #CONTRACT} by walking {@link #POINTER}.
     * Every step is asserted rather than defaulted, so a contract that moved the block
     * fails HERE instead of silently degrading into a weaker (or vacuous) comparison.
     */
    @SuppressWarnings("unchecked")
    private Set<String> declaredStatusVocabulary() throws IOException {
        assertThat(Files.isRegularFile(CONTRACT))
            .as("contract must exist on disk: %s (user.dir=%s)",
                CONTRACT, System.getProperty("user.dir"))
            .isTrue();
        Object node;
        try (Reader reader = Files.newBufferedReader(CONTRACT)) {
            node = new Yaml().load(reader);
        }
        StringBuilder at = new StringBuilder(CONTRACT.toString());
        for (String token : POINTER.split("/")) {
            if (token.isEmpty()) {
                continue;
            }
            String key = token.replace("~1", "/").replace("~0", "~");
            assertThat(node).as("%s: expected a mapping while resolving '%s'", at, key)
                .isInstanceOf(Map.class);
            Map<Object, Object> map = (Map<Object, Object>) node;
            Object next = null;
            boolean found = false;
            for (Map.Entry<Object, Object> entry : map.entrySet()) {
                if (String.valueOf(entry.getKey()).equals(key)) {
                    next = entry.getValue();
                    found = true;
                    break;
                }
            }
            assertThat(found).as("%s: missing key '%s' (present: %s)", at, key, map.keySet())
                .isTrue();
            node = next;
            at.append('.').append(key);
        }
        assertThat(node).as("%s: must be a schema mapping", at).isInstanceOf(Map.class);
        Object enumNode = ((Map<Object, Object>) node).get("enum");
        assertThat(enumNode).as("%s: must declare an `enum:` list", at).isInstanceOf(List.class);
        return ((List<Object>) enumNode).stream()
            .map(String::valueOf)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private String freshKey() {
        return "wire-vocab-" + UUID.randomUUID();
    }
}
