package com.example.emr;

import com.ax.template.authblueprint.common.Phi;

/** Entity with a PHI field — tagged via @Phi on the field. */
public class Encounter {

    private Long id;

    @Phi("diagnosis")
    private String diagnosis;

    public Long getId() {
        return id;
    }

    public String getDiagnosis() {
        return diagnosis;
    }
}
