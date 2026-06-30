package com.ax.template.authblueprint.tokenizedsecurities;

import java.time.Clock;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EligibleInvestorService {

    private final EligibleInvestorRepository grants;
    private final SecurityTokenRegisterRepository registers;
    private final Clock clock;

    public EligibleInvestorService(EligibleInvestorRepository grants,
                                   SecurityTokenRegisterRepository registers, Clock clock) {
        this.grants = grants;
        this.registers = registers;
        this.clock = clock;
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
