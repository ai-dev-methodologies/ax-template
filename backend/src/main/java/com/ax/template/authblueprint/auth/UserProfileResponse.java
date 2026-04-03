package com.ax.template.authblueprint.auth;

public record UserProfileResponse(String userId, String email, String role, boolean emailVerified) {}
