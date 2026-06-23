package com.ax.template.authblueprint.reproducibility;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * reproducible-procedure-l0 sole orchestrator. A DRAW records a server-generated seed + algorithm +
 * canonical input hash + selected ids (PROC-DRAW-001) and is REPLAYABLE: replay re-runs the recorded
 * algorithm with the recorded seed over the recorded input set and reproduces the byte-identical
 * selection, mutating nothing (PROC-REPLAY-001 — a divergence fails closed 422). A CLASSIFICATION
 * records the SHA-256 input hash + classifier version + resolved class and is idempotent per
 * (input_hash, version): the same input under the same version returns the existing row, a newer
 * version records a SEPARATE result rather than re-labeling history (PROC-CLASS-001). A sensitive
 * subject is stored {@code @JsonIgnore} raw and reached only by an ADMIN unmask (PROC-BLIND-001).
 * The reference PRNG / classifier are a fork-receiver swap behind the recorded-seed-replays +
 * version-pinned + role-blinded governance contract.
 */
@Service
public class ReproducibilityService {

    private final ProcedureRepository procedures;
    private final ReproducibilityMetrics metrics;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();   // seeds DRAWs server-side (CWE-330)

    public ReproducibilityService(ProcedureRepository procedures, ReproducibilityMetrics metrics,
                                  Clock clock) {
        this.procedures = procedures;
        this.metrics = metrics;
        this.clock = clock;
    }

    /** PROC-DRAW-001 — record a draw with a SERVER-generated seed; the result is replayable. */
    @Transactional
    public Procedure draw(String inputSetRef, List<String> candidates, int k, String rawSubject,
                          String actor) {
        long seed = secureRandom.nextLong();                            // seed generated SERVER-side
        List<String> sorted = candidates.stream().sorted().toList();    // canonical order → stable input hash
        String inputHash = Hashing.sha256Hex(String.join(",", sorted)); // recorded basis
        List<String> selected = SeededDraw.select(sorted, k, seed);     // deterministic from (sorted, k, seed)
        Procedure p = Procedure.draw(UUID.randomUUID(), inputSetRef, inputHash, SeededDraw.ALGORITHM,
            seed, k, String.join(",", sorted), String.join(",", selected), rawSubject, actor,
            Instant.now(clock));
        metrics.record("draw", "ok");
        return procedures.save(p);                                      // seed/algorithm/input_hash/selected immutable
    }

    /** PROC-REPLAY-001 — re-derive the byte-identical selection from the recorded seed; mutate nothing. */
    @Transactional(readOnly = true)
    public List<String> replay(UUID id) {
        Procedure p = procedures.findById(id).orElseThrow(ReproducibilityException::notFound);
        if (p.getKind() != ProcedureKind.DRAW) {
            metrics.record("replay", "not_replayable");
            throw ReproducibilityException.notReplayable();
        }
        List<String> replayed = SeededDraw.select(p.sortedCandidates(), p.getDrawK(), p.getSeed());
        if (!replayed.equals(p.selectedIdList())) {                     // pure verification; divergence fails closed
            metrics.record("replay", "diverged");
            throw ReproducibilityException.replayDiverged();            // 422 — never silently overwrite
        }
        metrics.record("replay", "ok");
        return replayed;                                                // byte-identical to the recorded selection
    }

    /** PROC-CLASS-001 — classify pinned to a version; same input + same version is idempotent. */
    @Transactional
    public Procedure classify(String inputSetRef, String input, String classifierVersion,
                              String resolvedClass, String rawSubject, String actor) {
        String inputHash = Hashing.sha256Hex(input);
        // PESSIMISTIC_WRITE serializes concurrent same-(input,version) classify so one records and
        // the rest return the existing row — byte-identical, never a divergent duplicate.
        return procedures.findClassificationForUpdate(inputHash, classifierVersion)
            .map(existing -> {
                metrics.record("classify", "idempotent");
                return existing;                                        // same input + version → same result
            })
            .orElseGet(() -> {
                Procedure p = Procedure.classify(UUID.randomUUID(), inputSetRef, inputHash,
                    classifierVersion, resolvedClass, rawSubject, actor, Instant.now(clock));
                metrics.record("classify", "ok");
                return procedures.save(p);                              // a NEW version → a SEPARATE row
            });
    }

    @Transactional(readOnly = true)
    public Procedure get(UUID id) {
        return procedures.findById(id).orElseThrow(ReproducibilityException::notFound);
    }

    /** PROC-BLIND-001 — the raw blinded subject, reached only by an ADMIN (gated in the controller). */
    @Transactional(readOnly = true)
    public String unmask(UUID id) {
        Procedure p = procedures.findById(id).orElseThrow(ReproducibilityException::notFound);
        metrics.record("unmask", "ok");
        return p.rawSubject();
    }
}
