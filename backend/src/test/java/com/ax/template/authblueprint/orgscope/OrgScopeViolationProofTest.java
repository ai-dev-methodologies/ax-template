package com.ax.template.authblueprint.orgscope;

import jakarta.persistence.Column;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VIOLATION proof for containment-scope-authz-l0. Structural assertions a deliberate break cannot
 * pass silently: the OrgUnit carries a materialized path + @Check no-self-parent + @Version; the
 * ScopeGrant is append-only one-per-(node,principal,role) and fully immutable; containment is a
 * downward-only path-prefix test (a leaf grant never reaches upward); NO delete path exists; the
 * grant write path uses the PESSIMISTIC_WRITE finder; and the migration carries the same backstops.
 */
@Tag("ORGSCOPE")
class OrgScopeViolationProofTest {

    // ── ORGSCOPE-TREE-001 — OrgUnit is an immutable tree node with @Check no-self-parent + @Version ──
    @Test @Tag("ORGSCOPE-TREE-001")
    void violation_orgUnitIsImmutableTreeNode_noSelfParent_versioned() throws Exception {
        for (Method m : OrgUnit.class.getMethods()) {
            assertThat(m.getName()).as("OrgUnit must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "parentId", "name", "path", "createdAt"}) {
            Column col = OrgUnit.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("OrgUnit." + f + " must be immutable").isFalse();
        }
        Check check = OrgUnit.class.getAnnotation(Check.class);
        assertThat(check.constraints().replaceAll("\\s+", " "))
            .as("OrgUnit must forbid a self-parent").contains("parent_id IS NULL OR parent_id <> id");
        assertThat(OrgUnit.class.getDeclaredField("version").isAnnotationPresent(Version.class)).isTrue();
    }

    // ── ORGSCOPE-CASCADE-001 — containment is a DOWNWARD-ONLY path-prefix test (no upward leak) ──
    @Test @Tag("ORGSCOPE-CASCADE-001")
    void violation_containmentIsDownwardOnlyPrefixTest() {
        UUID rootId = UUID.randomUUID();
        UUID divId = UUID.randomUUID();
        UUID leafId = UUID.randomUUID();
        UUID sibId = UUID.randomUUID();
        OrgUnit root = new OrgUnit(rootId, null, "Root", OrgUnit.rootPath(rootId), null);
        OrgUnit div = new OrgUnit(divId, rootId, "Div", OrgUnit.childPath(root.getPath(), divId), null);
        OrgUnit leaf = new OrgUnit(leafId, divId, "Leaf", OrgUnit.childPath(div.getPath(), leafId), null);
        OrgUnit sib = new OrgUnit(sibId, rootId, "Sib", OrgUnit.childPath(root.getPath(), sibId), null);

        // self + descendants are contained (downward, arbitrary depth)
        assertThat(div.isContainedBy(div)).as("a node contains itself").isTrue();
        assertThat(leaf.isContainedBy(div)).as("a descendant is contained").isTrue();
        assertThat(leaf.isContainedBy(root)).as("a deep descendant is contained").isTrue();
        // ancestors and siblings are NOT contained (no upward / sideways leak)
        assertThat(div.isContainedBy(leaf)).as("a leaf grant must NOT cascade upward to the parent").isFalse();
        assertThat(root.isContainedBy(div)).as("a grant must NOT cascade upward to an ancestor").isFalse();
        assertThat(sib.isContainedBy(div)).as("a grant must NOT cascade sideways to a sibling").isFalse();
    }

    // ── ORGSCOPE-GRANT-001 — the grant is append-only one-per-(node,principal,role), fully immutable ──
    @Test @Tag("ORGSCOPE-GRANT-001")
    void violation_grantAppendOnly_uniquePerNodePrincipalRole() throws Exception {
        for (Method m : ScopeGrant.class.getMethods()) {
            assertThat(m.getName()).as("ScopeGrant must have no public setter").doesNotStartWith("set");
        }
        for (String f : new String[]{"id", "orgUnitId", "principal", "role", "grantedBy", "grantedAt"}) {
            Column col = ScopeGrant.class.getDeclaredField(f).getAnnotation(Column.class);
            assertThat(col).as(f + " must carry @Column").isNotNull();
            assertThat(col.updatable()).as("ScopeGrant." + f + " must be immutable").isFalse();
        }
        jakarta.persistence.Table table = ScopeGrant.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(table.uniqueConstraints()[0].columnNames())
            .containsExactly("org_unit_id", "principal", "role");
    }

    // ── ORGSCOPE-CONTAINMENT-001 — role ordering: stronger satisfies weaker, never the reverse ──
    @Test @Tag("ORGSCOPE-CONTAINMENT-001")
    void violation_roleLadderStrongerSatisfiesWeaker() {
        assertThat(ScopeRole.MANAGER.satisfies(ScopeRole.VIEWER)).isTrue();
        assertThat(ScopeRole.MANAGER.satisfies(ScopeRole.MANAGER)).isTrue();
        assertThat(ScopeRole.EDITOR.satisfies(ScopeRole.VIEWER)).isTrue();
        assertThat(ScopeRole.VIEWER.satisfies(ScopeRole.MANAGER)).as("a weaker role must NOT satisfy a stronger one").isFalse();
        assertThat(ScopeRole.EDITOR.satisfies(ScopeRole.MANAGER)).isFalse();
    }

    // ── no delete path; grant write path takes the node row lock; check is fail-closed ──
    @Test @Tag("ORGSCOPE-CONCURRENT-001") @Tag("ORGSCOPE-CONTAINMENT-001")
    void violation_noDeletePath_lockedGrant_failClosedCheck() throws Exception {
        for (Method m : OrgUnitRepository.class.getDeclaredMethods()) {
            assertThat(m.getName()).as("no delete method on the repository").doesNotContain("delete");
        }
        for (String src : new String[]{"OrgScopeService", "OrgScopeController"}) {
            String text = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
                "com", "ax", "template", "authblueprint", "orgscope", src + ".java"));
            assertThat(text).as(src + " must contain no delete call — the tree is append-structured")
                .doesNotContain(".delete(").doesNotContain("deleteBy");
        }

        Method locked = OrgUnitRepository.class.getMethod("findByIdForUpdate", UUID.class);
        org.springframework.data.jpa.repository.Lock lock =
            locked.getAnnotation(org.springframework.data.jpa.repository.Lock.class);
        assertThat(lock).isNotNull();
        assertThat(lock.value()).isEqualTo(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "orgscope", "OrgScopeService.java"));
        int gs = svc.indexOf("public ScopeGrant grant(");
        assertThat(gs).as("grant() must exist").isPositive();
        String grantBody = svc.substring(gs, svc.indexOf("\n    }", gs));
        assertThat(grantBody).as("grant() must take the node row lock").contains("findByIdForUpdate");
        // the check derives the decision from containment and throws OUT_OF_SCOPE when no grant matches
        assertThat(svc).as("the check is fail-closed at the containment boundary")
            .contains("isContainedBy").contains("OrgScopeException.outOfScope()");
        assertThat(svc).as("no denormalized per-node ACL table is consulted")
            .doesNotContain("AclRow").doesNotContain("aclRepo");
    }

    // ── the migration carries the same backstops ──
    @Test @Tag("ORGSCOPE-TREE-001") @Tag("ORGSCOPE-GRANT-001")
    void violation_migrationCarriesTheSameBackstops() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/db/migration/V060__create_orgscope.sql")) {
            assertThat(in).as("V060__create_orgscope.sql must exist").isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("\\s+", " ");
            assertThat(sql).contains("parent_id IS NULL OR parent_id <> id");
            assertThat(sql).contains("UNIQUE INDEX uq_scope_grant");
            assertThat(sql).contains("(org_unit_id, principal, role)");
        }
    }

    // ── P1-64 REVERT (audit-seal-11) — grant()'s idempotency is the node-lock + recheck, NOT a
    //    REQUIRES_NEW isolated insert. A ScopeGrant is an FK child of the FOR-UPDATE-locked org_units
    //    row, so an inner-tx insert on a separate connection would self-hang on PostgreSQL waiting for
    //    a FOR KEY SHARE lock the suspended outer tx already holds. The insert MUST stay in the outer
    //    tx; the node lock + recheck serialize concurrent same-key grants to exactly one row. ──
    @Test @Tag("ORGSCOPE-CONCURRENT-001")
    void violation_grantIdempotencyIsLockPlusRecheck_notRequiresNewIsolation() throws Exception {
        String svc = Files.readString(Path.of(System.getProperty("user.dir"), "src", "main", "java",
            "com", "ax", "template", "authblueprint", "orgscope", "OrgScopeService.java"));
        int gs = svc.indexOf("public ScopeGrant grant(");
        assertThat(gs).as("grant() must exist").isPositive();
        String grantBody = svc.substring(gs, svc.indexOf("\n    }", gs));

        // order: take the node row lock → recheck for an existing grant → insert only if absent
        int lockAt = grantBody.indexOf("findByIdForUpdate");
        int recheckAt = grantBody.indexOf("findGrant(");
        int insertAt = grantBody.indexOf("members.persist");
        assertThat(lockAt).as("grant() must take the node row lock").isNotNegative();
        assertThat(recheckAt).as("grant() must recheck for an existing grant AFTER taking the lock")
            .isGreaterThan(lockAt);
        assertThat(insertAt).as("grant() must insert AFTER the recheck (idempotent under the lock)")
            .isGreaterThan(recheckAt);

        // the FK-child insert must NOT be isolated in a REQUIRES_NEW inner tx while the parent
        // org_units row is held FOR UPDATE — that self-hangs on PostgreSQL. No IdempotentInsert.
        assertThat(grantBody).as("the grant insert must stay in the outer tx (no REQUIRES_NEW isolation)")
            .doesNotContain("idempotentInsert");
        assertThat(java.util.Arrays.stream(OrgScopeService.class.getDeclaredFields())
                .noneMatch(f -> f.getType() == com.ax.template.authblueprint.common.IdempotentInsert.class))
            .as("OrgScopeService must NOT hold an IdempotentInsert — lock+recheck is the idempotency mechanism")
            .isTrue();
    }
}
