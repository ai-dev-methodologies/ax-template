---
pattern: fake-recipe
display_name: "Fake Recipe (fixture: missing L4 README)"
schema_version: 1
compatible_with_catalog_version: "v1.2.0-p1-absorbed"
last_verified_at: "2026-05-18"
enabled_l4_domains:
  - fake-domain
---

# Fake Recipe — fixture

This RECIPE.md is a fixture used by `recipe_governance_guard.sh --fixtures`
to validate that a recipe enabling a domain with no `templates/L4/<domain>/README.md`
is detected as a FAIL (not silently SKIPped).

`fake-domain` is intentionally non-existent so the guard must emit:
  FAIL [fake-recipe/fake-domain: enabled by recipe 'fake-recipe' but README missing ...]
