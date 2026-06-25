# Broadleaf module-set exhaustion ledger (FIXTURE: invalid)

module_count: 3
residue_count: 0

| module | classification | evidence |
|---|---|---|
| catalog | ABSORBED | commercecatalog + catalog-commerce-l0 |
| config | SKIP | Spring wiring |
| mysterymodule | TODO |  |
<!-- FIXTURE (fail): the 3rd row has an INVALID classification (TODO, not in
     {ABSORBED,RE-FIND,SKIP,RESIDUE}) AND an empty evidence column. The
     exhaustion guard MUST BLOCK this. -->
