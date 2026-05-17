package com.ax.template.authblueprint.auth;

import jakarta.validation.constraints.NotBlank;

public class VerifyEmailRequest {

    @NotBlank
    private String token;

    public VerifyEmailRequest() {}

    public VerifyEmailRequest(String token) {
        this.token = token;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
