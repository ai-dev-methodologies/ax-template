package com.ax.template.authblueprint.approvalworkflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CRUD + resolution orchestrator for {@link RoutingRule} (WF-ROUTE-001/002). The rule set
 * itself is plain admin-managed config (create/list/delete — no in-place edit); the
 * immutability guarantee that matters (WF-ROUTE-001's "later rule changes do NOT rewrite
 * in-flight requests") lives in {@link ApprovalService#submit}, which resolves a rule's
 * output ONCE and never re-joins against the live rule set afterward.
 */
@Service
public class RoutingRuleService {

    private final RoutingRuleRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RoutingRuleService(RoutingRuleRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public RoutingRuleResponse create(CreateRoutingRuleRequest body) {
        RoutingRule rule = new RoutingRule(
            UUID.randomUUID(),
            body.categoryOrDept().strip(),
            body.minAmount(),
            body.maxAmount(),
            serializeChain(body.approverRoleChain()),
            Instant.now(clock));
        RoutingRule saved = repository.save(rule);
        return RoutingRuleResponse.from(saved, objectMapper);
    }

    @Transactional(readOnly = true)
    public List<RoutingRuleResponse> list() {
        return repository.findAllByOrderByCategoryOrDeptAscMinAmountAsc(
                org.springframework.data.domain.PageRequest.of(0, 500)).stream()
            .map(r -> RoutingRuleResponse.from(r, objectMapper))
            .toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RoutingRuleNotFoundException(id);
        }
        repository.deleteById(id);
    }

    private String serializeChain(List<String> chain) {
        try {
            return objectMapper.writeValueAsString(chain);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("approverRoleChain is not JSON-serializable", ex);
        }
    }
}
