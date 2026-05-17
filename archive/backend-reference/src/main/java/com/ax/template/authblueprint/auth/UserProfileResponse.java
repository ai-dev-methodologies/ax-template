package com.ax.template.authblueprint.auth;

import java.util.List;

public record UserProfileResponse(
        String userId,
        String email,
        String role,
        boolean emailVerified,
        List<String> linkedProviders) {}
