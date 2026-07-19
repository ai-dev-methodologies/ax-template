package com.ax.template.authblueprint.apikey;

public enum ApiKeyScope {
    READ,
    WRITE;

    public String toAuthority() {
        return switch (this) {
            case READ  -> "ROLE_API_READ";
            case WRITE -> "ROLE_API_WRITE";
        };
    }
}
