---
pattern: fixture-empty-recipe-list
display_name: "Fixture: empty applied_recipes: list (must FAIL)"
schema_version: 1
enabled_l4_domains:
  - crud
applied_recipes:
---

# Fixture: fail_applied_recipes_empty_list

This fixture verifies that `applied_recipes:` (plural) with NO list entries
fails `recipe_governance_guard.sh`'s `check_applied_recipe_declared` function.

Expected guard result: **FAIL** (exit non-zero, VIOLATION message emitted).
