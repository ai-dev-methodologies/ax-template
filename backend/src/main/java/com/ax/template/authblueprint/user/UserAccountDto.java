package com.ax.template.authblueprint.user;

import com.ax.template.authblueprint.common.PublishedApi;

import java.util.UUID;

/**
 * Published account view — the {@code user} feature's cross-feature read surface
 * (DDD decomposition: AX-DDD-AUTH-USER retire, BACKLOG P0-1~11). The {@code auth} feature
 * consumes THIS instead of the {@link UserEntity} aggregate root.
 *
 * <p>{@code hasPassword} deliberately replaces the raw {@code hashedPassword} the old
 * entity coupling exposed: the only cross-feature question ever asked of the hash outside
 * a credential check is "does this account HAVE one?" (OAuth unlink guard). Credential
 * verification itself lives behind {@link UserAccountService#authenticate} so the hash
 * never crosses the feature seam.
 */
@PublishedApi
public record UserAccountDto(
    UUID id,
    String email,
    UserRole role,
    boolean emailVerified,
    boolean hasPassword
) {
    static UserAccountDto of(UserEntity u) {
        return new UserAccountDto(u.getId(), u.getEmail(), u.getRole(),
            u.isEmailVerified(), u.getHashedPassword() != null);
    }
}
