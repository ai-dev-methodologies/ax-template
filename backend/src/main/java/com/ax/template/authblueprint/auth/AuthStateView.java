package com.ax.template.authblueprint.auth;

import com.ax.template.authblueprint.user.ProviderLink;
import com.ax.template.authblueprint.user.UserRole;
import com.ax.template.authblueprint.user.VerificationState;

import java.util.List;

public record AuthStateView(
    String userId,
    String email,
    List<UserRole> roles,
    VerificationState verificationState,
    List<ProviderLink> providerLinks
) {
}
