---
title: sample
evidence:
  - upstream_id: sample-src
    section: "p1"
    quote: "Typically, migrations are scripts in the form V<VERSION>__<NAME>.zzz (with <VERSION> an underscore-separated version, such as '1' or '2_1')."
---
BACKLOG P2-74 NON-VACUITY control. Byte-identical to pass_angle_bracket_quote except
`.sql` -> `.zzz` in the quote. The point is that the symmetric strip introduced by P2-74
is not a way to make angle-bracket citations pass by construction: what the strip deletes
it deletes from BOTH texts, so the surviving prose still has to agree. This fixture must
stay RED — if it ever exits 0, the normalizer has started eating more than the tags.
