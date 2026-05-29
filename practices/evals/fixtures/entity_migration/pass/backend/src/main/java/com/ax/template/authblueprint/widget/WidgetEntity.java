package com.ax.template.authblueprint.widget;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

// IMW3 / IDW3 G4 regression fixture — the INLINE @Entity @Table pair on ONE line.
// The previous ENTITY_RE `^\s*@Entity\s*(\(|$)` did NOT match this form, so an
// un-migrated entity declared this way shipped GREEN. In the PASS fixture this
// entity IS backed by a CREATE TABLE widget migration, so the guard exits 0 —
// proving the widened `(?m)^\s*@Entity\b` anchor still resolves the inline
// @Table(name="widget") and recognises the backing migration.
@Entity @Table(name = "widget")
public class WidgetEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public UUID getId() { return id; }
}
