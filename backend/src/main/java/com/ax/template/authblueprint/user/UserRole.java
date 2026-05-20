package com.ax.template.authblueprint.user;

public enum UserRole {
    ADMIN,
    MANAGER,
    MEMBER,
    // R14 audit-log domain: AUDITOR role permits export access without
    // granting full ADMIN privileges (blueprints/audit-log-manifest.yaml#rbac.EXPORT,
    // AUDIT-EXPORT-002). Independent of ADMIN/MANAGER/MEMBER.
    AUDITOR
}
