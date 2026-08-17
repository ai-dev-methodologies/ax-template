package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * BACKLOG P2-120 — the <b>single writer</b> of {@code io.restassured.RestAssured.port}.
 *
 * <p><b>The problem.</b> {@code RestAssured.port} is a process-global mutable static, and before
 * P2-120 <b>139 test files</b> assigned it by hand (86 files carried a {@code *TestSupport.java}
 * {@code useRandomPort} definition — each definition body itself contains one assignment — plus
 * 53 further files carried a direct {@code RestAssured.port = ...;} statement with no
 * definition; 141 assignment statements in total across those 139 files. Measured by diffing the
 * pre-P2-120 tree: {@code git diff <pre-P2-120-sha> -- backend/src/test | grep -c
 * '^-.*static void useRandomPort('} for the definition count, and the same diff filtered for
 * removed {@code RestAssured\s*\.\s*port\s*=} lines — excluding one false-positive match inside a
 * diagnostic string literal — for the assignment count.) A global with 139 writers cannot be
 * reasoned about: any file that fails to write it, writes it late, or writes someone else's value
 * silently redirects every subsequent request in the JVM. This is one of the two surviving
 * candidates for the P3-144 flake — "a mistargeted port" — and the machine this was measured on
 * makes it concrete: rest-assured's default port is <b>8080</b>, and another process on this
 * machine answers {@code *:8080} with a <b>Content-Type-less 404</b>, which is byte-for-byte the
 * failure shape P3-144 exhibited.
 *
 * <p><b>The fix.</b> This class is registered as a JUnit Jupiter extension and, immediately
 * before every test method, reads the test instance's {@code @LocalServerPort} field and
 * publishes it to {@code RestAssured.port} — <b>recording what it published</b>. The 141 manual
 * assignments become unnecessary, and the global acquires exactly one writer. See
 * {@code practices/evals/restassured_port_single_writer_guard.sh}, which enforces that no other
 * file under {@code backend/src/test/**} assigns it.
 *
 * <p><b>Subject, not exception.</b> This file assigns {@code RestAssured.port} and the guard does
 * not flag it. That is not an allowlist entry: the guard derives its excluded file from
 * {@code backend/src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension}
 * — the registration that makes this class the writer in the first place. Delete the
 * registration and this file is scanned like every other. The rule's SUBJECT is defined by the
 * mechanism, not by a name on a list.
 *
 * <p><b>Registration.</b> Global ServiceLoader auto-detection, enabled in
 * {@code backend/src/test/resources/junit-platform.properties}. Measured before choosing it: of
 * the 178 entries on {@code testRuntimeClasspath}, <b>zero</b> ship a
 * {@code META-INF/services/org.junit.jupiter.api.extension.Extension} file (the same scan does
 * find four other {@code META-INF/services/org.junit.*} files, so it is not a vacuous scan), so
 * auto-detection turns on nothing but this class today. It is additionally bounded for tomorrow
 * by {@code junit.jupiter.extensions.autodetection.include=com.ax.template.*}, so a future
 * dependency that ships such a service file still cannot be switched on silently. The
 * alternative — {@code @ExtendWith} on each class — would have edited 161 files to buy nothing
 * that the include-filter does not already buy.
 *
 * <p><b>An unbalanced stub override is caught where it happened.</b> {@link #overrideForStub}
 * without its {@link #restoreAfterStub} leaves the global aimed at a stub server that the next
 * test never asked for, and — worse than the raw mis-aim — leaves this class <i>describing</i>
 * that state as deliberate, so every later failure report says "STUB OVERRIDE ... not a defect".
 * The {@code AfterEachCallback} below compares the override depth against what it was when the
 * test started and fails <b>that</b> test if it grew, after unwinding the leak so it cannot reach
 * the next class. A class-scoped override taken in {@code @BeforeAll} and released in
 * {@code @AfterAll} is unchanged across every test in that class and so is not a leak; a test's
 * own {@code @AfterEach} runs before this callback, so releasing there is in time too.
 *
 * <p><b>Silence is deliberate.</b> A test class with no {@code @LocalServerPort} field (a
 * {@code WebEnvironment.MOCK} test, a plain unit test, a stub-server test) gets its global left
 * <b>untouched</b>. Publishing a guess there would be the very bug this class exists to remove.
 * What it does instead is <b>record the absence</b>, so {@link HttpExtract} can say "nothing was
 * published for this test, so the port you see is whatever the process last left there" rather
 * than implying the port was verified.
 *
 * <p><b>Scope of the record: static, not thread-local.</b> The thing being described
 * ({@code RestAssured.port}) is process-global, so the expectation is held with the same scope.
 * A {@code ThreadLocal} would report "nothing published" from any worker thread in the
 * concurrency tests, even though those threads share — and are steered by — the same global.
 *
 * @see HttpExtract#target() which reads {@link #diagnose()} into every failure message
 */
public final class AxPort implements BeforeEachCallback, AfterEachCallback {

    /** What the authority last published, and on whose behalf. Never {@code null} after a run. */
    private static volatile Publication current =
        Publication.none("<no test has run yet — if HTTP already happened, AxPort is not registered>");

    /** Saved globals for {@link #overrideForStub}, so {@link #restoreAfterStub} is exact. */
    private static final Deque<Saved> stubStack = new ArrayDeque<>();

    /**
     * The override depth when the running test began — the baseline {@link #afterEach} holds it
     * to. Static for the same reason {@link #current} is: what is being described is a
     * process-global, and a {@code ThreadLocal} would report a clean baseline from any worker
     * thread that shares it.
     */
    private static volatile int stubDepthAtTestStart = 0;

    /**
     * Required by the {@code ServiceLoader}: auto-detection instantiates the extension through a
     * no-argument constructor.
     */
    public AxPort() {}

    /** How {@code RestAssured.port} came to hold its current value. */
    private enum Kind {
        /** Published from a test instance's {@code @LocalServerPort} field. */
        LOCAL_SERVER_PORT,
        /** Published by {@link AxPort#overrideForStub} to aim at a non-application server. */
        STUB_OVERRIDE,
        /** Nothing was published — the global holds whatever the process last left there. */
        NONE
    }

    private record Publication(Kind kind, int port, String owner) {
        static Publication none(String owner) {
            return new Publication(Kind.NONE, 0, owner);
        }
    }

    /** Both globals a stub override displaces: the real one, and this class's record of it. */
    private record Saved(int restAssuredPort, Publication publication) {}

    // ─── the writer ──────────────────────────────────────────────────────────────────────────

    /**
     * Publishes the test instance's {@code @LocalServerPort} to {@code RestAssured.port}.
     *
     * <p>Runs before every test method. {@code @LocalServerPort} is populated by Spring during
     * {@code TestInstancePostProcessor.postProcessTestInstance}, which is strictly earlier than
     * any {@code BeforeEachCallback}, so the value is always already injected here. A test's own
     * {@code @BeforeEach} runs <i>after</i> this callback, so helper calls that perform HTTP
     * during setup are already aimed correctly.
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        String owner = context.getRequiredTestClass().getName()
            + "#" + context.getRequiredTestMethod().getName();

        // Recorded on every path, including the "no field, touch nothing" one below: the balance
        // check in afterEach is about what THIS test did, not about what it inherited.
        stubDepthAtTestStart = stubStack.size();

        Integer port = findLocalServerPort(context);

        if (port == null) {
            // No field to read. Do NOT touch the global — see "Silence is deliberate" above.
            // A live stub override is a deliberate publication and must survive; anything else
            // becomes an explicit NONE so a stale record cannot be mistaken for this test's.
            if (current.kind() != Kind.STUB_OVERRIDE) {
                current = Publication.none(owner);
            }
            return;
        }

        if (port <= 0) {
            throw new AssertionError(
                "@LocalServerPort injected a non-positive port (" + port + ") into " + owner
                    + ". RestAssured would be pointed at a server that is not this test's "
                    + "application, so every request in the class would fail uniformly. Failing "
                    + "here instead, where the cause is visible.");
        }

        RestAssured.port = port;
        current = new Publication(Kind.LOCAL_SERVER_PORT, port, owner);
    }

    /**
     * Fails the test that leaked a stub override, at the test that leaked it.
     *
     * <p>Runs after every test method (and after that test's own {@code @AfterEach} methods, so a
     * {@code finally}-style release there is in time). An override the test took and did not
     * release would otherwise survive into the next class with nothing failing: the global stays
     * aimed at a stub server that has usually already been stopped, and {@link #diagnose()} keeps
     * describing that aim as deliberate — the very "nothing fails at the point of the mistake"
     * shape this class was written to remove.
     *
     * <p>The leak is unwound before the error is thrown, so exactly one test fails rather than
     * every test that follows it.
     */
    @Override
    public void afterEach(ExtensionContext context) {
        int baseline = stubDepthAtTestStart;
        int leaked = stubStack.size() - baseline;
        if (leaked <= 0) {
            return;
        }

        String owner = context.getRequiredTestClass().getName()
            + "#" + context.getRequiredTestMethod().getName();
        int aimedAt = RestAssured.port;
        while (stubStack.size() > baseline) {
            restoreAfterStub();
        }

        throw new AssertionError(
            owner + " called AxPort.overrideForStub " + leaked + " more time(s) than "
                + "AxPort.restoreAfterStub. RestAssured.port was left aimed at " + aimedAt
                + " — a stub server this test started, not the application — and AxPort would "
                + "have gone on reporting that aim as deliberate for every later test in this "
                + "JVM. Pair every overrideForStub with restoreAfterStub in a finally block (or "
                + "in @AfterAll, for a class-scoped override). The override has been unwound so "
                + "only this test fails.");
    }

    /**
     * Walks the test instances outermost-first (so a {@code @Nested} class inherits its
     * enclosing class's port) and, within each, walks the class hierarchy, returning the first
     * {@code @LocalServerPort}-annotated field's value.
     *
     * @return the injected port, or {@code null} when no such field exists anywhere
     */
    private static Integer findLocalServerPort(ExtensionContext context) {
        List<Object> instances = context.getRequiredTestInstances().getAllInstances();
        for (Object instance : instances) {
            for (Class<?> type = instance.getClass(); type != null; type = type.getSuperclass()) {
                for (Field field : type.getDeclaredFields()) {
                    if (!field.isAnnotationPresent(LocalServerPort.class)) {
                        continue;
                    }
                    try {
                        field.setAccessible(true);
                        Object value = field.get(instance);
                        if (value instanceof Number number) {
                            return number.intValue();
                        }
                        throw new AssertionError(
                            "@LocalServerPort on " + type.getName() + "." + field.getName()
                                + " holds " + (value == null ? "null" : value.getClass().getName())
                                + ", which is not a port. RestAssured cannot be aimed from it.");
                    } catch (IllegalAccessException | RuntimeException e) {
                        throw new AssertionError(
                            "cannot read @LocalServerPort field " + type.getName() + "."
                                + field.getName() + " — RestAssured cannot be aimed at this "
                                + "test's application.", e);
                    }
                }
            }
        }
        return null;
    }

    // ─── the one sanctioned manual aim: a server that is NOT this application ────────────────

    /**
     * Aims {@code RestAssured.port} at a server that is deliberately <b>not</b> this test's
     * application — a stub {@code HttpServer} that reproduces a pathological response shape.
     *
     * <p>This exists because such tests are real (the diagnosability proofs drive a stub that
     * answers without a {@code Content-Type} header) and because the honest way to express
     * "I mean a different port" is an API that says so, not an exemption from the rule. The
     * publication is recorded as {@link Kind#STUB_OVERRIDE}, so a failure message says the port
     * is aimed off-application on purpose rather than leaving a reader to wonder.
     *
     * <p>Pair every call with {@link #restoreAfterStub()} in a {@code finally} block.
     *
     * @throws AssertionError if {@code port} is not a usable port; the global is left untouched
     */
    public static void overrideForStub(int port) {
        if (port <= 0) {
            throw new AssertionError(
                "overrideForStub was given a non-positive port (" + port + "). RestAssured would "
                    + "be pointed at a server that is not this test's application, so every "
                    + "request in the class would fail uniformly. Failing here instead, where "
                    + "the cause is visible.");
        }
        stubStack.push(new Saved(RestAssured.port, current));
        RestAssured.port = port;
        current = new Publication(Kind.STUB_OVERRIDE, port,
            "a stub server started by this test (deliberately NOT this application)");
    }

    /** Undoes the most recent {@link #overrideForStub}, restoring both the global and the record. */
    public static void restoreAfterStub() {
        Saved saved = stubStack.poll();
        if (saved == null) {
            throw new AssertionError(
                "restoreAfterStub() without a matching overrideForStub() — RestAssured.port would "
                    + "be left aimed at a stub server for every later test in this JVM.");
        }
        RestAssured.port = saved.restAssuredPort();
        current = saved.publication();
    }

    // ─── the reader: drift / clobbering diagnosis ────────────────────────────────────────────

    /**
     * One line naming whether {@code RestAssured.port} still holds what this authority published.
     *
     * <p>This is the point of the whole exercise. P3-144 has two surviving candidates — "a
     * mistargeted port" and everything else — and until now a failure report could not tell them
     * apart, because the observed port carried no claim about what it <i>should</i> have been.
     * With a recorded expectation the next occurrence is decided on sight:
     * <b>CLOBBERED</b> convicts the port; <b>MATCHES</b> acquits it and moves the search to the
     * server that answered; <b>NOTHING PUBLISHED</b> says the port was never established at all,
     * which for a test that just performed HTTP is itself the finding.
     */
    public static String diagnose() {
        return diagnoseAgainst(RestAssured.port);
    }

    /**
     * {@link #diagnose()} against a supplied observation instead of the live global.
     *
     * <p>This exists so the CLOBBERED branch can be proven. Reaching it through the live global
     * would require the proof test to <b>be</b> the rogue writer this whole mechanism (and
     * {@code restassured_port_single_writer_guard.sh}) exists to forbid — a test that has to
     * break the rule to demonstrate the rule is not evidence, it is a second writer. Passing the
     * observation in exercises the same expression on the same recorded publication with nothing
     * mutated. Package-private: the seam is for {@code common}'s own proofs, not an API.
     */
    static String diagnoseAgainst(int actual) {
        Publication p = current;
        return switch (p.kind()) {
            case LOCAL_SERVER_PORT -> actual == p.port()
                ? "  port authority: MATCHES — AxPort published " + p.port() + " from "
                    + "@LocalServerPort for " + p.owner() + " and RestAssured.port still holds it. "
                    + "A mistargeted port is RULED OUT; whatever answered is the server on "
                    + p.port() + "."
                : "  port authority: CLOBBERED — AxPort published " + p.port() + " from "
                    + "@LocalServerPort for " + p.owner() + ", but RestAssured.port now reads "
                    + actual + ". Something assigned the process-global between the extension and "
                    + "this request. This IS the mistargeted-port failure (P3-144), confirmed — "
                    + "find the writer (practices/evals/restassured_port_single_writer_guard.sh "
                    + "forbids all of them but this class).";
            case STUB_OVERRIDE -> "  port authority: STUB OVERRIDE — AxPort.overrideForStub("
                + p.port() + ") aimed RestAssured at " + p.owner() + "; RestAssured.port reads "
                + actual + (actual == p.port() ? " (as published)" : " (CLOBBERED since)") + ". "
                + "A non-application response here is the point of the test, not a defect.";
            case NONE -> "  port authority: NOTHING PUBLISHED — no @LocalServerPort was published "
                + "for " + p.owner() + " (either it declares no such field, or AxPort never ran), "
                + "so RestAssured.port (" + actual + ") is whatever the process last left there. A "
                + "mistargeted port is NOT ruled out: rest-assured's own default is 8080, which on "
                + "a developer machine is routinely some other process.";
        };
    }
}
