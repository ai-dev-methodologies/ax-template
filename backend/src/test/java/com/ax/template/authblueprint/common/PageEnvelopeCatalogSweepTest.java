package com.ax.template.authblueprint.common;

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
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * BACKLOG P3-54 — page-envelope parity beyond scenario-local.
 * <p>
 * {@link PageEnvelopeContractParityTest} pins ONE hand-built envelope's wire
 * shape (S2.QUERY-BOUNDS.XB). It says nothing about the OTHER 20 real list
 * endpoints across the catalog whose contracts also declare a page/list
 * envelope. This test closes that scenario-local gap for the subset of those
 * endpoints that are actually LIVE-HTTP reachable today (an existing
 * RestAssured signup+login test-support fixture exists for the domain) —
 * driving each one as a real authorized principal and asserting the emitted
 * body's top-level member SET equals that endpoint's OWN contract-declared
 * shape (envelope shapes differ per contract: {@code content/totalElements/
 * totalPages/page/size} vs {@code items/totalElements} vs
 * {@code items/page/size/totalElements} — this is per-contract parity, not
 * one fixed shape).
 *
 * <p><b>The expected member set is READ FROM THE CONTRACT, not hard-coded
 * (2026-07-29 correction).</b> The first closure discovered the declaring
 * universe from disk but then compared each live response against a member set
 * literal-ised in the test body. That is not contract parity: it pins the
 * response against the shape the PRODUCTION CODE happened to emit on the day
 * the test was written, so {@code contracts/*.yaml} could drift arbitrarily
 * (rename {@code totalPages} -> {@code pageCount} in
 * {@code audit-log-openapi.yaml} and change no production code) and the sweep
 * stayed green — the contract half of "contract parity" was never read. Both
 * halves of each binding are now DERIVED from the contract document on disk
 * (see {@link #envelopeFromContract}): the URL the endpoint is driven at is
 * {@code servers[0].url + <path>}, and the expected member set is the
 * {@code required} list of the schema that {@code <path>.<method>.responses.200
 * .content.application/json.schema.$ref} resolves to. Only the routing
 * coordinates (contract stem, path, method) are declared in {@link #BINDINGS};
 * every member name comes from the yaml. Parsing is snakeyaml against the
 * checked-in file — deterministic and offline, no network and no generated
 * artifact.
 *
 * <h2>The TYPE axis (P3-85, 2026-07-29)</h2>
 * Member NAMES agreeing is not member TYPES agreeing. Until this axis existed the
 * sweep locked the member SET only, so the reproduction recorded in the BACKLOG
 * row passed: change any contract's {@code totalPages} from {@code type: integer}
 * to {@code type: string} and every member name is unchanged, so the sweep stayed
 * GREEN while the contract promised a wire type the code does not emit — a strict
 * generated client would fail to parse a response this test called conformant.
 * <p>
 * Each declared member's {@code type} (and {@code format}, where the contract
 * states one) is now compared against the ACTUAL JSON type of the emitted value
 * (see {@link #jsonTypeOf} / {@link #assertMemberTypeMatchesContract}). The
 * comparison follows JSON Schema: an integral value satisfies a declared
 * {@code number}, but a fractional one does not satisfy a declared
 * {@code integer}. Two fail-closed choices, both deliberate: a member declaring
 * no {@code type:} FAILS rather than being skipped (an unknown declared type
 * would silently drop that member back to name-only checking), and a JSON
 * {@code null} FAILS rather than being treated as unverifiable (no swept envelope
 * declares a nullable member).
 * <p>
 * <b>Disclosed non-tightness.</b> Every swept member is {@code array} or
 * {@code integer}, and the only {@code format} any of them carries is
 * {@code int64} — which any deserialized Integer/Long satisfies. The format arm
 * is therefore vacuous against today's catalog; it is written for the case it
 * exists to catch (a value overflowing a declared {@code int32}). The TYPE
 * comparison is what carries the mutation lock.
 *
 * <h2>Binding table (routing coordinates only — members are derived)</h2>
 * <ul>
 *   <li>GET {@code /audit-logs} — audit-log-openapi.yaml — testAuditLog</li>
 *   <li>GET {@code /notifications} — notification-openapi.yaml — testNotification</li>
 *   <li>GET {@code /sessions} — session-management-openapi.yaml — testSessionManagement</li>
 *   <li>GET {@code /favorites} — favorites-bookmarks-openapi.yaml — testFavorites</li>
 *   <li>GET {@code /activities} — activity-feed-openapi.yaml — testActivityFeed</li>
 *   <li>GET {@code /api-keys} — api-key-openapi.yaml — testApiKey</li>
 *   <li>GET {@code /approvals} — approval-workflow-openapi.yaml — testApprovalWorkflow</li>
 *   <li>GET {@code /approvals/inbox} — approval-workflow-openapi.yaml — testApprovalWorkflow</li>
 *   <li>GET {@code /admin/identity-verification} —
 *       identity-verification-openapi.yaml — testIdentityVerification</li>
 *   <li>POST {@code /search} — search-openapi.yaml — testSearch</li>
 * </ul>
 * 9 domains / 10 endpoints swept out of the 19 contracts that declare a
 * page/list envelope on disk (ratio 9/19). GREEN is confirmed by the wave's
 * R25 run, not asserted here. The remaining 10 are FULLY partitioned across
 * the three pinned sets below: NOT_REACHABLE (1) +
 * REACHABLE_BUT_PREEXISTING_DRIFT (1) + DECLARING_NOT_IN_SCOPE (8).
 *
 * <h2>The 21st declaring contract (2026-07-28 correction)</h2>
 * The first closure derived the universe with
 * {@code grep -lE '^ *(totalElements|totalPages):' contracts/*.yaml} -> 20
 * files, and explained the PRD's "21 contracts carry page-envelope fields"
 * away as "an items-only edge case a mechanical grep cannot reproduce". That
 * explanation was WRONG, and the PRD was right. The missing contract is
 * {@code search-openapi.yaml}: {@code SearchResultPage}
 * ({@code contracts/search-openapi.yaml:104-121}) is a genuine page envelope —
 * {@code hits} (array) + {@code page} + {@code size} + a total — but it spells
 * its total {@code totalHits}, not {@code totalElements}/{@code totalPages},
 * and its endpoint is a POST ({@code POST /api/v1/search},
 * {@code SearchController.java:51}) rather than a GET, so both halves of the
 * old derivation looked past it.
 * <p>
 * The derivation is therefore broadened from a two-NAME allowlist to the
 * member-name CLASS the catalog actually uses for a total count —
 * {@code ^ *total[A-Z][A-Za-z]*:} — which yields exactly the previous 20 plus
 * {@code search}. Disclosed non-tightness: {@code tokenized-securities} also
 * matches on {@code totalUnits} (a domain quantity, not a page count), but it
 * is independently a declaring contract via {@code GrantPage.pagination}'s
 * {@code totalElements}, so its membership is unchanged and the broadening
 * introduces no false member. And {@code search} is not merely documented as
 * excluded: it is live-HTTP reachable (an existing {@code @SpringBootTest} +
 * RestAssured harness, {@code SearchTestSupport}) and its DTO
 * ({@code SearchDto.SearchResultPage}) matches the contract exactly, so it is
 * SWEPT rather than allowlisted — the partition grows by a swept member, not
 * by an excuse.
 *
 * <p><b>How "fully partitioned" is proven (2026-07-28 correction).</b> The
 * original assertion summed one int and three set sizes — arithmetic only. It
 * could not see a SWAP: moving one domain from one allowlist into another
 * keeps every size (and therefore the sum) identical while one domain silently
 * vanishes from the universe and another is double-counted. The partition is
 * now asserted SET-WISE against the declaring universe DERIVED FROM DISK:
 * pairwise disjointness of the four sets, then union == universe. Every member
 * of every set is a CONTRACT STEM (the `contracts/<stem>-openapi.yaml`
 * basename), so the comparison is against real filenames, not prose labels.
 *
 * <h2>Allowlist A — NOT reachable (no live-HTTP test-support fixture exists
 * for the domain today; a full sweep needs per-domain auth+seed, the
 * artifact class this PRD wave defers)</h2>
 * {@code email-outbox} (EmailOutboxPage — contracts/email-outbox-openapi.yaml:189):
 * every existing test class in the domain is a plain Mockito unit test with
 * no {@code @SpringBootTest}/RestAssured harness — see
 * {@code EmailOutboxComplianceTest} et al. Pinned count:
 * {@link #NOT_REACHABLE}.size() == 1.
 * <p>
 * {@code scheduled-task} was here too until its P3-75 closure (2026-07-29):
 * {@code contracts/scheduled-task-openapi.yaml} declared {@code ScheduledTaskPage}/
 * {@code JobHistoryPage} envelopes that {@code ScheduledTaskController#list}/
 * {@code #history} never delivered (both return a bare {@code List<...>}) — the
 * SAME class of finding as the webhook/feature-flags rows below, but for a
 * domain with no live-HTTP harness to prove which side agreed. Fixed by
 * reconciling the contract to the code (bare array, matching
 * {@code ScheduledTaskSummary}/{@code JobHistorySummary}) and renaming
 * {@code TriggerResponse}'s fields ({@code taskId/triggered/message} ->
 * {@code executed/history/reason}) to the shape
 * {@code ScheduledTaskDto.TriggerResponse} actually emits. The contract no
 * longer declares any {@code total*} member, so {@code scheduled-task} drops
 * out of {@link #declaringUniverseFromDisk()} entirely rather than moving to
 * another allowlist — which took {@link #DECLARING_CONTRACTS} from 21 to 20.
 * (P3-91 later removed {@code webhook} the same way; the constant is now 19 —
 * see Allowlist B.)
 *
 * <h2>Allowlist B — reachable; the sweep found a pre-existing
 * contract/implementation drift for the stem (registered as (P3-67)/(P3-68),
 * both CLOSED 2026-07-29 — kept in this allowlist rather than moved to
 * {@link #SWEPT} because closing the drift did not add a live-HTTP binding
 * for either endpoint here)</h2>
 * <ul>
 *   <li><b>{@code GET /api/v1/admin/feature-flags}</b> — the contract
 *       declared {@code FeatureFlagPage} with FIVE members including
 *       {@code totalPages}, but BOTH sides of the running system agreed on
 *       four: {@code FeatureFlagDto.FlagPage}
 *       (backend/src/main/.../featureflags/FeatureFlagDto.java:76-81) and the
 *       FE's own {@code FeatureFlagPage} interface
 *       (frontend/apps/enterprise/.../featureFlagClient.ts:16-20) both omit
 *       {@code totalPages}. (P3-68) removed {@code totalPages} from the
 *       contract, so the contract now matches both producer and consumer
 *       exactly. Left in this allowlist rather than {@link #SWEPT} because no
 *       {@link EnvelopeBinding} exists here to drive it live and re-derive the
 *       member set from the (now-corrected) contract — a labelling-only move,
 *       deferred rather than done speculatively without a gradle run to
 *       confirm the live response.</li>
 * </ul>
 * Pinned count: {@link #REACHABLE_BUT_PREEXISTING_DRIFT}.size() == 1 (stem
 * count, not open-finding count — the remaining stem's originally-reported
 * drift is closed).
 * <p>
 * {@code webhook} was the second member of this allowlist until its P3-91
 * closure (2026-07-29), and left the same way {@code scheduled-task} left
 * Allowlist A. The residual this class's previous javadoc recorded as
 * "discovered by this closure and NOT yet registered/fixed" — the SAME
 * contract's {@code GET /webhook-endpoints} declaring an unused page envelope
 * against {@code WebhookAdminController#listEndpoints}'s bare
 * {@code List<WebhookDto.EndpointResponse>}
 * (backend/src/main/.../webhook/WebhookAdminController.java:59) — was
 * registered as (P3-91) and closed by the same reconciliation (P3-67) applied
 * to {@code /webhook-deliveries}: the 200 is now a bare array of
 * {@code WebhookEndpointSummary} and the envelope schema is deleted. With it
 * went the contract's last {@code total*} member, so {@code webhook} drops out
 * of {@link #declaringUniverseFromDisk()} entirely rather than moving to
 * another allowlist — {@link #DECLARING_CONTRACTS} is 19, not 20. Both webhook
 * list endpoints now declare the shape they actually emit.
 *
 * <p>Escape hatch stated in the PRD (reachable set &lt; 8 domains ⇒
 * decision-record fallback, precedent P3-41/42) does NOT apply here: the
 * reachable set is 8 domains (≥ 8), so this is the real sweep, not the
 * fallback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// R22 ContextCache lever: BEFORE_CLASS (not AFTER_CLASS). Under the full
// per-domain aggregate the Spring TestContext cache (default cap 32) evicts
// this class's context, leaving @LocalServerPort pointing at a dead Tomcat —
// every test in the class then fails uniformly with NoHttpResponseException.
// AFTER_CLASS only dirties on exit, so it does NOT protect this class from
// inheriting an already-evicted context; BEFORE_CLASS forces a fresh boot.
// Precedent: BillingFlowIT, FeatureFlagFlowIT, CommentComplianceTest.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Tag("PAGINATION")
@Tag("PAGE-OFFSET-001")
class PageEnvelopeCatalogSweepTest {

    /**
     * Routing coordinates of one swept endpoint — WHERE to look the contract up,
     * never WHAT it declares. {@code contractPath} is the key as it appears under
     * {@code paths:} in the yaml (i.e. WITHOUT the {@code servers[0].url} prefix),
     * so a path that no longer exists in the contract fails the lookup loudly
     * instead of silently degrading to a weaker assertion.
     */
    record EnvelopeBinding(String contractStem, String contractPath, String httpMethod) {}

    /**
     * The swept endpoints, keyed by a stable id the test methods below name.
     * 10 endpoints across 9 contract stems — the two approval endpoints share
     * {@code approval-workflow-openapi.yaml}.
     */
    static final Map<String, EnvelopeBinding> BINDINGS;
    static {
        Map<String, EnvelopeBinding> bindings = new LinkedHashMap<>();
        bindings.put("audit-log-list", new EnvelopeBinding("audit-log", "/audit-logs", "get"));
        bindings.put("notification-list", new EnvelopeBinding("notification", "/notifications", "get"));
        bindings.put("session-list", new EnvelopeBinding("session-management", "/sessions", "get"));
        bindings.put("favorite-list", new EnvelopeBinding("favorites-bookmarks", "/favorites", "get"));
        bindings.put("activity-feed-list", new EnvelopeBinding("activity-feed", "/activities", "get"));
        bindings.put("api-key-list", new EnvelopeBinding("api-key", "/api-keys", "get"));
        bindings.put("approval-own-list", new EnvelopeBinding("approval-workflow", "/approvals", "get"));
        bindings.put("approval-inbox", new EnvelopeBinding("approval-workflow", "/approvals/inbox", "get"));
        bindings.put("identity-verification-admin-list",
                new EnvelopeBinding("identity-verification", "/admin/identity-verification", "get"));
        bindings.put("search-result-page", new EnvelopeBinding("search", "/search", "post"));
        BINDINGS = Collections.unmodifiableMap(bindings);
    }

    /**
     * Domains actually swept by this class — DERIVED from {@link #BINDINGS} so the
     * partition below cannot claim a domain is swept unless a binding drives it.
     * (10 endpoints, 9 contracts: the two approval endpoints share
     * {@code approval-workflow-openapi.yaml}.)
     */
    static final Set<String> SWEPT = BINDINGS.values().stream()
            .map(EnvelopeBinding::contractStem)
            .collect(Collectors.toUnmodifiableSet());

    /** Allowlist A — see class javadoc. */
    static final Set<String> NOT_REACHABLE = Set.of("email-outbox");

    /**
     * Allowlist B — see class javadoc. Keyed by CONTRACT STEM (the drift lives
     * in {@code webhook-openapi.yaml} / {@code feature-flags-openapi.yaml}), not
     * by endpoint path, so that the partition below compares like with like.
     */
    static final Set<String> REACHABLE_BUT_PREEXISTING_DRIFT =
            Set.of("feature-flags");

    /**
     * Allowlist C — declaring contracts this wave did not sweep. Reachable in
     * principle; simply outside the right-sized 8-domain lane. Listed so the
     * partition is COMPLETE and the pin below is load-bearing rather than a stub.
     */
    static final Set<String> DECLARING_NOT_IN_SCOPE = Set.of(
            "billing", "comment-thread", "crud", "file-storage",
            "payment", "report-export", "tag-categorization", "tokenized-securities");

    /** Disk truth: contracts/*.yaml files declaring a {@code total*} count member. */
    static final int DECLARING_CONTRACTS = 19;

    /**
     * The declaring universe, DERIVED FROM DISK — the Java equivalent of
     * {@code grep -lE '^ *total[A-Z][A-Za-z]*:' contracts/*.yaml}, reduced to
     * contract stems. Derived rather than pinned so a new list contract cannot
     * join the catalog without landing in exactly one of the four sets below.
     *
     * <p>The predicate is a member-name CLASS, not a two-name allowlist: the
     * earlier {@code (totalElements|totalPages)} form silently missed
     * {@code search-openapi.yaml}'s {@code totalHits} and made the universe 20
     * instead of 21 (see the class javadoc's "21st declaring contract"
     * section). A name allowlist is exactly the shape that goes stale when a
     * new contract picks a different-but-equivalent member name.
     *
     * <p>Path convention follows the existing precedent in this test tree
     * ({@code WebhookIdempotentTest}: {@code Path.of("..", "templates", …)}) —
     * gradle runs tests with {@code user.dir} == {@code backend/}.
     */
    static Set<String> declaringUniverseFromDisk() throws IOException {
        Path contracts = Path.of("..", "contracts");
        assertThat(Files.isDirectory(contracts))
                .as("contracts/ must be reachable from the backend module (user.dir=%s)",
                        System.getProperty("user.dir"))
                .isTrue();
        Pattern declares = Pattern.compile("(?m)^ *total[A-Z][A-Za-z]*:");
        try (Stream<Path> yamls = Files.list(contracts)) {
            return yamls
                    .filter(p -> p.getFileName().toString().endsWith("-openapi.yaml"))
                    .filter(p -> {
                        try {
                            return declares.matcher(Files.readString(p)).find();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .map(p -> p.getFileName().toString().replace("-openapi.yaml", ""))
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /**
     * One declared envelope member: its OpenAPI {@code type} and, where the
     * contract states one, its {@code format}. Both come from the yaml.
     */
    record DeclaredMember(String type, String format) {}

    /**
     * One binding's contract-declared envelope: the URL the endpoint is published
     * at, the top-level member set its 200 response schema declares, and each
     * member's declared type. All THREE are read from
     * {@code contracts/<stem>-openapi.yaml} on disk.
     */
    record ContractEnvelope(String url, Set<String> members, Map<String, DeclaredMember> declared) {}

    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    /**
     * Resolves a binding against the checked-in contract document, offline and
     * deterministically:
     * <ol>
     *   <li>{@code servers[0].url + contractPath} — the URL to drive;</li>
     *   <li>{@code paths.<contractPath>.<method>.responses.200.content
     *       .application/json.schema.$ref} — the response schema reference;</li>
     *   <li>that schema's {@code required} list — the expected member set.</li>
     * </ol>
     * Every step is asserted rather than defaulted: a contract that no longer
     * declares the path, the 200, the json media type, a {@code $ref} to a
     * component schema, or a {@code required} list fails HERE, so a drifted
     * contract can never degrade the parity check into a weaker one.
     */
    static ContractEnvelope envelopeFromContract(EnvelopeBinding binding) throws IOException {
        String where = binding.contractStem() + "-openapi.yaml";
        Path file = Path.of("..", "contracts", where);
        assertThat(Files.isRegularFile(file))
                .as("contract must exist on disk: %s (user.dir=%s)", file, System.getProperty("user.dir"))
                .isTrue();
        Object doc;
        try (Reader reader = Files.newBufferedReader(file)) {
            doc = new Yaml().load(reader);
        }

        Object servers = descend(doc, where, "servers");
        assertThat(servers).as("%s: servers must be a list", where).isInstanceOf(List.class);
        List<?> serverList = (List<?>) servers;
        assertThat(serverList).as("%s: servers must not be empty", where).isNotEmpty();
        String base = String.valueOf(descend(serverList.get(0), where + "#servers[0]", "url"));

        Object schemaRef = descend(doc, where,
                "paths", binding.contractPath(), binding.httpMethod(),
                "responses", "200", "content", "application/json", "schema", "$ref");
        String ref = String.valueOf(schemaRef);
        assertThat(ref)
                .as("%s: %s %s must answer 200 with a component schema reference",
                        where, binding.httpMethod(), binding.contractPath())
                .startsWith(SCHEMA_REF_PREFIX);
        String schemaName = ref.substring(SCHEMA_REF_PREFIX.length());

        Object envelope = descend(doc, where, "components", "schemas", schemaName);
        String at = where + "#" + schemaName;

        Object requiredNode = descend(envelope, at, "required");
        assertThat(requiredNode).as("%s: required must be a list", at).isInstanceOf(List.class);
        Set<String> required = ((List<?>) requiredNode).stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
        assertThat(required).as("%s: a page envelope must declare its members", at).isNotEmpty();

        Map<?, ?> propertyNodes = asMap(descend(envelope, at, "properties"), at + ".properties");
        Set<String> properties = propertyNodes.keySet().stream()
                .map(String::valueOf)
                .collect(Collectors.toCollection(TreeSet::new));
        // An OPTIONAL envelope member would be an escape hatch: the response could
        // omit it and still "match the contract". Page envelopes declare every
        // member, so required and properties must coincide — and a mutation that
        // renames only one of the two halves is caught right here.
        assertThat(required)
                .as("%s: every declared envelope member must be required", at)
                .isEqualTo(properties);

        // P3-85 — the TYPE axis. Read each member's declared type (and format, where
        // stated). Fail-closed: a member with no `type:` is NOT waved through, because
        // "unknown declared type" would silently degrade the parity check for that
        // member back to the member-name-only assertion this axis exists to strengthen.
        Map<String, DeclaredMember> declared = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : propertyNodes.entrySet()) {
            String member = String.valueOf(entry.getKey());
            Map<?, ?> schema = asMap(entry.getValue(), at + ".properties." + member);
            Object type = valueOfKey(schema, "type");
            assertThat(type)
                    .as("%s.properties.%s: must declare a `type:` — an envelope member whose "
                            + "wire type the contract does not state cannot be checked against "
                            + "the response", at, member)
                    .isNotNull();
            Object format = valueOfKey(schema, "format");
            declared.put(member, new DeclaredMember(
                    String.valueOf(type), format == null ? null : String.valueOf(format)));
        }

        return new ContractEnvelope(base + binding.contractPath(), required, declared);
    }

    /** The value of {@code key} in a parsed yaml mapping, or null when absent. */
    private static Object valueOfKey(Map<?, ?> map, String key) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (String.valueOf(entry.getKey()).equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Walks {@code keys} down a parsed yaml document, failing with the exact
     * location and the keys that ARE present when a step is missing. Keys are
     * compared by {@link String#valueOf} so an unquoted yaml scalar key (notably
     * {@code 200:}, which snakeyaml parses as an Integer) resolves the same as a
     * quoted one.
     */
    private static Object descend(Object root, String where, String... keys) {
        Object node = root;
        StringBuilder at = new StringBuilder(where);
        for (String key : keys) {
            Map<?, ?> map = asMap(node, at.toString());
            Object next = null;
            boolean found = false;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (String.valueOf(entry.getKey()).equals(key)) {
                    next = entry.getValue();
                    found = true;
                    break;
                }
            }
            assertThat(found)
                    .as("%s: missing key '%s' (present: %s)", at, key, map.keySet())
                    .isTrue();
            node = next;
            at.append('.').append(key);
        }
        return node;
    }

    private static Map<?, ?> asMap(Object node, String at) {
        assertThat(node).as("%s: expected a yaml mapping", at).isInstanceOf(Map.class);
        return (Map<?, ?>) node;
    }

    /**
     * The partition is a SET property, not an arithmetic one. Sizes alone are
     * blind to a swap between two allowlists (constant sum, one domain lost and
     * another double-counted), which is exactly the escape a cross-family
     * reviewer demonstrated against the previous sum-only assertion.
     */
    @Test
    void declaringUniverseIsExactlyPartitionedAcrossTheFourSets() throws IOException {
        Set<String> universe = declaringUniverseFromDisk();
        assertThat(universe)
                .as("disk truth: contracts declaring a page/list envelope")
                .hasSize(DECLARING_CONTRACTS);

        Map<String, Set<String>> parts = new LinkedHashMap<>();
        parts.put("SWEPT", SWEPT);
        parts.put("NOT_REACHABLE", NOT_REACHABLE);
        parts.put("REACHABLE_BUT_PREEXISTING_DRIFT", REACHABLE_BUT_PREEXISTING_DRIFT);
        parts.put("DECLARING_NOT_IN_SCOPE", DECLARING_NOT_IN_SCOPE);

        // (1) pairwise disjoint — no domain may be claimed by two sets.
        List<String> names = new ArrayList<>(parts.keySet());
        for (int i = 0; i < names.size(); i++) {
            for (int j = i + 1; j < names.size(); j++) {
                Set<String> overlap = new TreeSet<>(parts.get(names.get(i)));
                overlap.retainAll(parts.get(names.get(j)));
                assertThat(overlap)
                        .as("%s and %s must be disjoint", names.get(i), names.get(j))
                        .isEmpty();
            }
        }

        // (2) union EQUALS the universe — nothing lost, nothing invented.
        Set<String> union = new TreeSet<>();
        parts.values().forEach(union::addAll);
        assertThat(union)
                .as("the four sets must exactly cover the declaring universe")
                .isEqualTo(new TreeSet<>(universe));

        // (3) the documented counts stay pinned (a shrunk lane is a visible edit).
        assertThat(SWEPT).hasSize(9);
        assertThat(NOT_REACHABLE).hasSize(1);
        assertThat(REACHABLE_BUT_PREEXISTING_DRIFT).hasSize(1);
        assertThat(DECLARING_NOT_IN_SCOPE).hasSize(8);
    }

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    private static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    private static String obtainToken(String email, String role) {
        given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        return given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().extract().path("accessToken");
    }

    private static Map<String, Object> bodyOf(Response response) {
        return response.jsonPath().getMap("$");
    }

    private static Set<String> membersOf(Response response) {
        return bodyOf(response).keySet();
    }

    /**
     * P3-85 — the OpenAPI type name for an actual deserialized JSON value, or null when
     * the value is JSON {@code null} (which carries no type of its own).
     *
     * <p>{@code integer} is reported for an integral number and {@code number} for a
     * fractional one; the comparison below then applies JSON Schema's rule that a declared
     * {@code number} also accepts an integral value, but a declared {@code integer} does
     * NOT accept a fractional one.
     */
    private static String jsonTypeOf(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof List) {
            return "array";
        }
        if (value instanceof Map) {
            return "object";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
            return "integer";
        }
        if (value instanceof Number) {
            return "number";
        }
        return "string";
    }

    /**
     * P3-85 — asserts one member's live value against its contract-declared type.
     *
     * <p>Null is fail-closed rather than skipped: none of the swept envelopes declares a
     * nullable member, so a null on the wire is a real mismatch, and treating it as
     * "unverifiable" would hand every member a way out of this axis.
     */
    private static void assertMemberTypeMatchesContract(
            String url, String member, DeclaredMember declared, Object value) {
        String actual = jsonTypeOf(value);
        assertThat(actual)
                .as("%s.%s: contract declares type '%s' but the response emitted JSON null",
                        url, member, declared.type())
                .isNotNull();

        boolean matches = declared.type().equals(actual)
                // JSON Schema: an integral value satisfies a declared `number`.
                || ("number".equals(declared.type()) && "integer".equals(actual));
        assertThat(matches)
                .as("%s.%s: contract declares type '%s' but the response emitted %s (%s)",
                        url, member, declared.type(), actual,
                        value.getClass().getSimpleName())
                .isTrue();

        if ("int32".equals(declared.format()) || "int64".equals(declared.format())) {
            // Disclosed non-tightness: every swept member that carries a format declares
            // int64, and any Integer/Long the harness deserializes fits — so this arm is
            // currently vacuous in practice. It is written for the case the axis exists to
            // catch (a value overflowing a declared int32) rather than left out, and the
            // TYPE comparison above is what carries the mutation lock.
            BigInteger n = new BigInteger(String.valueOf(value));
            BigInteger max = "int32".equals(declared.format())
                    ? BigInteger.valueOf(Integer.MAX_VALUE) : BigInteger.valueOf(Long.MAX_VALUE);
            BigInteger min = "int32".equals(declared.format())
                    ? BigInteger.valueOf(Integer.MIN_VALUE) : BigInteger.valueOf(Long.MIN_VALUE);
            assertThat(n)
                    .as("%s.%s: contract declares format '%s'", url, member, declared.format())
                    .isBetween(min, max);
        }
    }

    private static EnvelopeBinding binding(String id) {
        EnvelopeBinding binding = BINDINGS.get(id);
        assertThat(binding).as("unknown binding id '%s' (known: %s)", id, BINDINGS.keySet()).isNotNull();
        return binding;
    }

    private static void assertMatchesContract(ContractEnvelope expected, Response response) {
        Map<String, Object> body = bodyOf(response);
        assertThat(body.keySet())
            .as("%s must emit exactly the member set its contract declares", expected.url())
            .containsExactlyInAnyOrderElementsOf(expected.members());

        // P3-85 — member NAMES agreeing is not the same as member TYPES agreeing. Before
        // this axis, flipping a declared `totalPages` from integer to string changed no
        // member name and the sweep stayed green, so the contract could promise one wire
        // type while the code emitted another.
        expected.declared().forEach((member, declared) ->
                assertMemberTypeMatchesContract(expected.url(), member, declared, body.get(member)));
    }

    /**
     * Drives one GET binding at the URL its contract publishes it at, as a real
     * authorized principal, and compares the emitted member set against the
     * contract-declared one. Nothing about the expected shape is written here.
     */
    private void assertGetEnvelopeMatchesContract(String bindingId, String emailPrefix, String role)
            throws IOException {
        ContractEnvelope expected = envelopeFromContract(binding(bindingId));
        String token = obtainToken(freshEmail(emailPrefix), role);
        Response response = given().header("Authorization", "Bearer " + token)
            .when().get(expected.url())
            .then().statusCode(200).extract().response();
        assertMatchesContract(expected, response);
    }

    @Test
    void auditLogList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("audit-log-list", "sweep-audit", "MEMBER");
    }

    @Test
    void notificationList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("notification-list", "sweep-notif", "MEMBER");
    }

    @Test
    void sessionList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("session-list", "sweep-session", "MEMBER");
    }

    @Test
    void favoriteList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("favorite-list", "sweep-fav", "MEMBER");
    }

    @Test
    void activityFeedList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("activity-feed-list", "sweep-activity", "MEMBER");
    }

    @Test
    void apiKeyList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("api-key-list", "sweep-apikey", "MEMBER");
    }

    @Test
    void approvalOwnList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("approval-own-list", "sweep-approval-own", "MEMBER");
    }

    @Test
    void approvalInbox_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("approval-inbox", "sweep-approval-inbox", "MEMBER");
    }

    /**
     * The 21st declaring contract — see the class javadoc. Two reasons the old
     * sweep never reached it: the total member is spelled {@code totalHits},
     * and the endpoint is a POST (the query lives in the body). Both are
     * legitimate; neither makes the envelope any less of a page envelope. The
     * POST body is the only thing this method states that the GET helper cannot.
     */
    @Test
    void searchResultPage_envelopeMemberSet_matchesContract() throws IOException {
        ContractEnvelope expected = envelopeFromContract(binding("search-result-page"));
        String token = obtainToken(freshEmail("sweep-search"), "MEMBER");
        Response response = given().header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .body("{\"query\":\"page-envelope-sweep\"}")
            .when().post(expected.url())
            .then().statusCode(200).extract().response();
        assertMatchesContract(expected, response);
    }

    @Test
    void identityVerificationAdminList_envelopeMemberSet_matchesContract() throws IOException {
        assertGetEnvelopeMatchesContract("identity-verification-admin-list", "sweep-idv-admin", "ADMIN");
    }
}
