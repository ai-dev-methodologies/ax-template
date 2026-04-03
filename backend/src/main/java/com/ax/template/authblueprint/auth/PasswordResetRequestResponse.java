package com.ax.template.authblueprint.auth;

public class PasswordResetRequestResponse {

    private final String message;

    public PasswordResetRequestResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
