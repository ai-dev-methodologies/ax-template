package com.ax.template.authblueprint.orgscope;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * containment-scope-authz thin controller. The acting principal is ALWAYS the authenticated caller
 * (caller-authentication-only-no-userid-param); a containment check resolves the caller's grants —
 * the {@code principal} of a check is never a body/path param. Delegates to {@link OrgScopeService}.
 */
@RestController
public class OrgScopeController {

    public record CreateNodeReq(UUID parentId, @NotBlank @Size(max = 200) String name) {}
    public record GrantReq(@NotNull UUID orgUnitId, @NotBlank @Size(max = 320) String principal,
                           @NotNull ScopeRole role) {}
    public record CheckReq(@NotNull UUID targetNodeId, @NotNull ScopeRole requiredRole) {}

    public record NodeDto(UUID id, UUID parentId, String name, String path, Instant createdAt) {
        static NodeDto of(OrgUnit u) {
            return new NodeDto(u.getId(), u.getParentId(), u.getName(), u.getPath(), u.getCreatedAt());
        }
    }
    public record GrantDto(UUID id, UUID orgUnitId, String principal, ScopeRole role,
                           String grantedBy, Instant grantedAt) {
        static GrantDto of(ScopeGrant g) {
            return new GrantDto(g.getId(), g.getOrgUnitId(), g.getPrincipal(), g.getRole(),
                g.getGrantedBy(), g.getGrantedAt());
        }
    }
    public record DecisionDto(boolean allowed, UUID viaNodeId, ScopeRole viaRole) {
        static DecisionDto of(OrgScopeService.ScopeDecision d) {
            return new DecisionDto(d.allowed(), d.viaNodeId(), d.viaRole());
        }
    }

    private final OrgScopeService service;

    public OrgScopeController(OrgScopeService service) {
        this.service = service;
    }

    /** ORGSCOPE-TREE-001 — create a tree node (parentId null ⇒ root). */
    @PostMapping("/api/org-scope/nodes")
    public ResponseEntity<NodeDto> createNode(@Valid @RequestBody CreateNodeReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(NodeDto.of(service.createNode(req.parentId(), req.name())));
    }

    @GetMapping("/api/org-scope/nodes/{id}")
    public NodeDto getNode(@PathVariable UUID id) {
        return NodeDto.of(service.getNode(id));
    }

    /** ORGSCOPE-GRANT-001 — record a grant at a node (idempotent). */
    @PostMapping("/api/org-scope/grants")
    public ResponseEntity<GrantDto> grant(@Valid @RequestBody GrantReq req, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(GrantDto.of(service.grant(req.orgUnitId(), req.principal(), req.role(), auth.getName())));
    }

    @GetMapping("/api/org-scope/nodes/{id}/grants")
    public List<GrantDto> grantsAtNode(@PathVariable UUID id) {
        return service.grantsAtNode(id).stream().map(GrantDto::of).toList();
    }

    /** ORGSCOPE-CONTAINMENT/CASCADE-001 (keystone) — may the CALLER act on a node with a role?
     *  Allowed (200) iff the caller holds a satisfying grant at the node or any ancestor of it;
     *  otherwise 403 OUT_OF_SCOPE. The principal is the authenticated caller — never a param. */
    @PostMapping("/api/org-scope/check")
    public DecisionDto check(@Valid @RequestBody CheckReq req, Authentication auth) {
        return DecisionDto.of(service.check(auth.getName(), req.targetNodeId(), req.requiredRole()));
    }

    @ExceptionHandler(OrgScopeException.class)
    public ResponseEntity<ProblemDetail> handle(OrgScopeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
        pd.setType(URI.create(ex.type()));
        pd.setProperty("code", ex.code());
        return ResponseEntity.status(ex.status()).body(pd);
    }
}
