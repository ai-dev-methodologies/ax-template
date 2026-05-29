package com.ax.template.authblueprint.common;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit coverage for the cross-cutting {@link IdempotencyKeyStore} after its
 * lift out of the payment package.
 *
 * <p>Anchors {@code specs/idempotency-l0.yaml}: IDEMPOTENCY-CACHE-001
 * (per-scope key isolation), IDEMPOTENCY-DEDUP-001 (replay returns the cached
 * id without re-running the supplier) and IDEMPOTENCY-CONCURRENT-001 (a
 * same-key race computes the side effect exactly once). Pure JVM test — no
 * Spring context, so it stays out of every {@code test{Domain}} tag set and
 * runs only under the aggregate {@code test} task.
 */
class IdempotencyKeyStoreTest {

    @Test
    void findOrCreate_firstSeenKey_invokesSupplierAndCaches() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();
        UUID created = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        UUID result = store.findOrCreate(scope, "key-1", () -> {
            calls.incrementAndGet();
            return created;
        });

        assertThat(result).isEqualTo(created);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(store.get(scope, "key-1")).isEqualTo(created);
    }

    @Test
    void findOrCreate_sameKeyReplay_returnsCachedIdWithoutReinvokingSupplier() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();

        UUID r1 = store.findOrCreate(scope, "key-replay", () -> {
            calls.incrementAndGet();
            return first;
        });
        UUID r2 = store.findOrCreate(scope, "key-replay", () -> {
            calls.incrementAndGet();
            return UUID.randomUUID(); // must NOT be invoked on replay
        });

        assertThat(r1).isEqualTo(first);
        assertThat(r2).isEqualTo(first);
        assertThat(calls.get()).as("supplier runs exactly once across replays").isEqualTo(1);
    }

    @Test
    void findOrCreate_differentKeys_produceDistinctResults() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();

        UUID a = store.findOrCreate(scope, "key-a", UUID::randomUUID);
        UUID b = store.findOrCreate(scope, "key-b", UUID::randomUUID);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void scopeIsolation_sameKeyDifferentScopes_doNotCollide() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scopeA = UUID.randomUUID();
        UUID scopeB = UUID.randomUUID();
        UUID idA = UUID.randomUUID();
        UUID idB = UUID.randomUUID();

        UUID rA = store.findOrCreate(scopeA, "shared-key", () -> idA);
        UUID rB = store.findOrCreate(scopeB, "shared-key", () -> idB);

        assertThat(rA).isEqualTo(idA);
        assertThat(rB).isEqualTo(idB);
        assertThat(store.get(scopeA, "shared-key")).isEqualTo(idA);
        assertThat(store.get(scopeB, "shared-key")).isEqualTo(idB);
    }

    @Test
    void get_unseenKey_returnsNull() {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        assertThat(store.get(UUID.randomUUID(), "never-seen")).isNull();
    }

    /**
     * IDEMPOTENCY-CONCURRENT-001: 10 threads race on the same key; the supplier
     * must run exactly once and every thread must observe the same id.
     */
    @RepeatedTest(20)
    void findOrCreate_concurrentSameKey_computesExactlyOnce() throws InterruptedException {
        IdempotencyKeyStore store = new IdempotencyKeyStore();
        UUID scope = UUID.randomUUID();
        String key = "race-" + UUID.randomUUID();
        UUID winner = UUID.randomUUID();

        int threads = 10;
        AtomicInteger supplierCalls = new AtomicInteger();
        UUID[] observed = new UUID[threads];
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                observed[idx] = store.findOrCreate(scope, key, () -> {
                    supplierCalls.incrementAndGet();
                    return winner;
                });
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(supplierCalls.get()).as("supplier runs exactly once under race").isEqualTo(1);
        for (UUID o : observed) {
            assertThat(o).as("all racers observe the single winner id").isEqualTo(winner);
        }
        assertThat(store.get(scope, key)).isEqualTo(winner);
    }
}
