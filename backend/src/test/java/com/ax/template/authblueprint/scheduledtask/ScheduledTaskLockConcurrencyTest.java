package com.ax.template.authblueprint.scheduledtask;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SCHED-IDEMPOTENT-001 keystone — BACKLOG P2-48.
 * <p>
 * The claim under test is the one {@link LockingPolicy}'s contract makes and the one the
 * scheduled-task blueprint sells: an acquire is ATOMIC, so of N nodes racing for the same
 * task in the same TTL window exactly ONE runs the job. Before P2-48 the acquire was
 * {@code findByTaskName → test staleness → save}, a read-then-write whose two racers could
 * both pass the staleness test on the same row and both be told they held the lock.
 * <p>
 * Both branches of the acquire are raced, because they are made atomic by two DIFFERENT
 * primitives (see {@link DatabaseAdvisoryLock}) and a regression in either one is invisible
 * to the other's test:
 * <ul>
 *   <li>{@link #concurrentFirstAcquires_exactlyOneWins()} — row ABSENT: the arbiter is the
 *       {@code task_locks} PRIMARY KEY; losers' duplicate-key failures become {@code false}.</li>
 *   <li>{@link #concurrentStaleTakeovers_exactlyOneWins()} — row PRESENT and stale: the
 *       arbiter is the {@code SELECT ... FOR UPDATE} row lock held across the staleness test
 *       and the takeover UPDATE.</li>
 * </ul>
 * Each also asserts that NO racer threw. That is not decoration: it is half the contract.
 * {@code ScheduledTaskService#executeWithLock} calls {@code tryAcquire} OUTSIDE its try/catch,
 * so a loser that reports its loss by throwing (which is exactly what a read-then-write
 * acquire does once {@code @Version} rejects its stale UPDATE) escapes as a failed scheduler
 * tick or a 5xx on a manual trigger — an alarm on a task that was merely skipped.
 *
 * <h2>RED-on-revert (mutation proof)</h2>
 * Reverting {@link DatabaseAdvisoryLock#tryAcquire} to the non-locking derived finder turns
 * {@link #concurrentStaleTakeovers_exactlyOneWins()} RED — either two racers win the same
 * window, or the losers surface as throwables instead of {@code false}. (The first-acquire
 * test stays GREEN under that revert; the PK is doing that work, which is precisely why both
 * branches need their own race.)
 *
 * <p>Context choice: plain {@code RANDOM_PORT} with NO {@code @DirtiesContext} and NO
 * {@code @LocalServerPort} — this class calls the bean directly, so it shares the most common
 * context cache key without adding an entry, and the R22 eviction flake (a dirtied context
 * leaving {@code @LocalServerPort} on a dead Tomcat) cannot reach a test that never opens a
 * socket.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Tag("SCHEDULED_TASK")
@Tag("SCHED-IDEMPOTENT-001")
class ScheduledTaskLockConcurrencyTest {

    /** Same width as the DecisionGov DG-CONCURRENT-001 keystone. */
    private static final int RACERS = 8;

    @Autowired LockingPolicy lockingPolicy;
    @Autowired TaskLockRepository lockRepository;
    @Autowired Clock clock;
    @Autowired ScheduledTaskProperties properties;

    private String taskName;

    @BeforeEach
    void freshTaskName() {
        taskName = "p2-48-race-" + UUID.randomUUID();
    }

    @AfterEach
    void dropLockRow() {
        lockRepository.findById(taskName).ifPresent(lockRepository::delete);
    }

    /** Outcome of one race: who was told they hold the lock, and what escaped as an exception. */
    private record Outcome(List<String> winners, List<Throwable> thrown) { }

    private Outcome race() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(RACERS);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<String> winners = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Throwable> thrown = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < RACERS; i++) {
            String holder = "racer-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    if (lockingPolicy.tryAcquire(taskName, holder)) {
                        winners.add(holder);
                    }
                } catch (Throwable t) {
                    thrown.add(t);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS))
            .as("all racers finished — a hung acquire means the row lock was never released")
            .isTrue();

        return new Outcome(List.copyOf(winners), List.copyOf(thrown));
    }

    @Test
    @DisplayName("SCHED-IDEMPOTENT-001 — 8 concurrent FIRST acquires ⇒ exactly one holder, no exceptions")
    void concurrentFirstAcquires_exactlyOneWins() throws InterruptedException {
        Outcome outcome = race();

        assertThat(outcome.thrown())
            .as("a lost race is reported by returning false, never by throwing")
            .isEmpty();
        assertThat(outcome.winners())
            .as("the task_locks PRIMARY KEY admits exactly one first acquire")
            .hasSize(1);

        TaskLock persisted = lockRepository.findById(taskName).orElseThrow();
        assertThat(persisted.getLockHolder())
            .as("the row belongs to the racer that was told it won — not to a loser that "
                + "overwrote it")
            .isEqualTo(outcome.winners().get(0));
    }

    @Test
    @DisplayName("SCHED-IDEMPOTENT-001 — 8 concurrent STALE takeovers ⇒ exactly one holder, no exceptions")
    void concurrentStaleTakeovers_exactlyOneWins() throws InterruptedException {
        Instant crashedAt = Instant.now(clock)
            .minusSeconds(properties.getLockTtlSeconds() * 2L);
        lockRepository.saveAndFlush(new TaskLock(taskName, "crashed-holder", crashedAt));

        Outcome outcome = race();

        assertThat(outcome.thrown())
            .as("a lost takeover is reported by returning false, never by throwing")
            .isEmpty();
        assertThat(outcome.winners())
            .as("the FOR UPDATE row lock serializes the staleness test with the takeover, so "
                + "only the first racer sees a stale row — the rest re-read a fresh lockedAt")
            .hasSize(1);

        TaskLock persisted = lockRepository.findById(taskName).orElseThrow();
        assertThat(persisted.getLockHolder()).isEqualTo(outcome.winners().get(0));
        assertThat(persisted.getLockedAt())
            .as("the stale lockedAt was replaced, so the TTL window restarts from the takeover")
            .isAfter(crashedAt);
    }
}
