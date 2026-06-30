package com.ax.template.authblueprint.tokenizedsecurities;

/** Intent payload passed to the OnChainAnchor SPI for each applied transfer. */
public record AnchorIntent(String tokenCode, String transferId,
                           String fromHolderId, String toHolderId, long units) {}
