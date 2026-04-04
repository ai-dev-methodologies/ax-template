package com.ax.template.authblueprint.auth;

public class InvalidOAuthStateException extends RuntimeException {
    public InvalidOAuthStateException(String message) {
        super(message);
    }
}
