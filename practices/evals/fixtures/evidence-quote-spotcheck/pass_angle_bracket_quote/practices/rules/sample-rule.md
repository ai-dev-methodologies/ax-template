---
title: sample
evidence:
  - upstream_id: sample-src
    section: "p1"
    quote: "Typically, migrations are scripts in the form V<VERSION>__<NAME>.sql (with <VERSION> an underscore-separated version, such as '1' or '2_1')."
---
BACKLOG P2-74 regression fixture — the citation is VERBATIM page text and contains
literal unescaped angle brackets. Before P2-74 the snapshot side was stripped of
`<...>` and the quote side was not, so this shape could never match: the guard
reported QUOTE_NOT_IN_SNAPSHOT for a citation that is byte-correct. Now both sides
run through the same normalizer, so it resolves.
