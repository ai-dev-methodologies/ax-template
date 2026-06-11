package com.ax.template.authblueprint.recordlinkage;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * record-linkage-l0 sole orchestrator. The deterministic reference scorer weights normalized
 * exact agreement (fullName 0.5, birthDate 0.3, identifier 0.2) and bands Fellegi-Sunter-style
 * at LOWER=0.5 / UPPER=0.8 — the GOVERNANCE contract (recorded verdict, human-owned REVIEW
 * band, survivorship merge, tombstone-never-delete) is the catalog's claim; a fork-receiver
 * swaps scorers freely. All write-paths take the proposal's and BOTH records' PESSIMISTIC_WRITE
 * locks in ascending-id order (LINK-CONCURRENT-001). Survivorship rows are members:
 * {@link MemberWriter} writes, root-JPQL reads.
 */
@Service
public class LinkageService {

    static final BigDecimal LOWER = new BigDecimal("0.5000");
    static final BigDecimal UPPER = new BigDecimal("0.8000");
    static final BigDecimal W_NAME = new BigDecimal("0.5000");
    static final BigDecimal W_BIRTH = new BigDecimal("0.3000");
    static final BigDecimal W_IDENT = new BigDecimal("0.2000");
    static final List<String> IDENTITY_FIELDS = List.of("fullName", "birthDate", "identifier");
    /** The reference survivorship rule, recorded verbatim on every decision row. */
    static final String RULE_PREFER_SURVIVOR_NON_BLANK = "PREFER_SURVIVOR_NON_BLANK";

    private final LinkageRecordRepository records;
    private final MatchProposalRepository proposals;
    private final MemberWriter members;
    private final LinkageMetrics metrics;
    private final Clock clock;

    public LinkageService(LinkageRecordRepository records, MatchProposalRepository proposals,
                          MemberWriter members, LinkageMetrics metrics, Clock clock) {
        this.records = records;
        this.proposals = proposals;
        this.members = members;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional
    public LinkageRecord createRecord(String fullName, LocalDate birthDate, String identifier) {
        return records.save(new LinkageRecord(UUID.randomUUID(), fullName, birthDate,
            identifier, Instant.now(clock)));
    }

    /** LINK-BAND-001 — score, break down, record thresholds, band; AUTO_MATCH merges in-tx. */
    @Transactional
    public MatchProposal propose(UUID aId, UUID bId, String proposer) {
        if (Objects.equals(aId, bId)) {
            metrics.record("propose", "invalid");
            throw LinkageException.selfPair();
        }
        UUID lowId = aId.compareTo(bId) < 0 ? aId : bId;          // ascending-id lock order
        UUID highId = aId.compareTo(bId) < 0 ? bId : aId;
        LinkageRecord low = records.findByIdForUpdate(lowId).orElseThrow(LinkageException::notFound);
        LinkageRecord high = records.findByIdForUpdate(highId).orElseThrow(LinkageException::notFound);
        requireActive(low);
        requireActive(high);

        BigDecimal nameScore = agree(normalize(low.getFullName()), normalize(high.getFullName())) ? W_NAME : BigDecimal.ZERO;
        BigDecimal birthScore = low.getBirthDate() != null && low.getBirthDate().equals(high.getBirthDate()) ? W_BIRTH : BigDecimal.ZERO;
        BigDecimal identScore = agree(normalize(low.getIdentifier()), normalize(high.getIdentifier())) ? W_IDENT : BigDecimal.ZERO;
        BigDecimal total = nameScore.add(birthScore).add(identScore);
        String breakdown = "{\"fullName\":" + nameScore.stripTrailingZeros().toPlainString()
            + ",\"birthDate\":" + birthScore.stripTrailingZeros().toPlainString()
            + ",\"identifier\":" + identScore.stripTrailingZeros().toPlainString() + "}";
        MatchBand band = total.compareTo(UPPER) >= 0 ? MatchBand.AUTO_MATCH
            : total.compareTo(LOWER) >= 0 ? MatchBand.REVIEW : MatchBand.NO_MATCH;

        MatchProposal p = proposals.saveAndFlush(new MatchProposal(UUID.randomUUID(), lowId, highId,
            total, breakdown, LOWER, UPPER, band, Instant.now(clock)));
        if (band == MatchBand.AUTO_MATCH) {
            p.decide(ProposalStatus.CONFIRMED, "AUTO", Instant.now(clock));   // same trail, no human
            executeMerge(p, low, high);
            metrics.record("propose", "auto_merged");
        } else {
            metrics.record("propose", "ok");
        }
        return p;
    }

    /** LINK-REVIEW-001 — only an explicit human confirm decides the REVIEW band (and merges). */
    @Transactional
    public MatchProposal confirm(UUID proposalId, String decider) {
        MatchProposal p = proposals.findByIdForUpdate(proposalId).orElseThrow(LinkageException::notFound);
        if (p.getStatus() != ProposalStatus.PROPOSED) {
            metrics.record("confirm", "rejected");
            throw LinkageException.alreadyDecided();
        }
        if (p.getBand() == MatchBand.NO_MATCH) {
            metrics.record("confirm", "invalid");
            throw LinkageException.notConfirmable();
        }
        LinkageRecord low = records.findByIdForUpdate(p.getLowRecordId()).orElseThrow(LinkageException::notFound);
        LinkageRecord high = records.findByIdForUpdate(p.getHighRecordId()).orElseThrow(LinkageException::notFound);
        requireActive(low);                                       // captured by another merge → 409
        requireActive(high);
        p.decide(ProposalStatus.CONFIRMED, decider, Instant.now(clock));
        executeMerge(p, low, high);
        metrics.record("confirm", "ok");
        return p;
    }

    @Transactional
    public MatchProposal reject(UUID proposalId, String decider) {
        MatchProposal p = proposals.findByIdForUpdate(proposalId).orElseThrow(LinkageException::notFound);
        if (p.getStatus() != ProposalStatus.PROPOSED) {
            metrics.record("reject", "rejected");
            throw LinkageException.alreadyDecided();
        }
        p.decide(ProposalStatus.REJECTED, decider, Instant.now(clock));
        metrics.record("reject", "ok");
        return p;
    }

    /** LINK-SURVIVOR-001 — the LOW record survives (deterministic); one decision per field. */
    private void executeMerge(MatchProposal p, LinkageRecord survivor, LinkageRecord loser) {
        Instant now = Instant.now(clock);
        for (String field : IDENTITY_FIELDS) {
            String survivorValue = valueOf(survivor, field);
            String loserValue = valueOf(loser, field);
            boolean survivorWins = survivorValue != null && !survivorValue.isBlank();
            String winning = survivorWins ? survivorValue : loserValue;
            UUID source = survivorWins ? survivor.getId() : loser.getId();
            members.persist(new SurvivorshipDecision(UUID.randomUUID(), p.getId(), field,
                winning, winning == null ? null : source, RULE_PREFER_SURVIVOR_NON_BLANK, now));
            survivor.applySurvivorship(field, winning);
        }
        loser.tombstone(survivor.getId());                        // MERGED + pointer — never deleted
    }

    @Transactional(readOnly = true)
    public LinkageRecord get(UUID id) {
        return records.findById(id).orElseThrow(LinkageException::notFound);
    }

    /** LINK-RESOLVE-001 — follow merged-into chains, cycle-safe, to the living survivor. */
    @Transactional(readOnly = true)
    public LinkageRecord resolve(UUID id) {
        Set<UUID> visited = new HashSet<>();
        LinkageRecord current = get(id);
        while (current.getStatus() == RecordStatus.MERGED) {
            if (!visited.add(current.getId())) {
                throw LinkageException.resolutionCycle();
            }
            current = get(current.getMergedIntoId());
        }
        metrics.record("resolve", "ok");
        return current;
    }

    @Transactional(readOnly = true)
    public MatchProposal getProposal(UUID id) {
        return proposals.findById(id).orElseThrow(LinkageException::notFound);
    }

    @Transactional(readOnly = true)
    public List<SurvivorshipDecision> decisions(UUID proposalId) {
        getProposal(proposalId);                                  // 404 before an empty list
        return proposals.findDecisions(proposalId);
    }

    private static void requireActive(LinkageRecord r) {
        if (r.getStatus() == RecordStatus.MERGED) {
            throw LinkageException.participantMerged();
        }
    }

    private static boolean agree(String a, String b) {
        return a != null && !a.isBlank() && a.equals(b);
    }

    private static String normalize(String v) {
        return v == null ? null : v.strip().toLowerCase().replaceAll("\\s+", " ");
    }

    private static String valueOf(LinkageRecord r, String field) {
        return switch (field) {
            case "fullName" -> r.getFullName();
            case "birthDate" -> r.getBirthDate() == null ? null : r.getBirthDate().toString();
            case "identifier" -> r.getIdentifier();
            default -> throw new IllegalArgumentException("unknown identity field: " + field);
        };
    }
}
