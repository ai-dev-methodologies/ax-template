package com.ax.template.authblueprint.netting;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * collection-conservation-l0 sole orchestrator. {@link #net} reduces a run's directed gross
 * obligations to one signed net per member under the run's PESSIMISTIC_WRITE lock, in ONE transaction:
 * each net = Σ owed-to-member − Σ owed-by-member (per-node, NET-PER-NODE-001), the member nets sum to
 * EXACTLY 0 (set-wide, NET-SETWIDE-ZERO-001, asserted + DB @Check backstop), and NETTED is terminal
 * (net-once, NET-ONCE-001). Obligations are append-only inputs added only while OPEN.
 */
@Service
public class NettingService {

    static final int MONEY_SCALE = 4;

    private final NettingRunRepository runs;
    private final GrossObligationRepository obligations;
    private final NetPositionRepository positions;
    private final NettingMetrics metrics;
    private final Clock clock;

    public NettingService(NettingRunRepository runs, GrossObligationRepository obligations,
                          NetPositionRepository positions, NettingMetrics metrics, Clock clock) {
        this.runs = runs;
        this.obligations = obligations;
        this.positions = positions;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public NettingRun createRun(String runKey, String currency) {
        if (currency == null || currency.isBlank()) {
            metrics.record("create", "invalid");
            throw NettingException.invalidObligation();
        }
        String ccy = currency.strip().toUpperCase(Locale.ROOT);   // ISO-4217 canonical (case/space-insensitive)
        if (runs.existsByRunKey(runKey)) {
            metrics.record("create", "rejected");
            throw NettingException.duplicateRun();
        }
        try {
            NettingRun r = runs.saveAndFlush(new NettingRun(UUID.randomUUID(), runKey, ccy,
                NettingStatus.OPEN, zero(), Instant.now(clock)));
            metrics.record("create", "ok");
            return r;
        } catch (DataIntegrityViolationException e) {
            metrics.record("create", "rejected");
            throw NettingException.duplicateRun();
        }
    }

    /** NET-INPUTS-IMMUTABLE-001 / NET-PARTITION-001 — append a gross obligation while the run is OPEN. */
    @Transactional
    public GrossObligation addObligation(String runKey, String fromMember, String toMember,
                                         BigDecimal amount, String currency) {
        NettingRun run = runs.findByRunKeyForUpdate(runKey).orElseThrow(NettingException::notFound);
        if (run.getStatus() != NettingStatus.OPEN) {
            metrics.record("add", "run_not_open");
            throw NettingException.runNotOpen();
        }
        if (amount == null || fromMember == null || toMember == null || currency == null) {
            metrics.record("add", "invalid");
            throw NettingException.invalidObligation();
        }
        // Normalize member ids (trim) and currency (trim+upper) so a whitespace/case variant never splits
        // a member's net into two positions or spuriously mismatches the run currency.
        String from = fromMember.strip();
        String to = toMember.strip();
        String ccy = currency.strip().toUpperCase(Locale.ROOT);
        if (amount.signum() <= 0 || from.isEmpty() || to.isEmpty() || from.equals(to)) {
            metrics.record("add", "invalid");
            throw NettingException.invalidObligation();
        }
        if (!run.getCurrency().equals(ccy)) {                // NET-PARTITION-001 — one currency per run
            metrics.record("add", "currency_mismatch");
            throw NettingException.currencyMismatch();
        }
        GrossObligation o = obligations.save(new GrossObligation(UUID.randomUUID(), run.getId(),
            from, to, amount.setScale(MONEY_SCALE), ccy, Instant.now(clock)));
        metrics.record("add", "ok");
        return o;
    }

    /** NET-SETWIDE-ZERO-001 / NET-PER-NODE-001 / NET-ONCE-001 — the conserving reduction. */
    @Transactional
    public NettingRun net(String runKey) {
        NettingRun run = runs.findByRunKeyForUpdate(runKey).orElseThrow(NettingException::notFound);
        if (run.getStatus() != NettingStatus.OPEN) {         // net-once terminal
            metrics.record("net", "already_netted");
            throw NettingException.alreadyNetted();
        }
        List<GrossObligation> gross = obligations.findByRunId(run.getId());   // each row read once
        Map<String, BigDecimal> nets = new TreeMap<>();                       // deterministic order
        for (GrossObligation o : gross) {
            nets.merge(o.getToMember(), o.getAmount(), BigDecimal::add);          // creditor: + (received)
            nets.merge(o.getFromMember(), o.getAmount().negate(), BigDecimal::add); // debtor: − (sent)
        }
        BigDecimal total = zero();
        for (Map.Entry<String, BigDecimal> e : nets.entrySet()) {
            BigDecimal net = e.getValue().setScale(MONEY_SCALE);
            // PER-NODE INDEPENDENT cross-check (NET-PER-NODE-001): re-derive net from repository SUM
            // queries — a DIFFERENT code path than the in-memory merge — so a from/to swap, sign error,
            // or dropped obligation (which the set-wide Σ==0 identity CANNOT detect) is caught here.
            BigDecimal independent = obligations.sumOwedTo(run.getId(), e.getKey())
                .subtract(obligations.sumOwedBy(run.getId(), e.getKey())).setScale(MONEY_SCALE);
            if (net.compareTo(independent) != 0) {
                metrics.record("net", "not_conserved");
                throw NettingException.notConserved();
            }
            positions.save(new NetPosition(UUID.randomUUID(), run.getId(), e.getKey(), net));
            total = total.add(net);
        }
        if (total.signum() != 0) {           // set-wide closure — a structural identity of the two-legged
            metrics.record("net", "not_conserved");          // reduction (belt-and-suspenders; @Check backstops)
            throw NettingException.notConserved();
        }
        run.markNetted(total);                               // net_total = 0
        metrics.record("net", "ok");
        return run;
    }

    @Transactional(readOnly = true)
    public NettingRun getRun(String runKey) {
        return runs.findByRunKey(runKey).orElseThrow(NettingException::notFound);
    }

    @Transactional(readOnly = true)
    public Page<NetPosition> listPositions(String runKey, int page, int size) {
        NettingRun run = runs.findByRunKey(runKey).orElseThrow(NettingException::notFound);
        return positions.findByRunIdOrderByMemberAsc(run.getId(), PageRequest.of(clampPage(page), clampSize(size)));
    }

    @Transactional(readOnly = true)
    public Page<GrossObligation> listObligations(String runKey, int page, int size) {
        NettingRun run = runs.findByRunKey(runKey).orElseThrow(NettingException::notFound);
        return obligations.findByRunIdOrderByCreatedAtAsc(run.getId(), PageRequest.of(clampPage(page), clampSize(size)));
    }

    private static int clampPage(int p) { return Math.max(p, 0); }
    private static int clampSize(int s) { return Math.min(Math.max(s, 1), 200); }
    private static BigDecimal zero() { return BigDecimal.ZERO.setScale(MONEY_SCALE); }
}
