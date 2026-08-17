package com.ax.template.authblueprint.realtime;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.restassured.RestAssured;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Black-box compliance verification for realtime-policy-l0 (specs/realtime-policy-l0.yaml),
 * realized SSE-FIRST over MVC {@link org.springframework.web.servlet.mvc.method.annotation.SseEmitter}.
 *
 * <p>Covers the 6 mechanically-testable items (RT-PROTOCOL-001 is verification_type: review —
 * the RECIPE.md {@code realtime_protocol} declaration is documented in the L4 README, not
 * mechanically tested per the spec). RestAssured + a raw {@link HttpURLConnection} chunked
 * reader over a random port — portable, no MockMvc, no WebFlux, no WebSocket server.
 *
 * <p>SSE is chunked HTTP: an open subscription never closes on its own, so the stream reader
 * runs on a background thread with a bounded socket read-timeout and the test publishes via
 * the demo POST hook. {@code text/event-stream} frames are {@code id:}/{@code data:} lines —
 * the reader collects them until it has the expected count or times out.
 */
// R22 aggregate-test isolation: this is a new RANDOM_PORT @SpringBootTest sharing the
// default-properties context-cache key. Under the aggregate `./gradlew test` run a new context
// pushes the Spring TestContext ContextCache past its capacity-32 LRU, evicting a sibling
// default-properties context whose Hikari pool is then shut down while another test still holds
// it (observed: SessionRevocationCheckTest 4x UndeclaredThrowableException). BEFORE_CLASS forces
// this class to boot a FRESH context (never reusing a possibly-evicted one) and frees a cache
// slot before it runs, the SAME proven lever I18nPolicyComplianceTest uses to coexist with
// SessionRevocationCheckTest in the aggregate — confined to THIS test only (no sibling/global
// change). (An earlier AFTER_CLASS variant re-tripped the eviction once this class grew a longer
// backpressure-race test; BEFORE_CLASS matches the i18n precedent exactly and is stable.)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class RealtimePolicyComplianceTest {

    @LocalServerPort int port;

    @Autowired MeterRegistry meterRegistry;

    private String baseUrl;

    @BeforeEach
    void setup() {
        baseUrl = "http://localhost:" + port;
    }

    // ── RT-CHANNEL-AUTH-001 ──────────────────────────────────────────────────
    @Test
    @Tag("REALTIME")
    @Tag("RT-CHANNEL-AUTH-001")
    void rt_CHANNEL_AUTH_001_unauthenticatedSubscribeRejected() {
        // SSE subscribe is a plain HTTP GET — the EXISTING security chain authenticates it.
        // No bearer token → 401 from the chain BEFORE the controller (no "WS bypass").
        given()
            .accept("text/event-stream")
        .when().get("/api/realtime/topics/anyscope/notifications")
        .then().statusCode(401);
    }

    // ── RT-CHANNEL-AUTH-002 ──────────────────────────────────────────────────
    @Test
    @Tag("REALTIME")
    @Tag("RT-CHANNEL-AUTH-002")
    void rt_CHANNEL_AUTH_002_crossScopeTopicRejected() {
        String token = obtainToken("auth002");
        // A subscribe whose {tenantScope} segment != caller's resolved scope → 403.
        given()
            .header("Authorization", "Bearer " + token)
            .accept("text/event-stream")
        .when().get("/api/realtime/topics/some-other-tenant/notifications")
        .then().statusCode(403)
            .body("code", org.hamcrest.Matchers.equalTo(
                CrossTenantSubscriptionException.CODE));
    }

    // ── RT-FANOUT-001 ────────────────────────────────────────────────────────
    @Test
    @Tag("REALTIME")
    @Tag("RT-FANOUT-001")
    void rt_FANOUT_001_audienceFilteredBeforeBroadcast() throws Exception {
        // Three subscribers A, B, C on the same topic; publish addressed to [A, B];
        // C must NOT receive the frame at the transport layer.
        Caller a = newCaller("fanA");
        Caller b = newCaller("fanB");
        Caller c = newCaller("fanC");
        // Topic MUST be in the bounded allowlist (RT-OBSERVABILITY-001 cardinality bound).
        // Each test uses a DISTINCT allowlisted topic so per-topic state (retention / metrics)
        // does not contend across the serially-run tests.
        String topic = "notification";

        // All three subscribe to THEIR OWN scope (RT-CHANNEL-AUTH-002), same topic name.
        SseStream sa = open(a, topic, null, 1);
        SseStream sb = open(b, topic, null, 1);
        SseStream sc = open(c, topic, null, 1);

        // Publish addressed to A and B only. Publisher = A (its own scope).
        publish(a, topic, 1L, List.of(a.userId, b.userId), "hello-AB");

        // A and B receive the frame; C does not (bounded wait, then assert C empty).
        assertThat(sa.await(1)).as("A is in audience").isTrue();
        assertThat(sb.await(1)).as("B is in audience").isTrue();
        assertThat(sc.await(1)).as("C is NOT in audience — never delivered").isFalse();
        assertThat(sa.events()).anyMatch(e -> e.data.contains("hello-AB"));
        assertThat(sc.events()).isEmpty();

        sa.close(); sb.close(); sc.close();
    }

    // ── RT-BACKPRESSURE-001 ──────────────────────────────────────────────────
    @Test
    @Tag("REALTIME")
    @Tag("RT-BACKPRESSURE-001")
    void rt_BACKPRESSURE_001_slowConsumerDisconnectedAtThreshold() throws Exception {
        // A consumer that never reads back-pressures its pending queue; once the queue
        // depth crosses the manifest threshold the server completes-with-error +
        // disconnects (the stream ends). Threshold is the default RealtimeProperties value.
        Caller s = newCaller("bp");
        String topic = "audit"; // distinct allowlisted topic (see RT-FANOUT note)

        // Open but DO NOT drain — a stalled reader. We let the server push past threshold.
        SseStream slow = open(s, topic, null, Integer.MAX_VALUE);
        slow.stall(); // stop draining the socket so the server-side write queue fills.

        // Publish far past the threshold (default queue capacity 50) with non-trivial
        // payloads so the server-side TCP buffer + bounded write queue overflow
        // deterministically and the offer fails → complete-with-error + disconnect.
        String filler = "x".repeat(2048);
        for (long i = 1; i <= 5000; i++) {
            publish(s, topic, i, List.of(s.userId), "frame-" + i + "-" + filler);
        }

        // The disconnect surfaces either as the stream ending OR the backpressure meter.
        boolean disconnected = slow.awaitClosed(5);
        Counter bp = findCounter(RealtimeMetrics.DISCONNECT_RATE,
            RealtimeMetrics.TAG_REASON, RealtimeMetrics.DisconnectReason.BACKPRESSURE.label());
        assertThat(disconnected || (bp != null && bp.count() >= 1.0))
            .as("slow consumer disconnected at queue threshold (stream ended or backpressure meter fired)")
            .isTrue();

        slow.close();
    }

    // ── RT-RECONNECT-001 ─────────────────────────────────────────────────────
    @Test
    @Tag("REALTIME")
    @Tag("RT-RECONNECT-001")
    void rt_RECONNECT_001_lastEventIdResumesWithoutGap() throws Exception {
        Caller r = newCaller("recon");
        String topic = "payment"; // distinct allowlisted topic (see RT-FANOUT note)

        // Publish events 1..5 with NO live subscriber — all retained in the ring buffer.
        for (long i = 1; i <= 5; i++) {
            publish(r, topic, i, List.of(r.userId), "evt-" + i);
        }

        // Reconnect with Last-Event-ID: 2 → must replay 3, 4, 5 gap-free, none ≤ 2.
        SseStream resumed = open(r, topic, 2L, 3);
        assertThat(resumed.await(3)).as("3 retained events replayed").isTrue();

        List<Long> ids = new ArrayList<>();
        for (SseEvent e : resumed.events()) {
            ids.add(Long.parseLong(e.id));
        }
        assertThat(ids).containsExactly(3L, 4L, 5L); // gap-free, strictly > cursor
        assertThat(ids).allMatch(id -> id > 2L);

        resumed.close();
    }

    // ── RT-OBSERVABILITY-001 ─────────────────────────────────────────────────
    @Test
    @Tag("REALTIME")
    @Tag("RT-OBSERVABILITY-001")
    void rt_OBSERVABILITY_001_threeCanonicalMetricsBoundedNonPii() throws Exception {
        Caller o = newCaller("obs");
        String topic = "system"; // distinct allowlisted topic (see RT-FANOUT note)

        SseStream stream = open(o, topic, null, 3);
        // Send 3 frames addressed to the subscriber.
        for (long i = 1; i <= 3; i++) {
            publish(o, topic, i, List.of(o.userId), "m-" + i);
        }
        assertThat(stream.await(3)).isTrue();

        // (a) active_subscribers_count gauge present for this channel.
        boolean hasActive = meterRegistry.getMeters().stream()
            .anyMatch(m -> m.getId().getName().equals(RealtimeMetrics.ACTIVE_SUBSCRIBERS_COUNT));
        // (b) messages_sent_total counter present — channel tag is the BARE topic
        // (bounded), tenant is the resolved scope, neither carries the caller id.
        Counter sent = findCounter(RealtimeMetrics.MESSAGES_SENT_TOTAL,
            RealtimeMetrics.TAG_CHANNEL, topic);
        // (c) disconnect_rate counter exists after we close the stream. Closing the client
        // socket is detected by the server on its next write, so publish a few more frames
        // to force the drain worker's send() to fail → CLIENT disconnect → meter fires.
        stream.close();
        for (long i = 4; i <= 8; i++) {
            publish(o, topic, i, List.of(o.userId), "after-close-" + i);
            Thread.sleep(50);
        }
        boolean disconnectMeterEventuallyPresent = awaitMeter(RealtimeMetrics.DISCONNECT_RATE, 5);

        assertThat(hasActive).as("active_subscribers_count gauge emitted").isTrue();
        assertThat(sent).as("messages_sent_total counter emitted").isNotNull();
        assertThat(sent.count()).isGreaterThanOrEqualTo(3.0);
        assertThat(disconnectMeterEventuallyPresent).as("disconnect_rate counter emitted").isTrue();

        // BOUNDED, NON-PII labels only: assert NO realtime meter carries a tag whose value
        // is the caller id (PII / unbounded). Allowed tag keys: channel, tenant, reason.
        List<Meter> realtimeMeters = meterRegistry.getMeters().stream()
            .filter(m -> isRealtimeMeter(m.getId().getName()))
            .toList();
        assertThat(realtimeMeters).isNotEmpty();
        for (Meter m : realtimeMeters) {
            for (io.micrometer.core.instrument.Tag t : m.getId().getTags()) {
                assertThat(t.getKey()).isIn(
                    RealtimeMetrics.TAG_CHANNEL, RealtimeMetrics.TAG_TENANT, RealtimeMetrics.TAG_REASON);
                assertThat(t.getValue())
                    .as("no caller-id / PII leaks into a metric tag value")
                    .doesNotContain(o.userId);
            }
        }
    }

    // ── RT-OBSERVABILITY-001 (regression: metric-cardinality DoS — FINDING 1) ──
    @Test
    @Tag("REALTIME")
    @Tag("RT-OBSERVABILITY-001")
    void rt_OBSERVABILITY_001_unknownTopicRejected404_noMetricSeriesCreated() {
        String token = obtainToken("unktopic");
        String unknownTopic = "evil-" + UUID.randomUUID(); // not in the bounded allowlist

        // No metric series for this topic before the attempt.
        assertThat(channelMeterExists(RealtimeMetrics.ACTIVE_SUBSCRIBERS_COUNT, unknownTopic))
            .as("no active_subscribers_count series for the unknown topic yet").isFalse();

        // Subscribe to an unknown topic → 404 (NOT a registry entry, NOT a metric series).
        given()
            .header("Authorization", "Bearer " + token)
            .accept("text/event-stream")
        .when().get("/api/realtime/topics/" + SCOPE + "/" + unknownTopic)
        .then().statusCode(404)
            .body("code", org.hamcrest.Matchers.equalTo(UnknownTopicException.CODE));

        // Publish to an unknown topic → 404 as well (same cardinality bound on the write path).
        given()
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .body("{\"eventId\":1,\"audience\":[\"x\"],\"payload\":{}}")
        .when().post("/api/realtime/topics/" + SCOPE + "/" + unknownTopic + "/publish")
        .then().statusCode(404)
            .body("code", org.hamcrest.Matchers.equalTo(UnknownTopicException.CODE));

        // The rejection happened BEFORE any registry entry or Micrometer series was created:
        // no channel-labelled meter exists for the attacker-supplied topic.
        assertThat(channelMeterExists(RealtimeMetrics.ACTIVE_SUBSCRIBERS_COUNT, unknownTopic))
            .as("unknown topic must NOT create an active_subscribers_count series").isFalse();
        assertThat(channelMeterExists(RealtimeMetrics.MESSAGES_SENT_TOTAL, unknownTopic))
            .as("unknown topic must NOT create a messages_sent_total series").isFalse();
        assertThat(channelMeterExists(RealtimeMetrics.DISCONNECT_RATE, unknownTopic))
            .as("unknown topic must NOT create a disconnect_rate series").isFalse();
    }

    // ── RT-OBSERVABILITY-001 (regression: disconnect-reason race — FINDING 2) ──
    @Test
    @Tag("REALTIME")
    @Tag("RT-OBSERVABILITY-001")
    void rt_OBSERVABILITY_001_disconnectRecordedExactlyOnceUnderRace() throws Exception {
        // A slow consumer trips the BACKPRESSURE disconnect while its drain worker can also hit
        // a CLIENT send-error for the SAME subscriber. The one-shot CAS must elect a single
        // removal winner: disconnect_rate increments exactly once total for this isolated topic,
        // and active_subscribers_count returns to 0 (no double-decrement → never negative).
        Caller s = newCaller("race");
        String topic = "system"; // allowlisted; isolated from other tests' active-count usage
        // We measure deltas so prior tests sharing this topic don't perturb the assertion.
        double disconnectBefore = totalDisconnectCount(topic);

        SseStream slow = open(s, topic, null, Integer.MAX_VALUE);
        slow.stall(); // stop draining → server-side write queue fills → backpressure disconnect.

        String filler = "x".repeat(4096);
        for (long i = 1; i <= 2000; i++) {
            publish(s, topic, i, List.of(s.userId), "f-" + i + "-" + filler);
        }

        // Poll for the disconnect to be recorded (the stalled reader cannot down its own close
        // latch, so we assert on the server-side invariant: a disconnect_rate increment for this
        // channel). Then give any LOSING concurrent path a moment to (no-op) attempt removal.
        boolean recorded = false;
        long deadline = System.currentTimeMillis() + 6000L;
        while (System.currentTimeMillis() < deadline) {
            if (totalDisconnectCount(topic) - disconnectBefore >= 1.0) { recorded = true; break; }
            Thread.sleep(50);
        }
        assertThat(recorded).as("slow consumer disconnect was recorded").isTrue();
        Thread.sleep(300);

        double disconnectDelta = totalDisconnectCount(topic) - disconnectBefore;
        // EXACTLY ONE disconnect recorded for this subscriber across all reasons — not 0
        // (lost entirely) and not 2 (double-counted by both the backpressure + drain-error path).
        assertThat(disconnectDelta)
            .as("disconnect recorded exactly once under the backpressure/drain-error race")
            .isEqualTo(1.0);

        // active_subscribers_count for this channel never went negative (no double-decrement).
        double active = activeGauge(topic);
        assertThat(active).as("active_subscribers_count must not be driven negative").isGreaterThanOrEqualTo(0.0);

        slow.close();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean channelMeterExists(String name, String channel) {
        return meterRegistry.find(name).tag(RealtimeMetrics.TAG_CHANNEL, channel).meters().size() > 0;
    }

    /** Sum of disconnect_rate counters across all reasons for one channel. */
    private double totalDisconnectCount(String channel) {
        return meterRegistry.find(RealtimeMetrics.DISCONNECT_RATE)
            .tag(RealtimeMetrics.TAG_CHANNEL, channel).counters().stream()
            .mapToDouble(Counter::count).sum();
    }

    /** Current value of the active_subscribers_count gauge for one channel (0 if absent). */
    private double activeGauge(String channel) {
        var gauge = meterRegistry.find(RealtimeMetrics.ACTIVE_SUBSCRIBERS_COUNT)
            .tag(RealtimeMetrics.TAG_CHANNEL, channel).gauge();
        return gauge == null ? 0.0 : gauge.value();
    }

    private boolean isRealtimeMeter(String name) {
        return name.equals(RealtimeMetrics.ACTIVE_SUBSCRIBERS_COUNT)
            || name.equals(RealtimeMetrics.MESSAGES_SENT_TOTAL)
            || name.equals(RealtimeMetrics.DISCONNECT_RATE);
    }

    private Counter findCounter(String name, String tagKey, String tagValue) {
        return meterRegistry.find(name).tag(tagKey, tagValue).counter();
    }

    private boolean awaitMeter(String name, int seconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            boolean present = meterRegistry.getMeters().stream()
                .anyMatch(m -> m.getId().getName().equals(name));
            if (present) return true;
            Thread.sleep(50);
        }
        return false;
    }

    private String obtainToken(String prefix) {
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"MEMBER\"}")
        .when().post("/api/auth/email/signup");
        return given().header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login")
        .then().statusCode(200).extract().path("accessToken");
    }

    /** A caller = its bearer token + its resolved userId (Authentication.getName()). */
    private Caller newCaller(String prefix) {
        String token = obtainToken(prefix);
        String userId = given().header("Authorization", "Bearer " + token)
            .when().get("/api/api-keys/scope-probe/whoami")
            .then().statusCode(200).extract().path("userId");
        return new Caller(token, userId);
    }

    // Single-tenant reference: every authenticated caller resolves to ONE shared tenant
    // scope (RealtimeController.RESOLVED_TENANT_SCOPE) so multiple callers can fan out on
    // one topic. Audience membership is keyed by userId; the path scope is the tenant.
    private static final String SCOPE = "default";

    private void publish(Caller c, String topic, long eventId, List<String> audience, String body) {
        StringBuilder aud = new StringBuilder("[");
        for (int i = 0; i < audience.size(); i++) {
            if (i > 0) aud.append(",");
            aud.append("\"").append(audience.get(i)).append("\"");
        }
        aud.append("]");
        given()
            .header("Authorization", "Bearer " + c.token)
            .header("Content-Type", "application/json")
            .body("{\"eventId\":" + eventId + ",\"audience\":" + aud
                + ",\"payload\":{\"body\":\"" + body + "\"}}")
        .when().post("/api/realtime/topics/" + SCOPE + "/" + topic + "/publish")
        .then().statusCode(200);
    }

    /** Open an SSE subscription on a background thread and collect frames. */
    private SseStream open(Caller c, String topic, Long lastEventId, int expected)
            throws IOException, InterruptedException {
        String url = baseUrl + "/api/realtime/topics/" + SCOPE + "/" + topic;
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + c.token);
        conn.setRequestProperty("Accept", "text/event-stream");
        if (lastEventId != null) {
            conn.setRequestProperty("Last-Event-ID", Long.toString(lastEventId));
        }
        conn.setReadTimeout(1500); // bounded — never blocks the test forever
        conn.connect();
        SseStream stream = new SseStream(conn, expected);
        stream.start();
        // Wait for the server's ":subscribed" handshake comment so the subscription is
        // registered before the caller publishes — deterministic, no real-time sleep.
        stream.awaitReady(3);
        return stream;
    }

    private record Caller(String token, String userId) {}

    private record SseEvent(String id, String data) {}

    /** Background SSE reader: parses id:/data: lines, counts frames, supports stall+close. */
    private static final class SseStream {
        private final HttpURLConnection conn;
        private final int expected;
        private final CountDownLatch frames;
        private final CountDownLatch ready = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private final List<SseEvent> received = new CopyOnWriteArrayList<>();
        private final AtomicReference<Thread> reader = new AtomicReference<>();
        private volatile boolean stalled = false;

        SseStream(HttpURLConnection conn, int expected) {
            this.conn = conn;
            this.expected = expected;
            this.frames = new CountDownLatch(Math.max(expected, 0));
        }

        void start() {
            Thread t = new Thread(this::run, "sse-reader");
            t.setDaemon(true);
            reader.set(t);
            t.start();
        }

        private void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                String curId = null;
                StringBuilder curData = new StringBuilder();
                while ((line = in.readLine()) != null) {
                    if (line.startsWith(":")) {
                        // SSE comment — the ":subscribed" readiness handshake (or keep-alive).
                        ready.countDown();
                        continue;
                    }
                    if (stalled) {
                        // Stop draining so the server-side write queue fills (backpressure).
                        Thread.sleep(50_000);
                        continue;
                    }
                    if (line.startsWith("id:")) {
                        curId = line.substring(3).trim();
                    } else if (line.startsWith("data:")) {
                        curData.append(line.substring(5).trim());
                    } else if (line.isEmpty()) {
                        if (curId != null || curData.length() > 0) {
                            received.add(new SseEvent(curId, curData.toString()));
                            frames.countDown();
                            curId = null;
                            curData = new StringBuilder();
                        }
                    }
                }
            } catch (IOException | InterruptedException ignored) {
                // socket read-timeout / server disconnect / interrupt — stream ended.
            } finally {
                closed.countDown();
            }
        }

        void stall() {
            this.stalled = true;
        }

        boolean awaitReady(int seconds) throws InterruptedException {
            return ready.await(seconds, TimeUnit.SECONDS);
        }

        boolean await(int seconds) throws InterruptedException {
            return frames.await(seconds, TimeUnit.SECONDS);
        }

        boolean awaitClosed(int seconds) throws InterruptedException {
            return closed.await(seconds, TimeUnit.SECONDS);
        }

        List<SseEvent> events() {
            return List.copyOf(received);
        }

        void close() {
            Thread t = reader.get();
            if (t != null) t.interrupt();
            conn.disconnect();
        }
    }
}
