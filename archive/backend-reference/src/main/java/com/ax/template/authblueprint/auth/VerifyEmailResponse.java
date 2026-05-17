package com.ax.template.authblueprint.auth;

public class VerifyEmailResponse {

    private final String message;

    public VerifyEmailResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
