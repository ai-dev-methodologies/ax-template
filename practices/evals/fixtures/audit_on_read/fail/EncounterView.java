package com.example.emr;

import com.ax.template.authblueprint.common.Phi;

/** A DTO that exposes a patient's diagnosis — tagged PHI via @Phi. */
public record EncounterView(Long id, @Phi("diagnosis") String diagnosis) {}
