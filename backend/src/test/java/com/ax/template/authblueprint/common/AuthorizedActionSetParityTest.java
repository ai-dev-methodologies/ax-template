package com.ax.template.authblueprint.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.ax.template.authblueprint.approvalworkflow.ApprovalActionEvaluator;
import com.ax.template.authblueprint.approvalworkflow.ApprovalActionGuards;
import com.ax.template.authblueprint.approvalworkflow.ApprovalAggregateFixtures;
import com.ax.template.authblueprint.approvalworkflow.ApprovalRequest;
import com.ax.template.authblueprint.approvalworkflow.ApprovalRequestStateMachine;
import com.ax.template.authblueprint.approvalworkflow.ApprovalRequestStatus;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStep;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStepResponse;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStepStateMachine;
import com.ax.template.authblueprint.approvalworkflow.ApprovalStepStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S2.AUTHZ.XB — cross-boundary parity: the FE must not offer an approval-workflow
 * (전자결재) action the BE would reject, and must not hide one the BE allows.
 *
 * <p>Sibling of {@code common.PageEnvelopeContractParityTest} / {@code payment.MoneyContractParityTest}
 * (same pattern: one committed golden, two independent consumers — this Jackson test +
 * {@code frontend/tests/authz-action-parity.vitest.ts} both read
 * {@code frontend/tests/_fixtures/authorized-actions.golden.json}). Plain Jackson unit test — NO
 * {@code @SpringBootTest}, zero ContextCache pressure.
 *
 * <h2>Scope — this now CALLS the production decision function</h2>
 * When this test was written the BE emitted no action-set at all, so
 * {@code computeBeAuthorizedActions} was an honest-but-fragile REIMPLEMENTATION of the
 * enforced guards, anchored only by file:line comments. P2-38a/b closed that gap:
 * {@link ApprovalActionEvaluator} computes the set server-side, delegating every branch to
 * {@link ApprovalActionGuards} — the SAME predicates {@code ApprovalService} enforces on
 * the action path — plus the two real state machines' transition tables.
 *
 * <p>This test therefore no longer re-derives anything. It builds a probe aggregate from
 * each golden row and asks the REAL evaluator. Consequences, and the reason the residual
 * that kept the coverage cell {@code partial} is gone:
 * <ul>
 *   <li>neutering {@code isNextActionableStep} flips this test AND the action-path
 *       STEP_OUT_OF_ORDER test — one edit, two independent surfaces;</li>
 *   <li>same for {@code isRequester} (here + the ownership 404 + the requester-list
 *       contract test) and {@code canView} (here + single-GET visibility);</li>
 *   <li>a change to either state machine's ALLOWED map still flips this test, as before.</li>
 * </ul>
 *
 * <p>Mutation lock: hand-flip any one row's {@code expectedActions} in the golden and this
 * test goes RED, because the evaluator derives the set from production code and does not
 * echo the golden back at itself.
 *
 * <h2>P3-76 — the step-scoped leg</h2>
 * Every golden row now also carries {@code expectedStepActions} (step id → the actions the
 * caller may invoke on THAT step), asserted against
 * {@link ApprovalActionEvaluator#allowedStepActions}. The request-scoped array says whether
 * the caller may approve SOMETHING; it cannot say WHICH step is theirs, so a client
 * rendering an action panel had to re-derive the step — the client-side authorization guess
 * P2-38b removed, one level down. The request-scoped set is computed BY CALLING the
 * step-scoped method, and {@link #requestScopedApproveReject_isExactlyTheUnionOfTheStepScopedSets}
 * pins that relation, so the two legs cannot drift apart.
 */
// WORKFLOW is the approval-workflow domain's per-domain-task tag (matches every other
// class in backend/src/test/java/.../approvalworkflow/*, e.g. ApprovalComplianceTest) —
// binding this cross-boundary test to `./gradlew testApprovalWorkflow` even though the
// file itself lives in `common` alongside its PageEnvelope/Money contract-parity siblings.
@Tag("WORKFLOW")
@Tag("AUTHZ-XB")
@Tag("WF-AUTHZ-001")
@Tag("WF-AUTHZ-002")
@Tag("WF-AUTHZ-003")
@Tag("WF-STEP-001")
class AuthorizedActionSetParityTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private record StepFixture(int orderIndex, String approverUserId, ApprovalStepStatus status) {}

    private static JsonNode goldenRows() throws IOException {
        Path golden = Path.of(System.getProperty("user.dir"), "..", "frontend", "tests",
                "_fixtures", "authorized-actions.golden.json");
        return MAPPER.readTree(Files.readString(golden)).get("rows");
    }

    private static final ApprovalActionGuards GUARDS = new ApprovalActionGuards();
    private static final ApprovalActionEvaluator EVALUATOR = new ApprovalActionEvaluator(
            GUARDS,
            new ApprovalRequestStateMachine(FIXED_CLOCK),
            new ApprovalStepStateMachine(FIXED_CLOCK));

    /**
     * Builds the golden row's aggregate and asks the PRODUCTION
     * {@link ApprovalActionEvaluator}. No rule is expressed in this file.
     */
    private static List<String> beAuthorizedActions(JsonNode requestNode, String callerId) {
        return EVALUATOR.allowedActions(buildAggregate(requestNode), callerId);
    }

    private static ApprovalRequest buildAggregate(JsonNode requestNode) {
        List<ApprovalAggregateFixtures.StepSpec> steps = new ArrayList<>();
        for (JsonNode s : requestNode.get("steps")) {
            JsonNode acted = s.get("actedByUserId");
            steps.add(new ApprovalAggregateFixtures.StepSpec(
                    UUID.fromString(s.get("id").asText()),
                    s.get("orderIndex").asInt(),
                    s.get("approverUserId").asText(),
                    ApprovalStepStatus.valueOf(s.get("status").asText()),
                    acted == null || acted.isNull() ? null : acted.asText()));
        }
        return ApprovalAggregateFixtures.build(
                requestNode.get("requesterUserId").asText(),
                ApprovalRequestStatus.valueOf(requestNode.get("status").asText()),
                steps);
    }

    private static List<String> expectedActionsOf(JsonNode row) {
        List<String> expected = new ArrayList<>();
        row.get("expectedActions").forEach(n -> expected.add(n.asText()));
        return expected;
    }

    /**
     * P3-76 — the golden row's step-scoped expectations, asked of the PRODUCTION
     * {@link ApprovalActionEvaluator#allowedStepActions}. Keyed by step id, so a row that
     * reorders its steps cannot quietly re-point an expectation at a different step.
     */
    private static Map<String, List<String>> beStepActions(JsonNode requestNode, String callerId) {
        ApprovalRequest request = buildAggregate(requestNode);
        Map<String, List<String>> actual = new LinkedHashMap<>();
        for (ApprovalStep step : request.getSteps()) {
            actual.put(step.getId().toString(), EVALUATOR.allowedStepActions(request, step, callerId));
        }
        return actual;
    }

    private static Map<String, List<String>> expectedStepActionsOf(JsonNode row) {
        JsonNode node = row.get("expectedStepActions");
        assertThat(node)
                .as("every golden row must carry expectedStepActions (P3-76)")
                .isNotNull();
        Map<String, List<String>> expected = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : node.properties()) {
            List<String> actions = new ArrayList<>();
            entry.getValue().forEach(n -> actions.add(n.asText()));
            expected.put(entry.getKey(), actions);
        }
        return expected;
    }

    @Test
    void authorizedActionSet_matchesGoldenFixtureForEveryRow() throws IOException {
        JsonNode rows = goldenRows();
        assertThat(rows.isArray()).as("golden fixture must have a rows array").isTrue();
        assertThat(rows.size()).as("golden fixture must not be empty").isPositive();

        for (JsonNode row : rows) {
            String label = row.get("label").asText();
            List<String> expected = expectedActionsOf(row);
            List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
            assertThat(actual)
                    .as("row '%s' — BE-authorized action set must match the golden's expectedActions", label)
                    .isEqualTo(expected);
        }
    }

    @Test
    void requesterOnDraftRequest_mayViewSubmitAndCancel_butNotApproveOrReject() throws IOException {
        JsonNode row = findRow(goldenRows(), "requester_draft_own_request");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("cancel", "submit", "view");
    }

    @Test
    void assignedFirstStepApprover_onSubmittedRequest_mayApproveOrReject_butNotSubmitOrCancel() throws IOException {
        JsonNode row = findRow(goldenRows(), "approver_first_step_pending_submitted");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("approve", "reject", "view");
    }

    @Test
    void nonParticipant_getsNoActionsAtAll_notEvenView() throws IOException {
        JsonNode row = findRow(goldenRows(), "non_participant_submitted");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    /**
     * The key parity trap: an approver assigned to a LATER step is visible on a SUBMITTED request
     * (findVisibleTo does not check ordering) but is NOT actionable until every earlier step is
     * APPROVED (ApprovalService's strict-ordering guard). A FE that renders "approve" merely because
     * the caller's id appears in {@code steps[].approverUserId} — without also checking ordering —
     * would offer an action the BE rejects with 409 STEP_OUT_OF_ORDER.
     */
    @Test
    void secondStepApprover_isVisibleButNotActionable_whileEarlierStepIsStillPending() throws IOException {
        JsonNode row = findRow(goldenRows(), "second_step_approver_visible_but_out_of_order");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    /**
     * P3-63 positive — an approver who ACTED keeps view once the request goes terminal, so they
     * can see the outcome of their own decision. Still view-only (terminal ⇒ no transitions, and
     * they are not the requester).
     */
    @Test
    void actedApprover_keepsViewOnly_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "acted_approver_keeps_view_after_terminal");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    /**
     * P3-63 negative #1 — the qualifier that keeps the widening narrow. An assigned approver who
     * was never reached (an upstream rejection ended the chain first) gets NOTHING: a rejection
     * must not retroactively disclose the request to approvers who never decided.
     */
    @Test
    void unactedApprover_getsNothing_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "unacted_approver_no_view_after_terminal");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    /** P3-63 negative #2 — a DRAFT is not visible to its named approvers; it has not been sent. */
    @Test
    void assignedApprover_getsNothing_whileRequestIsDraft() throws IOException {
        JsonNode row = findRow(goldenRows(), "assigned_approver_no_view_on_draft");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).isEmpty();
    }

    @Test
    void requester_keepsViewOnly_afterRequestGoesTerminal() throws IOException {
        JsonNode row = findRow(goldenRows(), "requester_own_request_after_terminal_approval");
        List<String> actual = beAuthorizedActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual).containsExactly("view");
    }

    // ── P3-76: the step-scoped leg ──────────────────────────────────────────

    /**
     * P3-76 — the request-scoped array says whether the caller may approve SOMETHING; it
     * cannot say WHICH step is theirs, so a client rendering an action panel still had to
     * re-derive the step. {@code ApprovalStepResponse.allowedActions} answers that, and
     * this is its parity assertion: the golden's per-step expectations must equal what the
     * production evaluator computes, step for step.
     */
    @Test
    void stepScopedActionSet_matchesGoldenFixtureForEveryRow() throws IOException {
        for (JsonNode row : goldenRows()) {
            String label = row.get("label").asText();
            assertThat(beStepActions(row.get("request"), row.get("callerId").asText()))
                    .as("row '%s' — BE step-scoped action sets must match expectedStepActions", label)
                    .isEqualTo(expectedStepActionsOf(row));
        }
    }

    /**
     * The request-scoped set is the UNION of the per-step sets plus the request-scoped
     * tokens — {@code allowedActions} calls {@code allowedStepActions} rather than
     * repeating its branches, so the two CANNOT disagree. This asserts the consequence
     * that matters to a client: approve/reject appears on the request iff some step offers
     * it. A future refactor that re-inlines the step branches breaks this.
     */
    @Test
    void requestScopedApproveReject_isExactlyTheUnionOfTheStepScopedSets() throws IOException {
        for (JsonNode row : goldenRows()) {
            String label = row.get("label").asText();
            String caller = row.get("callerId").asText();
            List<String> request = beAuthorizedActions(row.get("request"), caller);
            List<String> unionOfSteps = beStepActions(row.get("request"), caller).values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .sorted()
                    .toList();
            List<String> requestStepScoped = request.stream()
                    .filter(a -> ApprovalActionEvaluator.APPROVE.equals(a)
                            || ApprovalActionEvaluator.REJECT.equals(a))
                    .sorted()
                    .toList();
            assertThat(requestStepScoped)
                    .as("row '%s' — request-scoped approve/reject must equal the union of the step sets", label)
                    .isEqualTo(unionOfSteps);
        }
    }

    /**
     * The discriminating row: once step 0 is APPROVED the 결재선 has advanced, so the
     * SECOND step — not the first — is the one its approver may act on. Paired with
     * {@link #secondStepApprover_isVisibleButNotActionable_whileEarlierStepIsStillPending}
     * (identical shape, step 0 still PENDING, every set empty), this is what makes the
     * step-scoped field falsifiable rather than a restatement of "steps[0]".
     */
    @Test
    void secondStep_becomesTheActionableOne_onceTheFirstIsApproved() throws IOException {
        JsonNode row = findRow(goldenRows(), "second_step_approver_actionable_after_first_approved");
        Map<String, List<String>> actual = beStepActions(row.get("request"), row.get("callerId").asText());
        assertThat(actual.get("1a2b3c4d-0000-4000-8000-000000000001"))
                .as("step 0 is carol's, not the caller's")
                .isEmpty();
        assertThat(actual.get("2b3c4d5e-0000-4000-8000-000000000002"))
                .as("step 1 is the caller's and every earlier step is APPROVED")
                .containsExactly("approve", "reject");
    }

    /**
     * The step-scoped vocabulary is NARROWER than the request-scoped one, and the contract
     * declares it so ({@code ApprovalStepResponse.allowedActions.items.enum: [approve,
     * reject]}, with {@code wire_missing: [cancel, submit, view]} in contract-enum-map).
     * `view` is request-scoped — visibility is a property of the request, not a step — and
     * submit/cancel act on the request as a whole. Emitting any of the three on a step
     * would advertise an action no step endpoint implements.
     */
    @Test
    void stepScopedSet_neverCarriesARequestScopedToken() throws IOException {
        for (JsonNode row : goldenRows()) {
            String label = row.get("label").asText();
            beStepActions(row.get("request"), row.get("callerId").asText())
                    .forEach((id, actions) -> assertThat(actions)
                            .as("row '%s' step %s", label, id)
                            .isSubsetOf(ApprovalActionEvaluator.APPROVE, ApprovalActionEvaluator.REJECT));
        }
    }

    /**
     * The DTO actually carries what the evaluator computes. The assertions above exercise
     * the evaluator directly; this one pins the wire projection, so a
     * {@code ApprovalStepResponse.from} that dropped the field (or passed the wrong step)
     * cannot pass on the strength of a correct evaluator.
     */
    @Test
    void stepResponse_carriesTheEvaluatorsAnswer() throws IOException {
        JsonNode row = findRow(goldenRows(), "second_step_approver_actionable_after_first_approved");
        ApprovalRequest request = buildAggregate(row.get("request"));
        String caller = row.get("callerId").asText();
        Map<String, List<String>> onTheWire = new LinkedHashMap<>();
        for (ApprovalStep step : request.getSteps()) {
            ApprovalStepResponse dto = ApprovalStepResponse.from(step, request, EVALUATOR, caller);
            onTheWire.put(dto.id().toString(), dto.allowedActions());
        }
        assertThat(onTheWire).isEqualTo(expectedStepActionsOf(row));
    }

    /**
     * A caller-independent projection (an internal fan-out with no caller) gets an EMPTY
     * set, not a populated one — absence of a claim, not a claim of absence. Mirrors the
     * request-scoped {@code evaluator == null} posture P2-38b established.
     */
    @Test
    void stepResponse_withoutACaller_claimsNothing() throws IOException {
        JsonNode row = findRow(goldenRows(), "second_step_approver_actionable_after_first_approved");
        ApprovalRequest request = buildAggregate(row.get("request"));
        for (ApprovalStep step : request.getSteps()) {
            assertThat(ApprovalStepResponse.from(step, request, EVALUATOR, null).allowedActions())
                    .as("no caller ⇒ no claim")
                    .isEmpty();
            assertThat(ApprovalStepResponse.from(step, request, null, "erin").allowedActions())
                    .as("no evaluator ⇒ no claim")
                    .isEmpty();
        }
    }

    /**
     * P3-76 follow-up — ID-COMPARISON PARITY. Every id comparison this domain enforces is a
     * bare {@link String#equals}: {@code ApprovalActionGuards.isAssignedApprover} (:42),
     * {@code isRequester} (:63), {@code hasActed} (:109). The FE fallback's
     * {@code authorized-actions.sameId} used to trim and case-fold, so a caller differing
     * from the stored id only in case or padding was AUTHORIZED client-side and REJECTED by
     * the server — the fallback offering an action the BE answers with 403/404.
     *
     * <p>The two sweeps above already assert these rows (they iterate the whole golden), so
     * BE and FE are pinned to the SAME denial. This states the semantics by name, and
     * proves the denial is about the ID rather than about an unactionable aggregate: the
     * exact-cased caller on the very same aggregate DOES get the actions.
     */
    @Test
    void callerIdComparison_isExact_notFoldedOrTrimmed() throws IOException {
        JsonNode rows = goldenRows();
        for (String label : List.of(
                "mixed_case_requester_id_denied", "whitespace_padded_requester_id_denied",
                "mixed_case_approver_id_denied", "whitespace_padded_approver_id_denied")) {
            JsonNode row = findRow(rows, label);
            JsonNode request = row.get("request");
            String distorted = row.get("callerId").asText();
            assertThat(beAuthorizedActions(request, distorted))
                    .as("row '%s' — a case/padding-distorted id must authorize NOTHING", label)
                    .isEmpty();
            assertThat(beStepActions(request, distorted).values())
                    .as("row '%s' — nor any step-scoped action", label)
                    .allSatisfy(actions -> assertThat(actions).isEmpty());
            // … and the EXACT id on the same aggregate is not empty, so the denial above is
            // the id comparison's doing, not an unactionable request.
            assertThat(beAuthorizedActions(request, distorted.trim().toLowerCase(Locale.ROOT)))
                    .as("row '%s' — the exact-cased caller IS authorized on this aggregate", label)
                    .isNotEmpty();
        }
    }

    private static JsonNode findRow(JsonNode rows, String label) {
        for (JsonNode row : rows) {
            if (label.equals(row.get("label").asText())) {
                return row;
            }
        }
        throw new AssertionError("golden fixture row not found: " + label);
    }
}
