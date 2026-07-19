package com.ax.template.authblueprint.consumerproof;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// CLEAN — @Entity mapped to `widget`, backed by V001__create_widget.sql.
@Entity
@Table(name = "widget")
public class Widget {
    @Id
    private Long id;
}
