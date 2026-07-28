package com.demo;

/** The delivery vocabulary the contract serializes — the SOURCE a vocab_scan resolves. */
public enum DeliveryStatus {
    PENDING,
    PENDING_RETRY,
    DELIVERING,
    SUCCEEDED,
    FAILED_PERMANENT
}
