package com.demo.other;

/**
 * An UNRELATED enum from another domain that happens to carry the SAME constants as
 * com.demo.WidgetStatus — the shape that makes a copy-pasted FQCN invisible to a
 * set-equality check (real-tree analogue: sessionmanagement.SessionStatus and
 * apikey.ApiKeyStatus are both {ACTIVE, REVOKED}).
 */
public enum OtherStatus {
    OPEN,
    CLOSED
}
