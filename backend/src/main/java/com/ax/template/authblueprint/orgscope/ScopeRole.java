package com.ax.template.authblueprint.orgscope;

/**
 * containment-scope-authz domain role granted at an org-unit node (ORGSCOPE-GRANT-001). These are
 * NOT JWT security authorities (the auth system mints only ADMIN/MANAGER/MEMBER/AUDITOR) — a
 * ScopeRole is a RELATIONSHIP a principal holds at a tree node, evaluated in the service against
 * the action's required role (NIST SP 800-162 ABAC: authorization by attributes/relationship of
 * subject to object). VIEWER &lt; EDITOR &lt; MANAGER is an ordered ladder of authority AT a node:
 * a held grant satisfies a required role iff it is at least as strong. This intra-node ordering is
 * orthogonal to the TREE cascade (ORGSCOPE-CASCADE-001), which is purely structural.
 */
public enum ScopeRole {
    VIEWER,
    EDITOR,
    MANAGER;

    /** Does a held grant of THIS role satisfy a {@code required} role? (at-least-as-strong). */
    public boolean satisfies(ScopeRole required) {
        return this.ordinal() >= required.ordinal();
    }
}
