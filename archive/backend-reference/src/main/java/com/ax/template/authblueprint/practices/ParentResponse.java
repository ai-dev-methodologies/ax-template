package com.ax.template.authblueprint.practices;

/**
 * Fixture for PRACTICES-API-002: response DTO that exposes only client-facing fields,
 * not the underlying JPA entity. Returning `Parent` directly would surface the
 * @OneToMany children collection and any lazy associations the API never agreed to
 * include in its contract.
 */
public record ParentResponse(Long id, String name, int childCount) {

    public static ParentResponse from(Parent p) {
        return new ParentResponse(p.getId(), p.getName(), p.getChildren().size());
    }
}
