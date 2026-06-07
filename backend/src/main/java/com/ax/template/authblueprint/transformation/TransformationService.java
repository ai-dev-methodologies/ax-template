package com.ax.template.authblueprint.transformation;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * transformation-conservation-l0 sole orchestrator. A record is validated by the pure
 * {@link ConservationCheck} (Σinput == Σgood + Σresidual exact, per base unit, every residual
 * classified) BEFORE anything is persisted; only on success are the immutable run + legs written in
 * ONE transaction (XFORM-ATOMIC-001) — a rejected transformation persists nothing.
 */
@Service
public class TransformationService {

    private static final int MONEY_SCALE = 4;

    public record RecordResult(TransformationRun run, List<TransformationLeg> legs) {}

    private final TransformationRunRepository runRepo;
    private final TransformationLegRepository legRepo;
    private final TransformationMetrics metrics;
    private final Clock clock;

    public TransformationService(TransformationRunRepository runRepo, TransformationLegRepository legRepo,
                                 TransformationMetrics metrics, Clock clock) {
        this.runRepo = runRepo;
        this.legRepo = legRepo;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public RecordResult record(String createdBy, List<ConservationCheck.Leg> legs) {
        ConservationCheck.Result r;
        try {
            r = ConservationCheck.check(legs);     // throws BEFORE any persist -> nothing saved on failure
        } catch (TransformationException e) {
            metrics.record(e.code());
            throw e;
        }
        UUID runId = UUID.randomUUID();
        TransformationRun run = runRepo.save(new TransformationRun(runId, createdBy, r.baseUnit(),
            r.totalInput().setScale(MONEY_SCALE), r.totalGood().setScale(MONEY_SCALE),
            r.totalResidual().setScale(MONEY_SCALE), Instant.now(clock)));
        for (ConservationCheck.Leg leg : legs) {
            legRepo.save(new TransformationLeg(UUID.randomUUID(), runId, leg.role(), leg.disposition(),
                leg.materialCode(), leg.qty().setScale(MONEY_SCALE), leg.unit().trim()));
        }
        metrics.record("recorded");
        return new RecordResult(run, legRepo.findByRunIdOrderByRoleAsc(runId, PageRequest.of(0, 200)));
    }

    @Transactional(readOnly = true)
    public RecordResult get(UUID runId) {
        TransformationRun run = runRepo.findById(runId).orElseThrow(TransformationException::notFound);
        return new RecordResult(run, legRepo.findByRunIdOrderByRoleAsc(runId, PageRequest.of(0, 200)));
    }
}
