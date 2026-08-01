---
title: "Selftest Malformed Frontmatter Rule (no closing delimiter)"
impact: "LOW"
verification:
  type: review

Fixture-only rule for practices/generate_index_selftest.sh. The opening
delimiter line above is never followed by a closing one anywhere in this
file, by design, to prove the generator's exit-1 policy on structurally
unparseable frontmatter (parse_frontmatter returns "no closing delimiter").
