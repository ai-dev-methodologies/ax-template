package com.ax.template.authblueprint.auth;

public class PasswordChangeResponse {

    private final String message;

    public PasswordChangeResponse(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
