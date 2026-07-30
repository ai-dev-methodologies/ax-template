# PRD — final-4 backlog convergence wave (P3-93 · P3-95 · P3-101 · P3-102) — rev 3, post-critic

Scope: the four remaining open rows of `docs/BACKLOG.md` (res18 rows :438-:442 authoritative)
PLUS one in-wave enforcement closure the architect promoted from "register a row" to "land now"
(W1b manifest integrity — B4). Executes AFTER residual-18 lands on main; branch from that
merged main; `../ax-template-res18` stays frozen. Standing pattern: dedicated worktree +
freeze + ONE central R25 on the frozen tree + push HEAD:main + codex cross-family final gate.

---

## 0. RALPLAN-DR

**Principles**
1. **A snapshot records what the source ACTUALLY says — mechanically.** Fetch = `curl`
   (manifest precedent `via: "curl"`, stripe/toss) piped through a COMMITTED deterministic
   extractor (strip script/style → strip tags → HTML-unescape → whitespace collapse — the
   guard's own normalizer pipeline, `evidence_quote_spotcheck_guard.sh` `strip_html`+`normalize`).
   WebFetch/WebSearch are TRIAGE ONLY (is the URL alive, where does content live) — a
   WebFetch result is a model's rendering, not source bytes, and a snapshot authored from it
   is paraphrase LABELLED verified, which is worse than today's honest "unverifiable" (B3).
   If a cited quote is absent from the extracted content, the CITATION is re-anchored in the
   citing template; the snapshot is never doctored. Dead/unfetchable URLs (recharts — B2) are
   recorded per-URL and their quotes stay in a numeric-floor residual.
2. **Ratchets only tighten, all FIVE halves at once, on IDENTITIES not findings.** The ledger
   unit is `(path, upstream_id)` — 53 findings = **52 unique identities** (time-series-chart.tsx
   carries two recharts anchors = one identity; a duplicate row is DUPLICATE_IDENTITY exit 2 —
   B1). Final ratchet moves ledger rows + `# require:` lines + `# min_entries:` +
   `LIVE_MIN_PROTECTED_ENTRIES` + `LIVE_REQUIRED_PROTECTED_IDENTITIES` together; guard:257's
   one-directional `<=` assert is raised to **equality on live_root** so the upward half-move
   (raise floor, add only some tuples) becomes mechanically impossible (A5).
3. **Pin only what a gate holds immutable.** Pinning 46 identities to snapshot bodies that
   nothing checksums would be a paper ratchet: TODAY 71 of 91 manifest sha/bytes diverge from
   disk (stripe 1657→2089 undetected; recharts/next-intl/kakao share ONE sha across three
   byte-counts = provably fabricated) and no guard looks (B4). `manifest_snapshot_integrity_guard`
   lands IN-WAVE (W1b) before the ratchet is declared done.
4. **Seals are history, not config.** v1 verdict files stay byte-identical; staleness is
   closed by a NEW versioned verdict from actually re-running the procedure — and honestly
   labelled: v1 was a self-recorded SIMULATION (its own §"context-0 simulation" heading), so
   a lower v2 score is a fidelity upgrade, not a regression (A11).
5. **Verify with the PROTECTED-mode guard invocation, iteratively; census-first; correct the
   brief where disk disagrees.** Advisory mode passes `section=None`; protected mode checks
   `section` FATALLY against the snapshot — so acceptance loops run the protected invocation,
   and snapshot section headings are authored to carry the 41 cited section names (36
   lowercase shadcn slugs + 5 editorial names; `normalize` is case-sensitive). Headings MAY
   be authored to carry cited section names; PROSE may not — a quote that matches only inside
   a heading line is a finding, not a pass (A6/A7).

**Decision Drivers (top 3)**
1. P3-93 is newly feasible (network was the sole open-by-design blocker; offline headroom
   measured 0) — and the architect's probes show the shadcn surface closes BY CONSTRUCTION:
   pages are HTTP 200 and 3/3 sampled cited quotes are VERBATIM LIVE through the extractor
   pipeline, so ~36-37 identities need zero authoring judgment.
2. The protected-anchor ratchet is the existing mutation lock — but it is only as strong as
   snapshot immutability, hence W1b in-wave (Driver for B4).
3. No item touches backend compilation — no lane gradle; one central R25.

**Premise corrections vs the original brief (disk/network-verified)**
- wcag-2-2 digest rebuild NOT needed: one template citation (checkbox.tsx), already clean and
  protected; rules sweep 0/185 at FULL --strict.
- recharts-2026-05 is registered in the JAVA manifest (`practices/upstream/_MANIFEST.yaml`,
  ~:347) with no body — and `recharts.org/en-US/api` is **404 / a 633-byte client-rendered
  SPA shell** (5 URLs probed): the whole SNAPSHOT_FILE_MISSING class (7 findings / 6
  identities) is NOT closable by static fetch. **F=7 is the EXPECTED case, not a fallback**:
  honest target = 18 + 46 = **64 pinned identities**, recharts re-registered as a residual row
  carrying the dead-URL probe record (B2, A13c).
- Disk byte truth (manifest is wrong): stripe-billing snapshot = **2089 bytes in BOTH catalogs,
  byte-identical** (manifest claims 1657); shadcn-ui = **2227** (manifest claims 2800) (A14).
- Option A (ratchet-only floor re-registration) is legitimately blessed by the row's own
  done-when ("or ledger 추가 래칫으로 플로어 상승") — not forbidden, just weaker. B is chosen
  because the P3-69 precedent shows fetch-verification catches real citation defects (2 found
  then); A remains the documented fallback if the network posture collapses mid-wave (A17).

**Options**
- **A) Ledger-only ratchet, no fetch.** Legal per the row; rejected as weaker while the
  network blocker is gone (see above — no longer claimed forbidden).
- **B) curl+extractor refresh + requote + ratchet + in-wave integrity guard, 3 lanes (CHOSEN).**

---

## 1. Work items

### W1 — P3-93 full-refresh, requote, ratchet (~85% of the wave)

**Census (first, own worktree):** advisory sweep enumerates findings; identity census derives
unique `(path, upstream_id)` pairs. Expected: 53 findings / **52 identities** = shadcn-ui 37 ·
recharts 6 identities (7 findings) · stripe 3 · shadcn-registry 2 · react-dropzone /
next-themes / kakao-postcode / input-otp 1 each. Census delta vs these numbers is reported,
not papered over. Pre-verified: all 52 quotes pass `MIN_PROTECTED_QUOTE_CHARS=24`.

**Extractor (committed tool, Lane A single-writer):** `practices/scripts/snapshot-extract.sh`
— curl → strip script/style → strip tags → unescape → collapse, deterministic, no model in the
loop. Snapshot bodies = extractor output, sectioned so cited `section` names resolve as
HEADINGS (never injected into prose — A7). Snapshot header (prose shape, next-themes
precedent): source URL(s) + HTTP status + fetched_at + exact extractor invocation + sha256 of
the extracted BODY (not of the file — no self-referential sha) (B3).

**Mechanical provenance (C1 — Critic):** three additional deliverables bind snapshots to the
`curl → extractor` transaction instead of trusting the header prose:
- **Per-URL receipts ledger** `practices/upstream/_FETCH-RECEIPTS.yaml` (committed,
  append-only, **single-writer Lane A** — the lane that fetches): EVERY attempted URL gets a
  row — url, curl exit code, HTTP status, byte count, sha256 of THAT URL's extracted output,
  fetched_at, error text on failure. Dead URLs (recharts) appear here as their probe record;
  nothing is fetched off-ledger. A multi-URL snapshot additionally gets ONE **assembly
  receipt** row (`kind: assembly`): sha256 of the assembled BODY + the list of per-URL receipt
  ids it was built from.
- **Extractor replay fixtures**: committed fixture HTML + expected extracted output;
  `snapshot-extract.sh --self-test` replays them (deterministic — proves the extractor, not
  the operator). Registered as fixtures of the W1b guard.
- **Guarded refresh transaction — three checks in DISTINCT digest domains (no cross-domain
  equality):** per touched id the W1b guard verifies the chain
  (a) manifest `sha`/`bytes` == the whole snapshot FILE on disk (file domain);
  (b) the header's recorded body-sha == sha256 of the file's body recomputed with the header
  stripped (body domain);
  (c) that body-sha == the id's assembly receipt (or, single-URL case, its per-URL receipt),
  and every per-URL receipt id the assembly references exists in the ledger (receipt domain).
  A snapshot+manifest edited together without the matching receipt row is exit 2
  (RECEIPT_MISSING). Refresh is therefore only legal through a new fetch appending receipts.
- **Heading-vs-prose is verified mechanically, not by principle**: Lane B adds a
  prose-presence pass to the protected loop acceptance — for each pinned identity, the quote
  must match on at least one NON-heading line of the normalized body (heading lines = the
  section-marker lines the extractor emits). A heading-only match is exit 1
  (QUOTE_ONLY_IN_HEADING), covered by a fixture.

**Fetch list (canonical URLs from the evidence blocks):** shadcn-ui root + ~37 per-component
pages (verified 200, quotes verbatim-live) · shadcn-registry per census · stripe: the 3 cited
docs.stripe.com sub-pages — **append-only expansion applied IDENTICALLY to BOTH catalogs'
byte-identical 2089-byte snapshots** (rules resolve per-catalog; 4 rule citations + 2
protected anchors must stay green — A8) · shadcn-ui expansion also append-only (utils.ts is
pinned — A9) · react-dropzone / next-themes / kakao-postcode / input-otp single pages.
recharts: probe once from this session for the record; expected dead → residual row with the
probe transcript (URL, status, byte-count, date).

**Requote loop:** iterate with the PROTECTED-mode invocation semantics (section checked
fatally), growing the ledger INCREMENTALLY with `# min_entries:` held at 18 (rows > floor is
legal; shrink is not) — the five-halves ratchet to 64 happens ONCE at the end (A6). Citation
fixes edit only the citing template's evidence block.

**Final ratchet:** 18 → **64** (= 18 + 46) across all five halves + the guard:257 assert
raised to a **five-surface equality census executed inside the guard on every live run** (C3):
`rows == distinct-require-identities == min_entries == len(frozenset) == LIVE_MIN_PROTECTED_ENTRIES
== 64` — not `rows >= min` plus inclusion; exit 0 without the census passing is impossible by
construction, and the acceptance matrix runs the census, not a count grep. RED demo subject is
**NAMED**: the census row `templates/**/accordion.tsx × shadcn-ui-2026-05`; if the census
shows no such row, the lexicographically-first newly-pinned shadcn identity becomes the
subject and Lane B NAMES it in its report BEFORE mutating (deterministic, no discretion).
Fabricate that quote → registered live invocation (`run-all-guards.sh:1236`) exit 1 →
restore. Fixture roots are live_root-gated; confirm registered fixture invocations still pass.

### W1b — manifest_snapshot_integrity_guard (B4, in-wave; Lane B)

New guard: for EVERY manifest id whose `.snapshot.md` body exists (either catalog), manifest
`sha` + `bytes` MUST equal the whole snapshot FILE on disk (`shasum -a 256` / `wc -c` — file
domain), and for every W1-touched id the full three-domain chain of the C1 refresh transaction
holds (file ← header/body ← receipts; distinct digest domains, checked separately — never a
cross-domain equality).

**Allowlist arithmetic pinned (C2):** the wave-start census — **71 divergent entries — is the
immutable baseline universe**, recorded verbatim in the allowlist header. Eight of the 71 are
W1-touched ids; W1 synchronizes them (true sha/bytes + receipt), so the **expected post-W1
residual allowlist is 63** — subject to recensus at integration, delta reported not papered.
Allowlist mechanics enforced by the guard, not by convention:
- entries keyed by unique `(catalog, id)` — duplicate = exit 2;
- **subset-only**: every entry must be in the frozen 71-baseline (additions = exit 2), and
  removal is the only legal edit (shrink-only);
- **non-redundancy**: an entry whose manifest sha/bytes NOW match disk is stale and fails the
  guard (forces burn-down instead of permitting a padded list);
- every entry carries a per-entry `reason:` (honest default: originally-fetched body lost —
  recorded sha/bytes are unverifiable-or-fabricated history).
Deliverables: guard + allowlist + fixtures (pass_clean / fail_diverged / fail_receipt_missing /
fail_stale_allowlist) + `fixture_kill_manifest.yaml` entry ([87] kill-proof) + run-all-guards
registration. RED demo: edit one byte of a fetched body → guard red → restore.

### W2 — P3-95 soft-delete rule jurisdiction (small)

As rev 1 (Jurisdiction section in both rule docs; ORM row-removal interception layer vs
service-level lifecycle/audit layer; the two rules COMPOSE on file-storage) — PLUS (A12):
reference the Jurisdiction section from the **impact-line region** of both rules, so
title-level readers see the reconciliation (nothing mechanically reads `protects_template_ids`
— zero readers verified — so prose placement is the only working surface). Evidence blocks
byte-unchanged; meta-block NOTEs (`FileStorageService.java:10`, `FileStatus.java` rationale if
worded against the statement) repointed at the new sections.

### W3 — P3-102 LockingPolicy holder-verified release (small)

Interface `release(UUID taskId, String lockHolder)` + **BOTH implementations in the file**:
`DbRowLockingPolicy.release` (:135 — findByIdForUpdate, clear only on holder match, mismatch =
no-op + log) AND `MockLockingPolicy.release` (:184 — forks inherit a compile error otherwise;
3 `release(UUID)` signatures exist at :81/:135/:184 — A10). Update the interface Contract
bullet (javadoc :55-62) + call site `ScheduledTaskService.java:212` →
`release(task.getId(), holder)`. **README unchanged** (SCHED-LOCK-001 row names tryAcquire
only). Meta block records the remaining BY-DESIGN gap: row-UUID key vs the production SPI's
taskName-keyed advisory-lock table — holder-verification divergence closed, key-shape
divergence documented. Meta YAML stays parseable (evidence meta-walk reads skeletons).

**Behavioral proof, not signature grep (C3):** Lane C AUTHORS (does not run — no lane gradle)
JUnit cases in the existing scheduled-task suite: (a) holder-mismatch `release` = no-op — lock
columns unchanged, warning logged, no throw; (b) matching-holder `release` clears both columns;
(c) release path goes through `findByIdForUpdate` (same PESSIMISTIC_WRITE repo method — assert
via the repository call, not prose). These EXECUTE in the wave's central R25 backend step
(`testScheduledTask`), which is the acceptance — grep remains only a fast pre-check.

### W4 — P3-101 sealed-verdict v2 re-run (small)

`skills/_tests/sealed-verdict/scheduler-l4-verdict-v2.md`: verdict_version "2", recorded_at
2026-07-30, rubric corrections = **exactly two items** — M6 (SKIP LOCKED → `SELECT … FOR
UPDATE` + TTL stale-reclaim OR ShedLock, per P2-48) AND M9 (v1 asserts the README has no
`applied_recipes:` key; README:107 has carried it since R8 — also stale, A11). Other 18
rubric items byte-identical to v1; the 2-item rubric diff is recorded in the v2 header. v2
states explicitly that **v1 was a self-recorded simulation** (v1 :51 "context-0 simulation"),
so a lower genuinely-context-0 v2 score reads as fidelity, not regression. Run the embedded
SP42 prompt with a real context-0 sub-agent (2 files only; explicit model); scores recorded
as scored — threshold failure = real README self-describability finding. v1 byte-identical.
**Execution receipt (C3):** the sub-agent's RAW transcript is committed as
`skills/_tests/sealed-verdict/scheduler-l4-verdict-v2-transcript.md` (agent output verbatim +
invocation header: model, files provided, date) and the v2 header references it by path — a
v2 without a committed transcript is the exact defect v1 disclosed, and integration REJECTS it
(file-existence + reference grep in the acceptance row). Fallback (context-0 run infeasible
in-wave only): dated row-side annotation, row stays open — no transcript, no v2.

### W5 — new BACKLOG rows to register (main loop; A13)

(a) **P2**: manifest sha/bytes integrity — measured 20/91 verified; partially closed by W1b's
guard+allowlist; the row tracks the **63-entry post-W1 residual** allowlist burn-down (71 is
the frozen baseline universe, C2). (b) **P2**: catalog-wide
time_decay expiry — practices-react oldest fetched_at 2026-05-13 → guard RED **2026-08-11
(~12 days)**, practices → 2026-08-16; unregistered-and-blocking when it fires; the row must
state: fetched_at may NOT be bulk-touched without re-fetching (that is doctoring one level
up). (c) **P3**: recharts residual — 7 findings / 6 identities, dead-URL probe record, floor 7.

### W5b — count-ratchet integration (Lane B; C4)

Adding `manifest_snapshot_integrity_guard.sh` raises guard disk truth **102 → 103**. Lane B
owns, in the SAME commit as the guard registration: the three enforced headline counts —
`README.md:45`, `CLAUDE.md:171`, `skills/ax-transform/SKILL.md:86` — updated to 103 (else
`doc_headline_count_guard` predictably blocks R25), AND the `[87]` double floor
(`fixture_kill_manifest.yaml` `min_items` + guard `LIVE_MIN_ITEMS`) raised **62 → 63** as the
new kill-proof is appended — otherwise the new proof stays removable without violating the
registry floor, which is exactly the shrink [87] exists to reject.

---

## 2. Binary acceptance matrix

| Item | Command | Expect |
|---|---|---|
| W1 findings | `evidence_quote_spotcheck_guard.sh --include-templates \| grep -c "TEMPLATE_"` | **7** (recharts only), all named in row (c) |
| W1 protected | `evidence_quote_spotcheck_guard.sh --strict --strict-templates --templates-only-protected` | exit 0, **64 unique identities** |
| W1 ratchet coherence | **five-surface equality census inside the guard, every live run** (C3): rows == distinct require == min_entries == frozenset == LIVE_MIN | all five == **64**; census failure = exit ≠ 0 |
| W1 heading-vs-prose | prose-presence pass (C1): each pinned quote matches a NON-heading normalized line | exit 0; heading-only fixture → QUOTE_ONLY_IN_HEADING red |
| W1 receipts | every fetched/probed URL has a `_FETCH-RECEIPTS.yaml` row; extractor `--self-test` replay | complete; self-test green |
| W1 RED demo | fabricate the NAMED subject's quote (`accordion.tsx × shadcn-ui-2026-05`, or named-first-lexicographic fallback) → live invocation | exit 1, then restored green |
| W1 time_decay | `time_decay_guard.sh` + `--catalog practices-react` | exit 0, REAL fetch timestamps only |
| W1b guard | `manifest_snapshot_integrity_guard.sh` live + 4 fixtures + [87] entry | live 0; fail fixtures red; one-byte body edit → red; receipt-less refresh → RECEIPT_MISSING |
| W1b allowlist | baseline-71 frozen header; guard mechanics: unique (catalog,id) / subset-only / non-redundant / per-entry reason | post-W1 residual **63** (recensus delta reported) |
| W1b touched ids | three-domain chain per touched id: manifest==FILE sha/bytes; header body-sha==stripped-body recompute; body-sha==assembly/per-URL receipt with all referenced rows present | exact, OUTSIDE allowlist |
| W2 | `grep -l "Jurisdiction"` both rules → 2; impact-line region references it; `evidence_guard.sh` + pre-commit 4 gates | green; evidence blocks byte-unchanged |
| W3 | JUnit behavioral cases (holder-mismatch no-op / match clears / ForUpdate path) EXECUTED by central R25 `testScheduledTask`; grep ×3 sites as pre-check | R25 green; all greps hit; README untouched |
| W4 | `git diff` empty on v1; v2 exists: verdict_version "2", 2-item rubric diff, simulation note, recorded scores; **committed transcript referenced from v2 header** | pass; v2 without transcript = REJECT |
| W5 | BACKLOG integrity guard (sibling IDs parenthesized) | 3 new rows, guard green |
| W5b | headline counts ×3 (README:45 / CLAUDE.md:171 / SKILL.md:86) == 103; [87] double floor == 63 | `doc_headline_count_guard` + [87] green |
| Wave | central R25 on frozen tree (JAVA_HOME per memory) | FULL PASS, `full_run` audit line |

**Mutation-lock obligations:** protected-anchor ratchet (now equality-asserted) = the quote
lock; W1b guard + [87] kill-proof = the snapshot-body lock (new); W2/W3/W4 are prose/skeleton
surfaces — grep + guard acceptance is the lock, no vacuous test theater.

## 3. Lanes & execution constraints

Base: post-residual-18 main. Worktree `ax-template-final4`, `/freeze`. No lane gradle.
Single-writer: extractor script + snapshots + manifests + **`_FETCH-RECEIPTS.yaml`** (Lane A —
the fetching lane writes its own receipts; Lane B only READS the ledger from its guard);
protected-anchor ledger + spotcheck-guard constants + new integrity guard + run-all-guards.sh
+ kill manifest + **DECISIONS.md** (Lane B — sole writer; main loop hands its entry TEXTS to
Lane B, never writes the file). Serialization: A → B (handoff = clean-identity list +
touched-id table (catalog, id, file sha/bytes, body-sha, receipt ids) + the committed receipts
ledger itself — everything the guard cross-checks arrives as committed artifacts, not prose).
Ledger events logged per standing directive. Push: R25 FULL PASS →
`git push origin HEAD:main` → codex final gate on pushed head (`gpt-5.6-sol`, xhigh,
`< /dev/null`).

| Lane | Items | Work | Model |
|---|---|---|---|
| A fetch+snapshot | W1 census · extractor authoring (+ `--self-test` fixtures) · curl fetches (~45 pages) **each appending its `_FETCH-RECEIPTS.yaml` row (+ assembly rows)** · snapshot bodies · manifest re-sync (real sha/bytes/fetched_at) · requote via protected-mode semantics · recharts probe record (= receipts rows) | network lane | sonnet exec + opus adversarial honesty review (no-doctoring, citation-vs-snapshot classification, heading-vs-prose check) before handoff |
| B ledger+guards | W1 final five-halves ratchet to 64 + five-surface equality census (C3) + prose-presence pass (C1) + NAMED RED demo · W1b integrity guard + receipts cross-check + baseline-71/residual-63 allowlist mechanics (C2) + 4 fixtures + [87] + registration · W5b headline ×3 → 103 + [87] floor → 63 (C4) · DECISIONS entries | single-writer of enforcement surfaces | opus (release-gate surface) |
| C small-3 | W2 jurisdiction (+A12 impact-line ref) · W3 signature ×3 sites + call site + meta gap note + JUnit behavioral cases (C3; executed by central R25) · W4 v2 re-run (spawns context-0 sub-agent, commits raw transcript — C3) | independent of A/B | sonnet exec + opus review of rule text; verdict context-0 agent sonnet, scored by opus |
| main loop | W5 three new rows · BACKLOG table · freeze · central R25 · push · codex | coordination | — |
