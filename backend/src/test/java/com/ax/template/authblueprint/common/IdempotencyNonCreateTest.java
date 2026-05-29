package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for the NON-CREATE idempotency guards added to
 * {@link IdempotencyKeyStore} — {@link IdempotencyKeyStore#firstSeen(UUID, String)}
 * and {@link IdempotencyKeyStore#idempotent(UUID, String, Runnable)}.
 *
 * <p>Anchors {@code specs/idempotency-l0.yaml} IDEMPOTENCY-SCOPE-001 (the method
 * scope covers POST/PUT/PATCH/DELETE). These overloads exist for PUT/PATCH/DELETE
 * mutations that have no new resource id to return, for which IDW2 found the
 * existing {@code findOrCreate}'s {@code Supplier<UUID>} awkward.
 *
 * <p>Pure JVM test — no Spring context. Tagged {@code COMMON_IDEMPOTENCY} so it is
 * addressable as a focused selection while still running under the aggregate
 * {@code test} task (no per-domain task is required for a cross-cutting primitive).
 */
@Tag("COMMON_IDEMPOTENCY")
class IdempotencyNonCreateTest {

    @Test
    void firstSeen_firstCall_returnsTrue() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();

        assertThat(store.firstSeen(scope, "key-1")).isTrue();
    }

    @Test
    void firstSeen_replaySameScopeAndKey_returnsFalse() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();

        assertThat(store.firstSeen(scope, "key-replay")).as("first call").isTrue();
        assertThat(store.firstSeen(scope, "key-replay")).as("replay").isFalse();
        assertThat(store.firstSeen(scope, "key-replay")).as("third call still replay").isFalse();
    }

    @Test
    void firstSeen_differentKeysSameScope_areIndependent() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();

        assertThat(store.firstSeen(scope, "key-a")).isTrue();
        assertThat(store.firstSeen(scope, "key-b")).as("distinct key is first-seen too").isTrue();
        assertThat(store.firstSeen(scope, "key-a")).as("key-a now a replay").isFalse();
    }

    @Test
    void firstSeen_sameKeyDifferentScopes_areIndependent() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();

        assertThat(store.firstSeen(scopeA, "shared-key")).as("scope A first").isTrue();
        assertThat(store.firstSeen(scopeB, "shared-key")).as("scope B independent first").isTrue();
        assertThat(store.firstSeen(scopeA, "shared-key")).as("scope A replay").isFalse();
        assertThat(store.firstSeen(scopeB, "shared-key")).as("scope B replay").isFalse();
    }

    @Test
    void firstSeen_doesNotPollute_findOrCreateCache() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();

        // Recording a non-create key must NOT leak into the create-side cache.
        assertThat(store.firstSeen(scope, "k")).isTrue();
        assertThat(store.get(scope, "k")).as("non-create guard leaves create cache empty").isNull();
    }

    @Test
    void idempotent_firstCall_runsActionAndReturnsTrue() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();
        AtomicInteger runs = new AtomicInteger();

        boolean ran = store.idempotent(scope, "mutate-1", runs::incrementAndGet);

        assertThat(ran).isTrue();
        assertThat(runs.get()).isEqualTo(1);
    }

    @Test
    void idempotent_replay_isNoOpAndReturnsFalse() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();
        AtomicInteger runs = new AtomicInteger();

        boolean first = store.idempotent(scope, "mutate-replay", runs::incrementAndGet);
        boolean second = store.idempotent(scope, "mutate-replay", runs::incrementAndGet);

        assertThat(first).isTrue();
        assertThat(second).as("replay returns false").isFalse();
        assertThat(runs.get()).as("action runs exactly once across replays").isEqualTo(1);
    }

    @Test
    void idempotent_differentScope_runsActionAgain() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();
        AtomicInteger runs = new AtomicInteger();

        store.idempotent(scopeA, "k", runs::incrementAndGet);
        store.idempotent(scopeB, "k", runs::incrementAndGet);

        assertThat(runs.get()).as("each scope independently runs once").isEqualTo(2);
    }

    /**
     * IDEMPOTENCY-CONCURRENT-001 carried over to the non-create guard: when 10
     * threads race on the same (scope, key), exactly one observes {@code true}
     * and exactly one runs the action.
     */
    @RepeatedTest(20)
    void firstSeen_concurrentSameKey_exactlyOneTrue() throws InterruptedException {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();
        String key = "race-" + UUID.randomUUID();

        int threads = 10;
        AtomicInteger trueCount = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (store.firstSeen(scope, key)) {
                    trueCount.incrementAndGet();
                }
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(trueCount.get()).as("exactly one racer is first-seen").isEqualTo(1);
    }
}
