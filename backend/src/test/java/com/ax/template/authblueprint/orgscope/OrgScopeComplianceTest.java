package com.ax.template.authblueprint.orgscope;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * containment-scope-authz compliance — verified against the live orgscope reference workload.
 * The invariant: OrgUnits form a TREE with a materialized ancestor path; a ScopeGrant gives a
 * principal a role AT a node; a containment check allows the caller IFF a satisfying grant is held
 * at the target node OR an ancestor of it (downward-only cascade — never a sibling or ancestor of
 * the granted node), else 403 OUT_OF_SCOPE; the cascade is derived from the tree; concurrent
 * same-key grants serialize to exactly one row.
 * Spec: specs/containment-scope-authz-l0.yaml (NIST RBAC role hierarchy + NIST SP 800-162 ABAC + CWE-362).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("ORGSCOPE")
class OrgScopeComplianceTest {

    @LocalServerPort int port;
    @Autowired OrgScopeService service;
    String member;
    String memberPrincipal;     // == auth.getName() == JWT subject == userId

    @BeforeEach
    void setup() {
        member = OrgScopeTestSupport.obtainToken(OrgScopeTestSupport.freshEmail("orgscope-member"), "MEMBER");
        memberPrincipal = given().header("Authorization", "Bearer " + member)
            .when().get("/api/auth/me").then().statusCode(200).extract().path("userId");
    }

    // ── helpers ──────────────────────────────────────────────────────────────────────
    private String createNode(String parentId, String name) {
        String body = parentId == null
            ? "{\"name\":\"" + name + "\"}"
            : "{\"parentId\":\"" + parentId + "\",\"name\":\"" + name + "\"}";
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body(body)
        .when().post("/api/org-scope/nodes").then().statusCode(201).extract().path("id");
    }

    private ExtractableResponse<Response> grant(String nodeId, String principal, String role) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"orgUnitId\":\"" + nodeId + "\",\"principal\":\"" + principal + "\",\"role\":\"" + role + "\"}")
        .when().post("/api/org-scope/grants").thenReturn().then().extract();
    }

    /** A containment check issued AS the member (principal = the caller). */
    private ExtractableResponse<Response> check(String targetNodeId, String requiredRole) {
        return given().header("Authorization", "Bearer " + member).header("Content-Type", "application/json")
            .body("{\"targetNodeId\":\"" + targetNodeId + "\",\"requiredRole\":\"" + requiredRole + "\"}")
        .when().post("/api/org-scope/check").thenReturn().then().extract();
    }

    // ── ORGSCOPE-TREE-001 — tree nodes carry a materialized ancestor path (prefix containment) ──
    @Test @Tag("ORGSCOPE-TREE-001")
    void tree_nodesCarryMaterializedAncestorPath() {
        String root = createNode(null, "Root");
        String divA = createNode(root, "Division A");
        String teamA1 = createNode(divA, "Team A1");

        String rootPath = given().header("Authorization", "Bearer " + member)
            .when().get("/api/org-scope/nodes/" + root).then().statusCode(200).extract().path("path");
        String divPath = given().header("Authorization", "Bearer " + member)
            .when().get("/api/org-scope/nodes/" + divA).then().statusCode(200).extract().path("path");
        String teamPath = given().header("Authorization", "Bearer " + member)
            .when().get("/api/org-scope/nodes/" + teamA1).then().statusCode(200).extract().path("path");

        assertThat(rootPath).isEqualTo("/" + root + "/");
        assertThat(divPath).isEqualTo("/" + root + "/" + divA + "/");
        assertThat(teamPath).isEqualTo("/" + root + "/" + divA + "/" + teamA1 + "/");
        // arbitrary-depth containment is a prefix relation
        assertThat(teamPath).startsWith(divPath);
        assertThat(divPath).startsWith(rootPath);
    }

    // ── ORGSCOPE-GRANT-001 — a grant is immutable + idempotent (one per node,principal,role) ──
    @Test @Tag("ORGSCOPE-GRANT-001")
    void grant_isIdempotent_onePerNodePrincipalRole() {
        String root = createNode(null, "Root");
        ExtractableResponse<Response> first = grant(root, "alice", "EDITOR");
        assertThat(first.statusCode()).isEqualTo(201);
        String firstId = first.jsonPath().getString("id");

        // re-grant the same (node, principal, role) → idempotent: same id, no duplicate row
        ExtractableResponse<Response> again = grant(root, "alice", "EDITOR");
        assertThat(again.statusCode()).isEqualTo(201);
        assertThat(again.jsonPath().getString("id")).isEqualTo(firstId);

        var grants = given().header("Authorization", "Bearer " + member)
            .when().get("/api/org-scope/nodes/" + root + "/grants")
            .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(grants).as("exactly one grant row for (node, alice, EDITOR)").hasSize(1);
    }

    // ── ORGSCOPE-CONTAINMENT-001 (KEYSTONE) — mid-tree grant → allowed on descendants, 403 on siblings/ancestors ──
    @Test @Tag("ORGSCOPE-CONTAINMENT-001")
    void containment_midTreeGrant_allowsDescendants_deniesSiblingsAndAncestors() {
        String root = createNode(null, "Root");
        String divA = createNode(root, "Division A");
        String teamA1 = createNode(divA, "Team A1");
        String divB = createNode(root, "Division B");        // sibling subtree of divA

        // grant the CALLER MANAGER at the mid-tree node divA
        assertThat(grant(divA, memberPrincipal, "MANAGER").statusCode()).isEqualTo(201);

        // allowed on the granted node and its descendants (downward containment, arbitrary depth)
        ExtractableResponse<Response> onDiv = check(divA, "MANAGER");
        assertThat(onDiv.statusCode()).isEqualTo(200);
        assertThat(onDiv.jsonPath().getBoolean("allowed")).isTrue();
        assertThat(onDiv.jsonPath().getString("viaNodeId")).isEqualTo(divA);
        assertThat(check(teamA1, "MANAGER").statusCode()).as("descendant allowed").isEqualTo(200);

        // 403 OUT_OF_SCOPE on a sibling subtree
        ExtractableResponse<Response> onSibling = check(divB, "MANAGER");
        assertThat(onSibling.statusCode()).isEqualTo(403);
        assertThat(onSibling.jsonPath().getString("code")).isEqualTo("OUT_OF_SCOPE");

        // 403 OUT_OF_SCOPE on an ancestor (the cascade is downward only)
        ExtractableResponse<Response> onAncestor = check(root, "MANAGER");
        assertThat(onAncestor.statusCode()).isEqualTo(403);
        assertThat(onAncestor.jsonPath().getString("code")).isEqualTo("OUT_OF_SCOPE");
    }

    // ── ORGSCOPE-CONTAINMENT-001 — a weaker grant role does not satisfy a stronger required role ──
    @Test @Tag("ORGSCOPE-CONTAINMENT-001")
    void containment_weakerRole_is403_strongerRoleSatisfiesWeaker() {
        String root = createNode(null, "Root");
        String div = createNode(root, "Division");
        grant(div, memberPrincipal, "VIEWER");

        // VIEWER grant cannot satisfy a MANAGER requirement → 403
        ExtractableResponse<Response> needManager = check(div, "MANAGER");
        assertThat(needManager.statusCode()).isEqualTo(403);
        assertThat(needManager.jsonPath().getString("code")).isEqualTo("OUT_OF_SCOPE");

        // but a VIEWER grant DOES satisfy a VIEWER requirement (at-least-as-strong)
        assertThat(check(div, "VIEWER").statusCode()).isEqualTo(200);
    }

    // ── ORGSCOPE-CASCADE-001 — a LEAF grant does not cascade upward; a ROOT grant covers the whole tree ──
    @Test @Tag("ORGSCOPE-CASCADE-001")
    void cascade_leafGrantNeverCascadesUpward_rootGrantCoversWholeTree() {
        String root = createNode(null, "Root");
        String div = createNode(root, "Division");
        String leaf = createNode(div, "Leaf");

        // a grant at the LEAF authorizes only the leaf — NOT its parent or any ancestor
        grant(leaf, memberPrincipal, "EDITOR");
        assertThat(check(leaf, "EDITOR").statusCode()).as("leaf itself allowed").isEqualTo(200);
        assertThat(check(div, "EDITOR").statusCode()).as("parent denied — no upward cascade").isEqualTo(403);
        assertThat(check(root, "EDITOR").statusCode()).as("ancestor denied — no upward cascade").isEqualTo(403);

        // a fresh principal granted at the ROOT covers the entire tree (downward, all depths)
        String adminEmail = OrgScopeTestSupport.freshEmail("orgscope-rootadmin");
        String adminTok = OrgScopeTestSupport.obtainToken(adminEmail, "MEMBER");
        String adminPrincipal = given().header("Authorization", "Bearer " + adminTok)
            .when().get("/api/auth/me").then().statusCode(200).extract().path("userId");
        grant(root, adminPrincipal, "MANAGER");
        for (String node : new String[]{root, div, leaf}) {
            ExtractableResponse<Response> dec = given().header("Authorization", "Bearer " + adminTok)
                .header("Content-Type", "application/json")
                .body("{\"targetNodeId\":\"" + node + "\",\"requiredRole\":\"MANAGER\"}")
                .when().post("/api/org-scope/check").thenReturn().then().extract();
            assertThat(dec.statusCode()).as("root grant covers " + node).isEqualTo(200);
            assertThat(dec.jsonPath().getString("viaNodeId")).isEqualTo(root);
        }
    }

    // ── a check against a non-existent node is 404, not a 403 leak ──
    @Test @Tag("ORGSCOPE-CONTAINMENT-001")
    void check_unknownNode_is404() {
        ExtractableResponse<Response> r = check(UUID.randomUUID().toString(), "VIEWER");
        assertThat(r.statusCode()).isEqualTo(404);
        assertThat(r.jsonPath().getString("code")).isEqualTo("RESOURCE_NOT_FOUND");
    }

    // ── ORGSCOPE-CONCURRENT-001 — keystone: N concurrent same-key grants → exactly one row ──
    @Test @Tag("ORGSCOPE-CONCURRENT-001")
    void concurrentSameKeyGrants_exactlyOneRow() throws Exception {
        String root = createNode(null, "Root");
        UUID nodeId = UUID.fromString(root);

        int n = 8;
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CountDownLatch start = new CountDownLatch(1);
        ConcurrentLinkedQueue<UUID> grantIds = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                start.await();
                try {
                    grantIds.add(service.grant(nodeId, "racer", ScopeRole.EDITOR, "tester").getId());
                } catch (RuntimeException ex) {
                    // a residual race must still converge — record nothing, the assert on the row count holds
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        // every successful caller saw the SAME grant id; exactly one row exists at the node
        assertThat(grantIds.stream().distinct().count())
            .as("ORGSCOPE-CONCURRENT-001 — all concurrent same-key grants converge to one id").isEqualTo(1L);
        var grants = given().header("Authorization", "Bearer " + member)
            .when().get("/api/org-scope/nodes/" + root + "/grants")
            .then().statusCode(200).extract().jsonPath().getList("$");
        assertThat(grants).as("exactly one grant row for (node, racer, EDITOR)").hasSize(1);
    }
}
