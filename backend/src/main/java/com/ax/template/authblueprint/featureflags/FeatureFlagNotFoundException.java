package com.ax.template.authblueprint.featureflags;

public class FeatureFlagNotFoundException extends RuntimeException {
    public FeatureFlagNotFoundException(String name) {
        super("Feature flag not found: name=" + name);
    }
}
