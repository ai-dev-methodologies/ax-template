package com.ax.template.authblueprint.tokenizedsecurities;

/** One-way issuance lifecycle for a security token (mirrors ThresholdRegisterStateMachine pattern). */
public enum IssuanceStatus {
    DRAFT,
    ISSUED
}
