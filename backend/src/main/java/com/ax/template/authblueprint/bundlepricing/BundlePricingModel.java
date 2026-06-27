package com.ax.template.authblueprint.bundlepricing;

/**
 * The two pricing modes a composite item (bundle / kit) can be priced in — absorbed from
 * Broadleaf {@code ProductBundlePricingModelType} (ITEM_SUM / BUNDLE).
 *
 * <ul>
 *   <li>{@link #ITEM_SUM} — the composite price is the CONSERVING roll-up of its children:
 *       Σ over children of (unitPrice × quantity) + Σ(bundle fees). (BUNDLE-ITEMSUM-001)</li>
 *   <li>{@link #BUNDLE} — the composite price is a FIXED base price, NOT summed from the
 *       children. (BUNDLE-FIXED-001)</li>
 * </ul>
 */
public enum BundlePricingModel {
    ITEM_SUM,
    BUNDLE
}
