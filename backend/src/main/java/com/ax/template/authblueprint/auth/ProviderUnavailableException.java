package com.ax.template.authblueprint.auth;

public class ProviderUnavailableException extends RuntimeException {
    private final String provider;

    public ProviderUnavailableException(String provider, Throwable cause) {
        super("Provider unavailable: " + provider, cause);
        this.provider = provider;
    }

    public String getProvider() { return provider; }
}
