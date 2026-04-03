package com.ax.template.authblueprint.auth;

public class SignupResponse {

    private final String userId;
    private final String message;

    public SignupResponse(String userId, String message) {
        this.userId = userId;
        this.message = message;
    }

    public String getUserId() { return userId; }
    public String getMessage() { return message; }
}
