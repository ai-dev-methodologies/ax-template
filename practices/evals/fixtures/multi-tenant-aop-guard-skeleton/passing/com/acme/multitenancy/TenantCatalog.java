package com.acme.multitenancy;

import java.util.List;
import java.util.UUID;

/**
 * Generated from blueprints/multi-tenant-manifest.yaml#scheduled-task-tenant-scope.tenant_catalog_contract
 * with <root> = acme.
 *
 * Single source of tenant enumeration for scheduled tasks. MUST be a
 * separate non-TenantOwned bean (it is the tenant discriminator, not
 * a tenant-scoped resource — chicken-and-egg avoidance: listing tenants
 * cannot itself require a tenant context).
 *
 * Implementations read the Tenant entity table directly without going
 * through @Filter activation (see #context-resolution); this is the
 * only entity type that legitimately bypasses tenant scoping.
 *
 * listActive() MUST return only currently-active tenants; deactivated
 * tenants MUST NOT receive scheduled-task side effects.
 */
public interface TenantCatalog {

    List<UUID> listActive();
}
