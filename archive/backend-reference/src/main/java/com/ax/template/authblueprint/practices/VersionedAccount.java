package com.ax.template.authblueprint.practices;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Fixture for PRACTICES-PERS-004: optimistic locking via JPA @Version.
 * The @Version column is incremented on every persist; concurrent writes that started
 * from the same row read but reach commit with stale versions throw OptimisticLockException
 * (or its JPA equivalent) instead of silently losing one update.
 */
@Entity
public class VersionedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String holder;

    private long balance;

    @Version
    private long version;

    public Long getId() {
        return id;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public long getVersion() {
        return version;
    }
}
