package com.ax.template.authblueprint.auth;

public class ResendVerificationResponse {

    private final String message;

    public ResendVerificationResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
