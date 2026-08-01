package com.ax.template.authblueprint.scheduledtask.templateharness;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TEMPLATE VERIFICATION HARNESS for {@code templates/backend/scheduled-task/LockingPolicy.java}
 * (BACKLOG P2-62).
 *
 * <h2>The gap this closes</h2>
 * {@link com.ax.template.authblueprint.scheduledtask.DatabaseAdvisoryLockReleaseTest} verifies
 * the PRODUCTION lock ({@code DatabaseAdvisoryLock}, keyed by {@code task_name} in a separate
 * {@code task_locks} table). The holder-verification fix of BACKLOG P3-102 landed in the FORK
 * TEMPLATE twin instead — {@code templates/backend/scheduled-task/LockingPolicy.java}, keyed by
 * the {@code scheduled_tasks} row UUID — and templates are skeletons: they name
 * {@code com.example.app}, they are not on any source set, and nothing compiles or runs them.
 * The only evidence the template fix worked was a grep plus a structural assertion, i.e. prose.
 *
 * <h2>What this harness does instead</h2>
 * It carries an executable COPY of the three methods that make the template's lock correct
 * ({@code tryAcquire}, {@code release}, {@code isLockHeld}), running against hand-rolled
 * stand-ins for the template's collaborators — no Spring, no JPA, no new source set and no new
 * Gradle task. Two things then hold at once:
 * <ul>
 *   <li>{@link #copiedTryAcquire_skipsWhenLockIsHeldByAnotherNode()} and friends exercise the
 *       copied logic for real, so "the fix works" stops being a claim about text.</li>
 *   <li>{@link #harnessCopyIsCharacterIdenticalToTheTemplate()} re-reads BOTH files from disk,
 *       extracts the three method bodies from each, and asserts they are identical after
 *       whitespace normalisation. Comments are NOT stripped — they must match too, because a
 *       comment that survives the code it describes is the same rot in a slower form. Edit the
 *       template without editing this copy (or the reverse) and this test goes RED.</li>
 * </ul>
 * A copy without that second assertion would rot within one edit; that is why the drift test is
 * part of the harness rather than a follow-up.
 *
 * <h2>Boundary, stated rather than implied</h2>
 * This proves the template's lock LOGIC, not its Spring wiring: the copied methods run outside a
 * container, so the transaction propagation the template declares ({@code REQUIRES_NEW}, BACKLOG
 * P2-60) and the JPA {@code @Lock(PESSIMISTIC_WRITE)} on {@code findByIdForUpdate} are NOT
 * exercised here — an in-memory map has no row locks. What WOULD verify those is a fork-side
 * integration test against a real datasource; the catalog cannot run one for a skeleton that
 * names {@code com.example.app}. The extractor's one assumption is recorded where it is made:
 * see {@link #extractMethodBody}.
 */
@Tag("SCHEDULED_TASK")
@DisplayName("P2-62 — template LockingPolicy verification harness (executable copy + drift lock)")
class TemplateLockingPolicyHarnessTest {

    private static final Path TEMPLATE_RELATIVE =
            Paths.get("templates", "backend", "scheduled-task", "LockingPolicy.java");
    private static final Path HARNESS_RELATIVE = Paths.get(
            "backend", "src", "test", "java", "com", "ax", "template", "authblueprint",
            "scheduledtask", "templateharness", "TemplateLockingPolicyHarnessTest.java");

    private static final Duration TTL = Duration.ofSeconds(300);

    // ─────────────────────────────────────────────────────────────────────────
    // Stand-ins for the template's collaborators. Only the surface the copied
    // methods touch is modelled; the field and method NAMES must match the
    // template's, because the copy below is verbatim.
    // ─────────────────────────────────────────────────────────────────────────

    /** Mirrors the lock-state API of {@code templates/backend/scheduled-task/ScheduledTask}. */
    static final class ScheduledTask {
        private final UUID id;
        private String lockHolder;
        private Instant lockedAt;

        ScheduledTask(UUID id) {
            this.id = id;
        }

        UUID getId() {
            return id;
        }

        public String getLockHolder() {
            return lockHolder;
        }

        public Instant getLockedAt() {
            return lockedAt;
        }

        public void acquireLock(String holder) {
            this.lockHolder = holder;
            this.lockedAt = Instant.now();
        }

        public void releaseLock() {
            this.lockHolder = null;
            this.lockedAt = null;
        }

        /** Test-only: plant a lock acquired at an arbitrary instant (for staleness cases). */
        void plantLock(String holder, Instant at) {
            this.lockHolder = holder;
            this.lockedAt = at;
        }
    }

    /**
     * Mirrors {@code ScheduledTaskRepository}'s two lock-path members. Records which reads and
     * writes happened so the tests can assert the acquire/release paths went through
     * {@code findByIdForUpdate} — the pessimistic read — and never a plain lookup.
     */
    static final class ScheduledTaskRepository {
        private final Map<UUID, ScheduledTask> rows = new HashMap<>();
        private final List<UUID> forUpdateReads = new ArrayList<>();
        private final List<UUID> saves = new ArrayList<>();

        void seed(ScheduledTask task) {
            rows.put(task.getId(), task);
        }

        Optional<ScheduledTask> findByIdForUpdate(UUID taskId) {
            forUpdateReads.add(taskId);
            return Optional.ofNullable(rows.get(taskId));
        }

        ScheduledTask save(ScheduledTask task) {
            saves.add(task.getId());
            rows.put(task.getId(), task);
            return task;
        }

        List<UUID> forUpdateReads() {
            return forUpdateReads;
        }

        List<UUID> saves() {
            return saves;
        }
    }

    /** Stand-in for the SLF4J logger the copied bodies call. Output is irrelevant here. */
    static final class Log {
        void debug(String format, Object... args) {
            // no-op
        }

        void warn(String format, Object... args) {
            // no-op
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // The executable copy. Everything between BEGIN/END markers is verbatim from
    // templates/backend/scheduled-task/LockingPolicy.java#DbRowLockingPolicy and is
    // pinned character-for-character by harnessCopyIsCharacterIdenticalToTheTemplate().
    // ─────────────────────────────────────────────────────────────────────────
    static final class DbRowLockingPolicy {

        private final Log log = new Log();
        private final ScheduledTaskRepository taskRepository;
        private final Duration lockTtl;

        DbRowLockingPolicy(ScheduledTaskRepository taskRepository, Duration lockTtl) {
            this.taskRepository = taskRepository;
            this.lockTtl = lockTtl;
        }

        public boolean tryAcquire(UUID taskId, String lockHolder) {
            // FOR UPDATE, not findById: the row lock must span the isLockHeld() test and the
            // acquireLock() write below, or two nodes both pass the test and both "win".
            return taskRepository.findByIdForUpdate(taskId).map(task -> {
                // Check if lock is free or stale
                if (isLockHeld(task)) {
                    log.debug("Lock held by {} — skipping taskId={}", task.getLockHolder(), taskId);
                    return false;
                }
                if (task.getLockHolder() != null) {
                    log.warn("Reclaiming stale lock from {} for taskId={}", task.getLockHolder(), taskId);
                }
                task.acquireLock(lockHolder);
                taskRepository.save(task);
                return true;
            }).orElse(false);
        }

        public void release(UUID taskId, String lockHolder) {
            // FOR UPDATE, not findById: the holder-match test and the clearing write below
            // must be one indivisible step, or a concurrent stale takeover can commit between
            // the test and the write and have its brand-new lock deleted by this stale caller
            // (lock-theft replay — the same hazard tryAcquire's row lock prevents).
            taskRepository.findByIdForUpdate(taskId).ifPresent(task -> {
                if (!Objects.equals(lockHolder, task.getLockHolder())) {
                    log.warn("Release attempted by non-holder (expected={}, actual={}) — "
                            + "no-op for taskId={}", task.getLockHolder(), lockHolder, taskId);
                    return;
                }
                task.releaseLock();
                taskRepository.save(task);
            });
        }

        private boolean isLockHeld(ScheduledTask task) {
            if (task.getLockHolder() == null || task.getLockedAt() == null) {
                return false;
            }
            // Lock is stale if it was acquired longer ago than the TTL
            return task.getLockedAt().plus(lockTtl).isAfter(Instant.now());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Behaviour of the copied logic
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("tryAcquire on a free row claims it and records the holder")
    void copiedTryAcquire_claimsAFreeRow() {
        var repo = new ScheduledTaskRepository();
        var id = UUID.randomUUID();
        repo.seed(new ScheduledTask(id));
        var policy = new DbRowLockingPolicy(repo, TTL);

        assertThat(policy.tryAcquire(id, "node-a")).isTrue();
        assertThat(repo.rows.get(id).getLockHolder()).isEqualTo("node-a");
        assertThat(repo.forUpdateReads()).containsExactly(id);
    }

    @Test
    @DisplayName("tryAcquire returns false (never throws) when another node holds a live lock")
    void copiedTryAcquire_skipsWhenLockIsHeldByAnotherNode() {
        var repo = new ScheduledTaskRepository();
        var id = UUID.randomUUID();
        var task = new ScheduledTask(id);
        task.plantLock("node-a", Instant.now());
        repo.seed(task);
        var policy = new DbRowLockingPolicy(repo, TTL);

        assertThat(policy.tryAcquire(id, "node-b")).isFalse();
        assertThat(repo.rows.get(id).getLockHolder()).isEqualTo("node-a");
        assertThat(repo.saves()).isEmpty();
    }

    @Test
    @DisplayName("tryAcquire reclaims a lock older than the TTL (SCHED-LOCK-002)")
    void copiedTryAcquire_reclaimsAStaleLock() {
        var repo = new ScheduledTaskRepository();
        var id = UUID.randomUUID();
        var task = new ScheduledTask(id);
        task.plantLock("crashed-node", Instant.now().minus(TTL).minusSeconds(1));
        repo.seed(task);
        var policy = new DbRowLockingPolicy(repo, TTL);

        assertThat(policy.tryAcquire(id, "node-b")).isTrue();
        assertThat(repo.rows.get(id).getLockHolder()).isEqualTo("node-b");
    }

    @Test
    @DisplayName("tryAcquire on an absent row returns false rather than throwing")
    void copiedTryAcquire_absentRowIsALostRaceNotAnError() {
        var repo = new ScheduledTaskRepository();
        var policy = new DbRowLockingPolicy(repo, TTL);

        assertThat(policy.tryAcquire(UUID.randomUUID(), "node-a")).isFalse();
    }

    @Test
    @DisplayName("release by a NON-holder is a no-op — the P3-102 lock-theft replay")
    void copiedRelease_byNonHolderIsANoOp() {
        var repo = new ScheduledTaskRepository();
        var id = UUID.randomUUID();
        var task = new ScheduledTask(id);
        task.plantLock("node-c", Instant.now());   // C reclaimed the lock as stale
        repo.seed(task);
        var policy = new DbRowLockingPolicy(repo, TTL);

        policy.release(id, "node-b");              // B's late release must NOT clear C's lock

        assertThat(repo.rows.get(id).getLockHolder()).isEqualTo("node-c");
        assertThat(repo.saves()).isEmpty();
    }

    @Test
    @DisplayName("release by the current holder clears the lock, through the FOR UPDATE read")
    void copiedRelease_byHolderClearsTheLock() {
        var repo = new ScheduledTaskRepository();
        var id = UUID.randomUUID();
        var task = new ScheduledTask(id);
        task.plantLock("node-a", Instant.now());
        repo.seed(task);
        var policy = new DbRowLockingPolicy(repo, TTL);

        policy.release(id, "node-a");

        assertThat(repo.rows.get(id).getLockHolder()).isNull();
        assertThat(repo.rows.get(id).getLockedAt()).isNull();
        assertThat(repo.forUpdateReads()).containsExactly(id);
        assertThat(repo.saves()).containsExactly(id);
    }

    @Test
    @DisplayName("release of an absent row is a no-op, not an exception")
    void copiedRelease_absentRowIsANoOp() {
        var repo = new ScheduledTaskRepository();
        var policy = new DbRowLockingPolicy(repo, TTL);

        policy.release(UUID.randomUUID(), "node-a");

        assertThat(repo.saves()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Drift lock — what makes the copy above evidence rather than a fork
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("the copy is character-identical to the template (whitespace-normalised)")
    void harnessCopyIsCharacterIdenticalToTheTemplate() throws IOException {
        Path repoRoot = repoRoot();
        String template = Files.readString(repoRoot.resolve(TEMPLATE_RELATIVE), StandardCharsets.UTF_8);
        String harness = Files.readString(repoRoot.resolve(HARNESS_RELATIVE), StandardCharsets.UTF_8);

        // Both files declare the methods twice or more (the template also has
        // MockLockingPolicy; this file also quotes the names in prose), so each side is
        // scoped to its own DbRowLockingPolicy class body first.
        String templateScope = classBody(template, "class DbRowLockingPolicy implements LockingPolicy");
        String harnessScope = classBody(harness, "static final class DbRowLockingPolicy");

        for (String signature : List.of(
                "public boolean tryAcquire(UUID taskId, String lockHolder)",
                "public void release(UUID taskId, String lockHolder)",
                "private boolean isLockHeld(ScheduledTask task)")) {
            String fromTemplate = normalise(extractMethodBody(templateScope, signature));
            String fromHarness = normalise(extractMethodBody(harnessScope, signature));
            assertThat(fromHarness)
                    .as("harness copy of %s has drifted from templates/backend/scheduled-task/"
                            + "LockingPolicy.java — re-copy the template body verbatim (or, if the "
                            + "template intentionally changed, update this copy AND the tests above)",
                            signature)
                    .isEqualTo(fromTemplate);
            assertThat(fromTemplate).as("extractor returned an empty body for %s", signature).isNotBlank();
        }
    }

    /**
     * Walks up from the working directory until the template file is visible. Gradle runs tests
     * with {@code backend/} as the working directory; a run from the repo root also works.
     */
    private static Path repoRoot() {
        Path candidate = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (candidate != null) {
            if (Files.exists(candidate.resolve(TEMPLATE_RELATIVE))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException(
                "P2-62 harness: could not locate " + TEMPLATE_RELATIVE + " above "
                        + System.getProperty("user.dir")
                        + " — the harness cannot verify a template it cannot read");
    }

    /** Returns the brace-balanced body of the class whose declaration contains {@code marker}. */
    private static String classBody(String source, String marker) {
        int at = source.indexOf(marker);
        if (at < 0) {
            throw new IllegalStateException("P2-62 harness: class marker not found: " + marker);
        }
        return balancedBodyAfter(source, source.indexOf('{', at + marker.length()));
    }

    /**
     * Returns the brace-balanced body of the method whose signature line contains
     * {@code signature}.
     *
     * <p>ASSUMPTION, recorded where it is made: the scanner skips string and character literals
     * and both comment forms while counting braces, so SLF4J {@code {}} placeholders inside
     * message strings cannot unbalance it. It does NOT handle text blocks
     * ({@code """ ... """}); none of the three copied methods uses one, and a future body that
     * did would surface here as an extraction failure rather than as a silent mismatch.
     */
    private static String extractMethodBody(String scope, String signature) {
        int at = scope.indexOf(signature);
        if (at < 0) {
            throw new IllegalStateException("P2-62 harness: method signature not found: " + signature);
        }
        return balancedBodyAfter(scope, scope.indexOf('{', at + signature.length()));
    }

    private static String balancedBodyAfter(String source, int openBrace) {
        if (openBrace < 0) {
            throw new IllegalStateException("P2-62 harness: no opening brace found");
        }
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = openBrace; i < source.length(); i++) {
            char c = source.charAt(i);
            char next = (i + 1 < source.length()) ? source.charAt(i + 1) : '\0';
            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (inString || inChar) {
                if (c == '\\') {
                    i++;
                } else if (inString && c == '"') {
                    inString = false;
                } else if (inChar && c == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                inLineComment = true;
                i++;
            } else if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
            } else if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(openBrace + 1, i);
                }
            }
        }
        throw new IllegalStateException("P2-62 harness: unbalanced braces while extracting a body");
    }

    /** Collapses every whitespace run to a single space and trims. Comments are preserved. */
    private static String normalise(String body) {
        return body.replaceAll("\\s+", " ").trim();
    }
}
