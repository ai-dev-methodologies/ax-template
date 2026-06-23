package com.ax.template.authblueprint.orgscope;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.Check;

import java.time.Instant;
import java.util.UUID;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * containment-scope-authz root: one node in an org-unit TREE. Each node has a {@code parentId}
 * (the root has none) and a MATERIALIZED PATH — the {@code /}-delimited chain of ancestor ids
 * ending in this node's own id, e.g. {@code /root/.../self/} — that makes subtree containment a
 * deterministic prefix test at ARBITRARY depth (ORGSCOPE-TREE-001). A grant at this node cascades
 * DOWNWARD to its whole subtree (a node is contained by an ancestor when its path is prefixed by
 * the ancestor's path); the cascade is DERIVED from this path, never a denormalized per-node ACL
 * (ORGSCOPE-CASCADE-001). The tree is append-structured here — no reparent/move — so the path is
 * fixed at creation. The {@code @Check} forbids a self-parent. The node is its own sole mutator:
 * the only mutable column is {@link #version} (JPA-managed); identity + structure are immutable.
 */
@AggregateRoot
@Entity
@Table(name = "org_units")
@Check(constraints = "parent_id IS NULL OR parent_id <> id")
public class OrgUnit {

    /** Path segment delimiter — also the leading/trailing sentinel so a prefix test is unambiguous. */
    static final String SEP = "/";

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The parent node's id; NULL only for a tree root (the @Check forbids self-parenting). */
    @Column(name = "parent_id", updatable = false)
    private UUID parentId;

    @Column(name = "name", nullable = false, updatable = false, length = 200)
    private String name;

    /**
     * Materialized ancestor path: {@code /} + the {@code /}-joined chain of ancestor ids + this
     * node's own id + {@code /}. A node N is in the subtree of node A iff {@code N.path} starts
     * with {@code A.path} (A.path is itself a {@code /}…/A/ prefix). Immutable — set at creation.
     */
    @Column(name = "path", nullable = false, updatable = false, length = 2000)
    private String path;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OrgUnit() {}

    public OrgUnit(UUID id, UUID parentId, String name, String path, Instant createdAt) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.createdAt = createdAt;
    }

    /** The materialized path of a child of {@code parentPath}, ending in {@code childId}. */
    static String childPath(String parentPath, UUID childId) {
        return parentPath + childId + SEP;
    }

    /** The materialized path of a root node (no parent): {@code /selfId/}. */
    static String rootPath(UUID selfId) {
        return SEP + selfId + SEP;
    }

    /**
     * Containment test, derived purely from the tree (ORGSCOPE-CASCADE-001): is THIS node in the
     * subtree of (i.e. contained by) {@code ancestor}? True iff this node's path is prefixed by
     * the ancestor's path — which holds for the ancestor itself (self-containment) and for every
     * descendant at arbitrary depth, and is FALSE for siblings and strict ancestors of {@code
     * ancestor} (a grant at a leaf does NOT cascade upward).
     */
    public boolean isContainedBy(OrgUnit ancestor) {
        return this.path.startsWith(ancestor.path);
    }

    public UUID getId() { return id; }
    public UUID getParentId() { return parentId; }
    public String getName() { return name; }
    public String getPath() { return path; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
}
