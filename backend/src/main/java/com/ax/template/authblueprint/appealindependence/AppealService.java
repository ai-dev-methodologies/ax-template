package com.ax.template.authblueprint.appealindependence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * appeal-decider-independence-l0 sole orchestrator. Every write is an INSERT (append-only —
 * APPEAL-OUTCOME-001); {@link #fileAppeal} enforces independence against EVERY decider already
 * in the chain (APPEAL-CHAIN-001), not only the immediate parent — the DB @Check on
 * {@link AppealDecision} backstops only the pairwise (immediate-parent) case, which is the one
 * a single-row CHECK constraint can express (APPEAL-DISTINCT-001).
 */
@Service
public class AppealService {

    private final AppealDecisionRepository repository;
    private final Clock clock;

    public AppealService(AppealDecisionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Level 0 — the decision that may later be appealed. */
    @Transactional
    public AppealDecision fileOriginal(String decidedBy, String outcome) {
        UUID id = UUID.randomUUID();
        AppealDecision original = new AppealDecision(id, null, id, 0, AppealDecisionKind.ORIGINAL,
            decidedBy, null, outcome, Instant.now(clock));
        return repository.save(original);
    }

    /**
     * APPEAL-DISTINCT-001 / APPEAL-CHAIN-001 — {@code decidedBy} MUST differ from every decider
     * already in the chain (walked via {@code chainRootId}), and the parent MUST NOT already
     * have an appeal (one appeal per level).
     */
    @Transactional
    public AppealDecision fileAppeal(UUID parentId, String decidedBy, String outcome) {
        AppealDecision parent = repository.findById(parentId).orElseThrow(AppealException::notFound);

        if (repository.findByParentDecisionId(parentId).isPresent()) {
            throw AppealException.alreadyAppealed();
        }

        List<AppealDecision> chain = repository.findByChainRootIdOrderByLevelAsc(parent.getChainRootId());
        java.util.Set<String> priorDeciders = chain.stream()
            .map(AppealDecision::getDecidedBy)
            .collect(Collectors.toSet());
        if (priorDeciders.contains(decidedBy)) {
            throw AppealException.deciderNotIndependent(decidedBy);
        }

        AppealDecision appeal = new AppealDecision(UUID.randomUUID(), parent.getId(),
            parent.getChainRootId(), parent.getLevel() + 1, AppealDecisionKind.APPEAL,
            decidedBy, parent.getDecidedBy(), outcome, Instant.now(clock));
        return repository.save(appeal);
    }

    @Transactional(readOnly = true)
    public AppealDecision get(UUID id) {
        return repository.findById(id).orElseThrow(AppealException::notFound);
    }

    @Transactional(readOnly = true)
    public List<AppealDecision> chain(UUID chainRootId) {
        return repository.findByChainRootIdOrderByLevelAsc(chainRootId);
    }
}
