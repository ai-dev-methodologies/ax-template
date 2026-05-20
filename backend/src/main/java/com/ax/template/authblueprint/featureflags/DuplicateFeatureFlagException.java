package com.ax.template.authblueprint.featureflags;

public class DuplicateFeatureFlagException extends RuntimeException {
    public DuplicateFeatureFlagException(String name) {
        super("Feature flag already exists: name=" + name);
    }
}
