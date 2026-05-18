---
pattern: fixture-multi-recipe
display_name: "Fixture: multi-recipe membership using R6+ plural form"
schema_version: 1
enabled_l4_domains:
  - crud
  - audit-log
applied_recipes:
  - booking
  - marketplace
---

# Fixture: pass_applied_recipes_plural

This fixture verifies that `applied_recipes:` (plural, R6+ canonical form) with ≥2 entries
passes `recipe_governance_guard.sh`'s `check_applied_recipe_declared` function.

Expected guard result: **PASS** (exit 0).
