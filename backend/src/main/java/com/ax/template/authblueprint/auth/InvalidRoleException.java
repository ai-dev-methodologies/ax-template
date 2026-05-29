package com.ax.template.authblueprint.auth;

/**
 * Signup requested a role string that does not map to any {@code UserRole} constant.
 *
 * <p>IMW2-C (IDW2 dogfood 2026-05-29): {@code AuthServiceImpl.resolveRole} previously
 * caught the {@link IllegalArgumentException} from {@code UserRole.valueOf(...)} and
 * silently downgraded an UNKNOWN requested role to {@code MEMBER}. A fork-receiver who
 * asks for a role not present in the enum (a typo, a stale role name, or a role the
 * template does not define) then receives a privilege downgrade that surfaces only as
 * confusing later {@code 403}s — never as an explicit failure at signup time. All three
 * IDW2 personas hit this. This exception makes the deviation a LOUD 400 instead, mapped
 * to {@code application/problem+json} by {@code AuthExceptionHandler}.
 */
public class InvalidRoleException extends RuntimeException {
    public InvalidRoleException(String message) {
        super(message);
    }
}
