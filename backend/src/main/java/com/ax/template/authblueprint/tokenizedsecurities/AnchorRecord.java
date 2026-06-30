package com.ax.template.authblueprint.tokenizedsecurities;

/** An anchor record stored by the OnChainAnchor; used for reconciliation. */
public record AnchorRecord(String transferId, String fromHolderId,
                           String toHolderId, long units, String txRef) {}
