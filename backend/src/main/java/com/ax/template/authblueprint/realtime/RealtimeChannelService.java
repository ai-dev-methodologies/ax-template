package com.ax.template.authblueprint.realtime;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ax.template.authblueprint.realtime.RealtimeMetrics.DisconnectReason;

/**
 * In-memory SSE channel registry realizing realtime-policy-l0 (specs/realtime-policy-l0.yaml)
 * over MVC {@link SseEmitter} — plain chunked HTTP, so subscriptions go through the existing
 * Spring Security chain (RT-CHANNEL-AUTH-001) and are RestAssured-testable. NO WebFlux, NO
 * WebSocket server: SSE is the canonical reference; the WebSocket path is documented in the
 * L4 README as the parallel option.
 *
 * <p>Per-topic state:
 * <ul>
 *   <li><b>subscribers</b> — live {@link Subscriber} list; fan-out filters by audience
 *       BEFORE emit (RT-FANOUT-001), never "send to all then filter at the client".</li>
 *   <li><b>retention</b> — a bounded ring buffer keyed by a monotonic event id so a
 *       reconnect with {@code Last-Event-ID} replays gap-free (RT-RECONNECT-001).</li>
 *   <li><b>per-subscriber bounded write queue</b> — capacity = manifest
 *       {@code queueThreshold}; fan-out OFFERS to the queue (never blocks the publisher),
 *       a per-subscriber drain worker performs the blocking servlet {@code send}. If the
 *       queue is full (slow consumer) the offer fails → complete-with-error + disconnect
 *       (RT-BACKPRESSURE-001, Project Reactor bounded-buffer / drop-by-disconnect concept).
 *       This is the key reason a slow consumer cannot back-pressure the publisher or other
 *       subscribers — the publisher only ever does a non-blocking enqueue.</li>
 * </ul>
 *
 * <p>RT-OBSERVABILITY-001: {@link RealtimeMetrics} records active subscribers / messages
 * sent / disconnect-by-reason with bounded, non-PII labels only.
 */
@Service
public class RealtimeChannelService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeChannelService.class);

    private final RealtimeProperties properties;
    private final RealtimeMetrics metrics;
    // One daemon drain thread per live subscriber; a slow consumer parks only its own thread.
    private final ExecutorService drainPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "realtime-drain");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, CopyOnWriteArrayList<Subscriber>> subscribersByTopic = new ConcurrentHashMap<>();
    private final Map<String, Retention> retentionByTopic = new ConcurrentHashMap<>();

    public RealtimeChannelService(RealtimeProperties properties, RealtimeMetrics metrics) {
        this.properties = properties;
        this.metrics = metrics;
    }

    @PreDestroy
    void shutdown() {
        drainPool.shutdownNow();
    }

    /**
     * RT-CHANNEL-AUTH-001 (the security chain has already authenticated this GET) +
     * RT-RECONNECT-001. Registers a new SSE subscriber on {@code topic} and, if
     * {@code lastEventId} is non-null, replays every retained event with id greater
     * than the cursor BEFORE returning the live emitter — so there is no gap between
     * the disconnect point and the resumed stream.
     *
     * @param audienceMembership the audience tokens the caller belongs to (used by
     *                           {@link #publish} to decide visibility). Always includes
     *                           the caller id itself.
     */
    public SseEmitter subscribe(String tenantScope, String topic, String callerId,
                                Set<String> audienceMembership, Long lastEventId) {
        String key = registryKey(tenantScope, topic);
        SseEmitter emitter = new SseEmitter(0L); // no servlet-level timeout; policy is queue-based
        Subscriber sub = new Subscriber(callerId, Set.copyOf(audienceMembership), emitter,
            new ArrayBlockingQueue<>(Math.max(properties.getQueueThreshold(), 1)), topic, tenantScope);
        CopyOnWriteArrayList<Subscriber> subs =
            subscribersByTopic.computeIfAbsent(key, t -> new CopyOnWriteArrayList<>());
        subs.add(sub);
        metrics.activeSubscribers(topic, tenantScope).incrementAndGet();

        emitter.onCompletion(() -> remove(topic, sub, DisconnectReason.COMPLETE));
        emitter.onTimeout(() -> {
            remove(topic, sub, DisconnectReason.TIMEOUT);
            emitter.complete();
        });
        emitter.onError(ex -> remove(topic, sub, DisconnectReason.CLIENT));

        // Start the per-subscriber blocking-drain worker.
        drainPool.submit(() -> drain(key, sub));

        // Subscription-ready handshake: an SSE comment line (": ...") that is NOT a data
        // frame (W3C SSE §9.1 — comment lines are ignored by the EventSource client) but
        // flushes the response so a client knows the subscription is registered before it
        // triggers a publish. Eliminates the subscribe/publish race without a real-time
        // sleep; never counted as a delivered message.
        try {
            emitter.send(SseEmitter.event().comment("subscribed"));
        } catch (IOException ex) {
            remove(topic, sub, DisconnectReason.CLIENT);
            emitter.completeWithError(ex);
            return emitter;
        }

        // RT-RECONNECT-001 — gap-free replay from the retention window (enqueued in order).
        if (lastEventId != null) {
            for (RetainedEvent ev : retentionFor(key).since(lastEventId)) {
                if (isVisibleTo(sub, ev.audience())) {
                    enqueue(key, sub, ev);
                }
            }
        }
        return emitter;
    }

    /**
     * RT-FANOUT-001 — resolve audience FIRST, then fan-out only to matching subscribers.
     * A misbehaving client cannot suppress a frame the server never sent it. The event is
     * retained (RT-RECONNECT-001) regardless of who is currently connected so a later
     * reconnect can replay it. The fan-out only ENQUEUES (non-blocking) per subscriber, so
     * a slow consumer never back-pressures the publisher or its peers.
     *
     * @param audience the set of audience tokens this event is addressed to; a subscriber
     *                 receives the frame iff its membership intersects this set.
     * @param eventId  a monotonic id supplied by the caller (test/demo) — the retention
     *                 ring is keyed by it; reconnect resumes from {@code Last-Event-ID}.
     */
    public void publish(String tenantScope, String topic, Set<String> audience,
                        long eventId, Map<String, Object> payload) {
        String key = registryKey(tenantScope, topic);
        RetainedEvent ev = new RetainedEvent(eventId, Set.copyOf(audience), payload);
        retentionFor(key).add(ev);

        List<Subscriber> subs = subscribersByTopic.getOrDefault(key, new CopyOnWriteArrayList<>());
        for (Subscriber sub : subs) {
            if (isVisibleTo(sub, audience)) { // AUDIENCE FILTER BEFORE EMIT
                enqueue(key, sub, ev);
            }
        }
    }

    private String registryKey(String tenantScope, String topic) {
        return tenantScope + "/" + topic;
    }

    /** Visible iff the event audience intersects the subscriber's membership. */
    private boolean isVisibleTo(Subscriber sub, Set<String> audience) {
        for (String token : sub.membership()) {
            if (audience.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * RT-BACKPRESSURE-001 — non-blocking enqueue onto the bounded per-subscriber write
     * queue. If the queue is full the consumer is too slow to keep up: complete-with-error
     * + disconnect rather than buffer unboundedly or block the publisher.
     */
    private void enqueue(String key, Subscriber sub, RetainedEvent ev) {
        if (!sub.queue().offer(ev)) {
            disconnectSlowConsumer(key, sub);
        }
    }

    /** Per-subscriber drain loop: blocking servlet write happens HERE, never on publish. */
    private void drain(String key, Subscriber sub) {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                RetainedEvent ev = sub.queue().poll(1, TimeUnit.SECONDS);
                if (ev == null) {
                    if (!isLive(key, sub)) {
                        return; // subscriber removed (disconnect / complete) — stop draining.
                    }
                    continue;
                }
                sub.emitter().send(SseEmitter.event()
                    .id(Long.toString(ev.id()))
                    .name("message")
                    .data(ev.payload()));
                metrics.recordMessageSent(sub.channel(), sub.tenant());
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (IOException | IllegalStateException ex) {
            // Broken pipe / already-completed — treat as a client-side disconnect.
            if (remove(key, sub, DisconnectReason.CLIENT)) {
                try {
                    sub.emitter().completeWithError(ex);
                } catch (RuntimeException ignored) {
                    // emitter already terminal.
                }
            }
        }
    }

    private boolean isLive(String key, Subscriber sub) {
        // The disconnected CAS flag is the single source of truth (a removal sets it before
        // detaching from the list), so the drain worker stops promptly once any path disconnects.
        return !sub.disconnected().get();
    }

    private void disconnectSlowConsumer(String key, Subscriber sub) {
        if (!remove(key, sub, DisconnectReason.BACKPRESSURE)) {
            return; // already removed by a concurrent path; disconnect once.
        }
        log.warn("realtime backpressure disconnect: channel={} (write queue full at threshold={})",
            sub.channel(), properties.getQueueThreshold());
        try {
            sub.emitter().completeWithError(
                new IllegalStateException("slow consumer disconnected: write queue exceeded "
                    + properties.getQueueThreshold()));
        } catch (RuntimeException ignored) {
            // emitter already terminal.
        }
    }

    /**
     * Remove a subscriber EXACTLY ONCE and record the disconnect reason atomically with the
     * activeSubscribers decrement (RT-OBSERVABILITY-001 correctness). The race: a slow
     * consumer can trip {@link #disconnectSlowConsumer} (reason=BACKPRESSURE on queue.offer
     * failure) while the drain worker concurrently hits an IOException (reason=CLIENT) for the
     * SAME subscriber. The per-subscriber {@link Subscriber#disconnected} CAS guarantees the
     * FIRST caller wins: it performs the registry removal, the activeSubscribers decrement, AND
     * records disconnect_rate with ITS true reason — paired, never split. Later callers are
     * no-ops (no double-decrement, no misattributed/duplicate disconnect_rate). The
     * CopyOnWriteArrayList.remove() is no longer the winner-selector (it could succeed after the
     * losing path already lost the CAS); the AtomicBoolean is the single source of truth.
     */
    private boolean remove(String key, Subscriber sub, DisconnectReason reason) {
        if (!sub.disconnected().compareAndSet(false, true)) {
            return false; // a concurrent path already won and recorded the disconnect.
        }
        CopyOnWriteArrayList<Subscriber> subs = subscribersByTopic.get(key);
        if (subs != null) {
            subs.remove(sub);
        }
        metrics.activeSubscribers(sub.channel(), sub.tenant()).decrementAndGet();
        metrics.recordDisconnect(sub.channel(), sub.tenant(), reason);
        return true;
    }

    private Retention retentionFor(String key) {
        return retentionByTopic.computeIfAbsent(key,
            t -> new Retention(properties.getRetentionEvents()));
    }

    // ── value types ─────────────────────────────────────────────────────────────

    /**
     * A live SSE subscriber: caller id + audience membership + bounded write queue +
     * the metric dimensions ({@code channel} = bare topic, {@code tenant} = scope) +
     * a one-shot {@code disconnected} CAS flag that elects the single removal/disconnect
     * winner across the concurrent backpressure / drain-error / lifecycle-callback paths.
     */
    private record Subscriber(String callerId, Set<String> membership,
                              SseEmitter emitter, BlockingQueue<RetainedEvent> queue,
                              String channel, String tenant, AtomicBoolean disconnected) {
        Subscriber(String callerId, Set<String> membership, SseEmitter emitter,
                   BlockingQueue<RetainedEvent> queue, String channel, String tenant) {
            this(callerId, membership, emitter, queue, channel, tenant, new AtomicBoolean(false));
        }
    }

    /** A retained event for Last-Event-ID replay. */
    private record RetainedEvent(long id, Set<String> audience, Map<String, Object> payload) {}

    /**
     * Bounded per-topic ring buffer keyed by monotonic event id (RT-RECONNECT-001).
     * Older-than-window events are evicted; {@link #since} returns the gap-free tail
     * strictly newer than a cursor.
     */
    private static final class Retention {
        private final int capacity;
        private final Deque<RetainedEvent> ring = new ArrayDeque<>();

        Retention(int capacity) {
            this.capacity = capacity;
        }

        synchronized void add(RetainedEvent ev) {
            ring.addLast(ev);
            while (ring.size() > capacity) {
                ring.removeFirst();
            }
        }

        synchronized List<RetainedEvent> since(long cursor) {
            return ring.stream().filter(e -> e.id() > cursor).toList();
        }
    }
}
