package com.ax.template.authblueprint.consumerproof;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// VIOLATING — entity_migration_guard
// @Entity mapped to table `widget_ghost`, but NO V*.sql migration creates that
// table — entity<->migration drift that ddl-auto:create-drop hides.
@Entity
@Table(name = "widget_ghost")
public class Widget {
    @Id
    private Long id;
}
