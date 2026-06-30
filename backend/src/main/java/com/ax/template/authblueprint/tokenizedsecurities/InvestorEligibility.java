package com.ax.template.authblueprint.tokenizedsecurities;

import java.util.UUID;

/**
 * 수취인 적격성 판정 seam. 기본 구현은 deny-by-default allowlist.
 * fork는 이를 on-chain ONCHAINID / KYC 어댑터로 교체한다.
 */
public interface InvestorEligibility {
    /** @return true ONLY when the holder is positively eligible for this register; absence ⇒ false. */
    boolean isEligible(UUID registerId, String holderId);
}
