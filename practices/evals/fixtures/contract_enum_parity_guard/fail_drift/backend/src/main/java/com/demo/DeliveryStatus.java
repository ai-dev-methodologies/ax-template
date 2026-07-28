package com.demo;

/** The delivery vocabulary the contract serializes — the SOURCE a vocab_scan resolves. */
public enum DeliveryStatus {
    PENDING,
    PENDING_RETRY,
    SUCCEEDED,
    FAILED_PERMANENT
}
