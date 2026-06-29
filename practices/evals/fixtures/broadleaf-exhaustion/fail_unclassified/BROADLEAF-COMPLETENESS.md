# Broadleaf module-set exhaustion ledger (FIXTURE: invalid)

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
| common | SKIP | shared plumbing |

## Core commerce-package set

| module | classification | evidence |
|---|---|---|
| catalog | ABSORBED | commercecatalog + catalog-commerce-l0 |
| config | SKIP | Spring wiring |
| mysterymodule | TODO |  |

## Common sub-package set

| common_package | classification | evidence |
|---|---|---|
| money | ABSORBED | payment-l0 MONEY family |

## Profile-core sub-package set

| profile_package | classification | evidence |
|---|---|---|
| domain | ABSORBED | default-member-singleton-l0 |
<!-- FIXTURE (fail): the 3rd core row has an INVALID classification (TODO, not in
     {ABSORBED,RE-FIND,SKIP,RESIDUE}) AND an empty evidence column. The
     exhaustion guard MUST BLOCK this. (Counts match so the failure isolates
     to the classification/evidence checks.) -->
