package com.ax.template.authblueprint.mececlassification;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * mece-classification-l0 sole orchestrator for {@link ItemClassification}. The FIRST assignment for
 * a (scheme, item) pair creates the classification identity plus its first move (from=null); a
 * SECOND initial-assignment attempt is 409 (MECE-EXCLUSIVE-001 — reclassify instead). Every category
 * CHANGE appends a new immutable move (MECE-RECLASS-003) — the current category is always derived
 * from the latest move, never a mutable field. Rule-based classification
 * ({@link #classifyByAttribute}) NEVER fails open: an attribute matching no rule resolves to the
 * scheme's residual category (MECE-EXHAUSTIVE-002).
 */
@Service
public class ItemClassificationService {

    static final int MAX_MOVES = 500;

    private final ItemClassificationRepository classifications;
    private final ClassificationSchemeService schemes;
    private final MemberWriter members;
    private final Clock clock;

    public ItemClassificationService(ItemClassificationRepository classifications,
                                     ClassificationSchemeService schemes, MemberWriter members, Clock clock) {
        this.classifications = classifications;
        this.schemes = schemes;
        this.members = members;
        this.clock = clock;
    }

    /** MECE-EXCLUSIVE-001 — the FIRST assignment for (schemeKey, itemRef); a second attempt is 409. */
    @Transactional
    public ItemClassification classify(String schemeKey, String itemRef, String category, String actor, String reason) {
        schemes.getScheme(schemeKey);                                  // 404 if the scheme is missing
        if (classifications.existsBySchemeKeyAndItemRef(schemeKey, itemRef)) {
            throw MeceException.alreadyClassified();
        }
        UUID classificationId = UUID.randomUUID();
        Instant now = Instant.now(clock);
        try {
            ItemClassification saved = classifications.saveAndFlush(
                new ItemClassification(classificationId, schemeKey, itemRef, now));
            members.persist(new ClassificationMove(UUID.randomUUID(), classificationId, null, category, actor, reason, now));
            return saved;
        } catch (DataIntegrityViolationException e) {                 // lost the uq(scheme_key, item_ref) race
            throw MeceException.alreadyClassified();
        }
    }

    /** MECE-EXHAUSTIVE-002 — resolve the category by matching {@code attributeValue} against the
     *  scheme's rules; NO match falls through to the residual category, never a rejection. */
    @Transactional
    public ItemClassification classifyByAttribute(String schemeKey, String itemRef, String attributeValue,
                                                   String actor, String reason) {
        ClassificationScheme scheme = schemes.getScheme(schemeKey);
        String category = schemes.resolveCategory(schemeKey, attributeValue).orElse(scheme.getResidualCategory());
        return classify(schemeKey, itemRef, category, actor, reason);
    }

    /** MECE-RECLASS-003 — append a new move; the FROM is the item's CURRENT (latest-move) category. */
    @Transactional
    public ItemClassification reclassify(String schemeKey, String itemRef, String newCategory, String actor, String reason) {
        ItemClassification ic = classifications.findBySchemeKeyAndItemRef(schemeKey, itemRef)
            .orElseThrow(MeceException::notClassified);
        String current = latestCategory(ic.getId());
        members.persist(new ClassificationMove(
            UUID.randomUUID(), ic.getId(), current, newCategory, actor, reason, Instant.now(clock)));
        return ic;
    }

    @Transactional(readOnly = true)
    public String currentCategory(String schemeKey, String itemRef) {
        ItemClassification ic = classifications.findBySchemeKeyAndItemRef(schemeKey, itemRef)
            .orElseThrow(MeceException::notClassified);
        return latestCategory(ic.getId());
    }

    private String latestCategory(UUID classificationId) {
        List<ClassificationMove> latest = classifications.findMovesLatestFirst(classificationId, PageRequest.of(0, 1));
        return latest.isEmpty() ? null : latest.get(0).getToCategory();
    }

    @Transactional(readOnly = true)
    public List<ClassificationMove> history(String schemeKey, String itemRef) {
        ItemClassification ic = classifications.findBySchemeKeyAndItemRef(schemeKey, itemRef)
            .orElseThrow(MeceException::notClassified);
        return classifications.findMoves(ic.getId(), PageRequest.of(0, MAX_MOVES));
    }

    /** MECE-EXHAUSTIVE-002 — the residual (or any) category's current population, visibly queryable. */
    @Transactional(readOnly = true)
    public long countInCategory(String schemeKey, String category) {
        schemes.getScheme(schemeKey);                                  // 404 if the scheme is missing
        return classifications.countCurrentByCategory(schemeKey, category);
    }
}
