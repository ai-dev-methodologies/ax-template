package com.ax.template.authblueprint.practices;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.ax.template.authblueprint.common.AggregateRoot;

/**
 * Fixture entity for PRACTICES-PERS-005: soft-delete via @SQLDelete + @Where.
 *
 * <p>Demonstrates the correct pattern:
 * <ul>
 *   <li>{@code @SQLDelete} — Hibernate converts deleteById() to UPDATE ... SET deleted_at = NOW()</li>
 *   <li>{@code @Where} — all standard JPQL queries automatically exclude deleted rows</li>
 *   <li>{@code @Version} — optimistic locking from BaseEntity contract</li>
 * </ul>
 *
 * <p>This entity does NOT extend BaseEntity (the template abstract class is in com.example.app).
 * It replicates the BaseEntity fields inline to keep the fixture self-contained in the
 * backend test application.
 */
@AggregateRoot
@Entity
@Table(name = "soft_deleted_records")
@SQLDelete(sql = "UPDATE soft_deleted_records SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@Where(clause = "deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
public class SoftDeletedRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String label;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 64)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 64)
    private String lastModifiedBy;

    @Version
    @Column(nullable = false)
    private Long version;

    /** Soft-delete timestamp. NULL = active. Set by @SQLDelete — do not set directly. */
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public UUID getId()                { return id; }
    public String getLabel()           { return label; }
    public Instant getCreatedAt()      { return createdAt; }
    public Instant getUpdatedAt()      { return updatedAt; }
    public String getCreatedBy()       { return createdBy; }
    public String getLastModifiedBy()  { return lastModifiedBy; }
    public Long getVersion()           { return version; }
    public Instant getDeletedAt()      { return deletedAt; }
    public boolean isDeleted()         { return deletedAt != null; }

    public void setLabel(String label) { this.label = label; }
}
