package com.ax.template.authblueprint.widget;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

// IMW3 / IDW3 G4 regression fixture — the INLINE @Entity @Table pair on ONE line
// with NO backing migration and an EMPTY allowlist. Under the OLD ENTITY_RE
// `^\s*@Entity\s*(\(|$)` this entity escaped detection and the guard exited 0
// (the false negative). Under the widened `(?m)^\s*@Entity\b` it is DETECTED:
// table "widget" has no CREATE/ALTER TABLE migration → the guard exits 1.
@Entity @Table(name = "widget")
public class WidgetEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    public UUID getId() { return id; }
}
