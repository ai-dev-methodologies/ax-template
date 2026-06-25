# Broadleaf module-set exhaustion ledger (FIXTURE: valid, no residue)

module_count: 3
residue_count: 0

| module | classification | evidence |
|---|---|---|
| catalog | ABSORBED | commercecatalog + catalog-commerce-l0 |
| config | SKIP | Spring wiring — not a correctness invariant |
| media | RE-FIND | blob lifecycle = file-storage-l0 |
