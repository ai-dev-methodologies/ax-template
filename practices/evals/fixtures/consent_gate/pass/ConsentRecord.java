package com.example.share;

import jakarta.persistence.Entity;

/**
 * Adoption marker for the consent_gate guard fixtures: a tree that declares a
 * ConsentRecord @Entity has adopted the consent ledger, so the guard scans its
 * data-sharing methods. (Minimal stub — real shape is common/ConsentRecord.)
 */
@Entity
public class ConsentRecord {
}
