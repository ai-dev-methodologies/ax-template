package com.ax.template.authblueprint.mececlassification;

import com.ax.template.authblueprint.common.MemberWriter;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * mece-classification-l0 sole orchestrator for {@link ClassificationScheme}. Declaring a scheme
 * REQUIRES a non-blank residual category (MECE-EXHAUSTIVE-002 — 422 without one); a scheme, once
 * declared, is immutable. Rules are additive members: {@link #resolveCategory} is the exhaustiveness
 * fallback every rule-based classification consults — no match falls through to the caller (the
 * residual bucket, resolved by {@link ItemClassificationService}), never a classification failure.
 */
@Service
public class ClassificationSchemeService {

    static final int MAX_RULES = 500;

    private final ClassificationSchemeRepository schemes;
    private final MemberWriter members;
    private final Clock clock;

    public ClassificationSchemeService(ClassificationSchemeRepository schemes, MemberWriter members, Clock clock) {
        this.schemes = schemes;
        this.members = members;
        this.clock = clock;
    }

    @Transactional
    public ClassificationScheme declare(String schemeKey, String residualCategory) {
        if (residualCategory == null || residualCategory.isBlank()) {
            throw MeceException.missingResidual();
        }
        if (schemes.existsBySchemeKey(schemeKey)) {
            throw MeceException.duplicateScheme();
        }
        try {
            return schemes.saveAndFlush(
                new ClassificationScheme(UUID.randomUUID(), schemeKey, residualCategory, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {                 // lost the uq(scheme_key) race
            throw MeceException.duplicateScheme();
        }
    }

    @Transactional
    public ClassificationRule addRule(String schemeKey, String matchValue, String category) {
        getScheme(schemeKey);                                         // 404 if the scheme is missing
        try {
            return members.persistAndFlush(
                new ClassificationRule(UUID.randomUUID(), schemeKey, matchValue, category, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {                 // lost the uq(scheme_key, match_value) race
            throw MeceException.duplicateRule();
        }
    }

    @Transactional(readOnly = true)
    public ClassificationScheme getScheme(String schemeKey) {
        return schemes.findBySchemeKey(schemeKey).orElseThrow(MeceException::schemeNotFound);
    }

    @Transactional(readOnly = true)
    public List<ClassificationRule> rules(String schemeKey) {
        getScheme(schemeKey);                                         // 404 before an empty list
        return schemes.findRules(schemeKey, PageRequest.of(0, MAX_RULES));
    }

    /** MECE-EXHAUSTIVE-002 — the rule lookup an attribute-based classify consults before falling back
     *  to the scheme's residual category. Empty means "no rule matched", NOT "reject". */
    @Transactional(readOnly = true)
    Optional<String> resolveCategory(String schemeKey, String attributeValue) {
        return schemes.findRuleByMatch(schemeKey, attributeValue).map(ClassificationRule::getCategory);
    }
}
