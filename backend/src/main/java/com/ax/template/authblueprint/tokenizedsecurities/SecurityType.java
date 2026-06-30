package com.ax.template.authblueprint.tokenizedsecurities;

/** 자본시장법상 토큰화 대상 증권 유형 (조각투자 두 갈래). */
public enum SecurityType {
    /** 비금전신탁 수익증권 — 기초자산(채권·대출채권 등)을 신탁 후 발행. */
    TRUST_BENEFICIARY,
    /** 투자계약증권 — 기초자산 공유지분 양도 후 발행. */
    INVESTMENT_CONTRACT
}
