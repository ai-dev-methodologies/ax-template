# Broadleaf module-set exhaustion ledger (FIXTURE: valid four-table, no residue)

maven_module_count: 2
module_count: 3
common_subpackage_count: 1
profile_subpackage_count: 1
residue_count: 0
absorbed_vertical_count: 0

## Maven module-set

| maven_module | classification | evidence |
|---|---|---|
| core/broadleaf-framework | ABSORBED | core commerce package = the core table below |
| common | SKIP | shared plumbing — no portable correctness invariant |

## Core commerce-package set

| module | classification | evidence |
|---|---|---|
| catalog | ABSORBED | commercecatalog + catalog-commerce-l0 |
| config | SKIP | Spring wiring — not a correctness invariant |
| media | RE-FIND | blob lifecycle = file-storage-l0 |

## Common sub-package set

| common_package | classification | evidence |
|---|---|---|
| money | ABSORBED | payment-l0 MONEY family |

## Profile-core sub-package set

| profile_package | classification | evidence |
|---|---|---|
| domain | ABSORBED | default-member-singleton-l0 |
