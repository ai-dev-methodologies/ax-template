package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibleInvestorService {

    static final int MAX_PAGE_SIZE = 200;

    private final EligibleInvestorRepository grants;
    private final SecurityTokenRegisterRepository registers;
    private final Clock clock;

    public EligibleInvestorService(EligibleInvestorRepository grants,
                                   SecurityTokenRegisterRepository registers, Clock clock) {
        this.grants = grants;
        this.registers = registers;
        this.clock = clock;
    }

    /**
     * READ-ELIGIBLE-001: paginated list of eligible investors for a token.
     * Returns 404 if tokenCode doesn't exist. ADMIN-only enforced at controller.
     */
    @Transactional(readOnly = true)
    public Page<EligibleInvestor> listByTokenCode(String tokenCode, int page, int size) {
        SecurityTokenRegister register = registers.findByTokenCode(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Order.asc("id")));
        return grants.findByRegisterId(register.getId(), pageable);
    }

    /** Reads the register (one aggregate) to resolve its id, then writes ONE EligibleInvestor. */
    @Transactional
    public EligibleInvestor grant(String tokenCode, String holderId) {
        SecurityTokenRegister register = registers.findByTokenCode(tokenCode)
                .orElseThrow(TokenizedSecuritiesException::notFound);
        if (grants.existsByRegisterIdAndHolderId(register.getId(), holderId)) {
            return grants.findByRegisterIdAndHolderId(register.getId(), holderId)
                    .orElseThrow(TokenizedSecuritiesException::notFound);
        }
        try {
            return grants.saveAndFlush(new EligibleInvestor(register.getId(), holderId, Instant.now(clock)));
        } catch (DataIntegrityViolationException e) {
            // concurrent duplicate grant — idempotent
            return grants.findByRegisterIdAndHolderId(register.getId(), holderId)
                    .orElseThrow(TokenizedSecuritiesException::notFound);
        }
    }
}
