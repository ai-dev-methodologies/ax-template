# final-4 W1 — Lane A handoff (fetch + snapshot)

Date: 2026-07-30. Worktree: `ax-template-final4` (branch `final4`). Lane A owns
`practices/scripts/snapshot-extract.sh`, all `*.snapshot.md` files, both catalogs'
`_MANIFEST.yaml`, and `practices/upstream/_FETCH-RECEIPTS.yaml`. This document is the
serialization handoff to Lane B (§3 of PRD-final-4: "handoff = clean-identity list +
touched-id table + the committed receipts ledger itself"). Lane A does not touch the
protected-anchor ledger, `run-all-guards.sh`, `fixture_kill_manifest.yaml`, `BACKLOG.md`,
or `DECISIONS.md` — none of those were edited.

## 1. Census (W1, first step)

`bash practices/evals/evidence_quote_spotcheck_guard.sh --include-templates` at wave start:

| | Expected (PRD) | Actual |
|---|---|---|
| Findings | 53 | **53** |
| Unique identities | 52 | **52** |
| shadcn-ui-2026-05 | 37 | **37** |
| recharts-2026-05 | 6 identities / 7 findings | **6 identities / 7 findings** |
| stripe-billing-2026-05 | 3 | **3** |
| shadcn-registry-2026-05 | 2 | **2** |
| react-dropzone-2026-05 | 1 | **1** |
| next-themes-2026-05 | 1 | **1** |
| kakao-postcode-2026-05 | 1 | **1** |
| input-otp-2026-05 | 1 | **1** |

No delta — the PRD's pre-verified numbers held exactly on first sweep.

## 2. Extractor

`practices/scripts/snapshot-extract.sh` (new, committed) + `practices/scripts/lib/snapshot_extract.py`
(the actual extraction function, in its own file — a `python3 - <<PY` heredoc form was tried
first and rejected: feeding the script body via stdin consumes the same fd the script needs to
read the HTML from, so `sys.stdin.read()` returns empty inside the heredoc; a real file avoids
the conflict). Pipeline: strip `<script>`/`<style>` blocks → strip tags → HTML-unescape →
whitespace collapse — mirrors `evidence_quote_spotcheck_guard.sh`'s own `strip_html()` +
the non-relaxation half of `normalize()` exactly (deliberately excludes the smart-quote/
backtick/blockquote relaxations, which are quote-vs-snapshot comparison rules applied at
verify time, not extraction).

`--self-test` replays two committed fixtures under `practices/scripts/fixtures/snapshot-extract/`
(`basic.html`/`basic.expected.txt` — script/style stripping, entity unescape, whitespace collapse;
`nested-tags.html`/`nested-tags.expected.txt` — nested tags, fake tag-like strings inside
script/style that must NOT leak, `&lt;`/`&gt;`/`&mdash;`/`&copy;` unescaping). Both pass:

```
$ bash practices/scripts/snapshot-extract.sh --self-test
snapshot-extract: self-test PASS (2 fixture(s))
```

Metadata bug found and fixed during this session: the first draft printed `bytes=` as the RAW
HTML download size while `sha256=` hashed the EXTRACTED output — two different artifacts under
one label. Fixed to print `raw_bytes=` (diagnostic) and `bytes=` (extracted, paired with `sha256`).

## 3. Fetch summary

55 URLs fetched this session (all via the extractor, all recorded), 8 assembly rows — 63 total
receipt rows in `practices/upstream/_FETCH-RECEIPTS.yaml`. Every URL is `curl_exit: 0` (no
transport failures); "dead" content is represented as HTTP 404 / thin-SPA-shell 200, not as a
fetch error.

## 4. Snapshots touched (8 identities)

Header format adopted for every touched file (next-themes precedent, generalized): title line,
then bold-label lines (`**Source URL(s):**`, `**HTTP status:**`, `**Fetched at:**`,
`**Extractor invocation:**`, `**Body SHA-256 (below the `---` divider, header excluded):**`),
then a blank line, `---`, blank line, then body. **The header/body boundary is the first
literal `---\n\n` in the file** — this is the exact split Lane B's `manifest_snapshot_integrity_guard.sh`
must use to isolate the body before recomputing its sha256; recomputed and verified against every
file below before this handoff was written. All 8 are **append-only**: every byte of pre-existing
body content is preserved verbatim; only new sections were added.

| Catalog | id | file bytes | file sha256 | body sha256 | receipt ids (fetch) | assembly id |
|---|---|---|---|---|---|---|
| practices-react | shadcn-ui-2026-05 | 8030 | `0e2450f4abd75de32650270ad61e32e81238544ce6168175710cf7a0217a88a3` | `5b5dfa2f1d4703c62bc43a3683502fb24de78b8314ed995e9363001670f7fbbe` | r001–r037 (37) | asm-shadcn-ui-2026-05 |
| practices-react | shadcn-registry-2026-05 | 4527 | `5b6a9c4d8b25e00e2a2550ae1ad923019f54b19a36c14a49af73372903076f99` | `491d495de216242c3e33b8fce238c72fe08544bd3252380a367485f7a823dc95` | r033 (reused sonner fetch) | asm-shadcn-registry-2026-05 |
| practices-react | react-dropzone-2026-05 | 3514 | `10ac7d79c1ccbf08f873a1b58253d8061ad554104c00f89098071c7b1f7ea553` | `7a87a1520c1bfc16c18c6c85cc2c286099c094c10ce9dedd292d847f2c7c5d7b` | r038, r039 | asm-react-dropzone-2026-05 |
| practices-react | next-themes-2026-05 | 4311 | `3c8da38db8a38ac3de9e1fe7a77f8a52abe167a5a06dd558e9dbd38485e76ed2` | `757221cb8a79ea419de1fbf3127d2c0ded9263ed08a92857ddc26c4a86298e86` | r040, r041, r042 | asm-next-themes-2026-05 |
| practices-react | kakao-postcode-2026-05 | 2989 | `b555265ae6145b1fafcaa21a0a87f1cb79b4af488e86c5d92b25f380161f73b8` | `abb7d582ae297e778a931f4f2b5ad272c8a02b61d15d5db3f56a0951d5dfe767` | r043 | asm-kakao-postcode-2026-05 |
| practices-react | input-otp-2026-05 | 2673 | `367154fa525a23479eddb49555ba47fe55510dc5bf5e3cf8d291bf2f4c82ee49` | `aa7d7296927bec2170d68344fe2cd87c9777b720856f30d9c98563e8ae68d6a4` | r044 | asm-input-otp-2026-05 |
| practices-react | stripe-billing-2026-05 | 3394 | `94287a976a406c8cfcae337374ce1a370e5ecf7aecc45e3c6ac24cd9140a6fee` | `d42d9ebc4fc383c6dff3578cfa0057efb42b7a7a87f163c36a71bdc99509f20e` | r045, r046, r047, r048 | asm-stripe-billing-2026-05-practices-react |
| practices | stripe-billing-2026-05 | 3394 | `94287a976a406c8cfcae337374ce1a370e5ecf7aecc45e3c6ac24cd9140a6fee` | `d42d9ebc4fc383c6dff3578cfa0057efb42b7a7a87f163c36a71bdc99509f20e` | r045, r046, r047, r048 (shared, same URLs) | asm-stripe-billing-2026-05-practices |

stripe-billing-2026-05 is byte-identical across both catalogs (same file sha, same body sha) —
verified programmatically before writing (`assert stripe_react_body_sha == stripe_java_body_sha`).

**Manifest re-sync**: both `_MANIFEST.yaml` files updated for these 8 identities to the TRUE
whole-file `sha`/`bytes` (via `shasum -a 256` / `wc -c`, independently re-verified against disk
after every edit) + real `fetched_at: "2026-07-30T00:51:30Z"` + `via: "curl+snapshot-extract.sh"`.
No other manifest entries were touched (`recharts-2026-05` manifest entry is untouched — see §6).

## 5. Requote fixes (citation, not snapshot, edited — 19 quote edits across 18 files)

Every fix below: the OLD quote was verified absent from the live-fetched, extracted page; the
NEW quote is a literal (verbatim) substring of that extraction. None of these are paraphrases —
where the old citation itself was a fabricated gloss (not a real quote from any page), the
citation now anchors to genuine page text instead.

| File | upstream_id | old quote (truncated) | new quote |
|---|---|---|---|
| templates/L1/components/calendar.tsx | shadcn-ui-2026-05 | "A date field component that allows users to enter and edit date. Built on top of react-day-picker." | "A calendar component that allows users to select a date or a range of dates." |
| templates/L1/components/combobox.tsx | shadcn-ui-2026-05 | "Autocomplete input and command palette with Radix UI and cmdk." | "Autocomplete input with a list of suggestions." |
| templates/L1/components/command.tsx | shadcn-ui-2026-05 | "Fast, composable, unstyled command menu for React." | "Command menu for search and quick actions." |
| templates/L1/components/date-picker.tsx | shadcn-ui-2026-05 | "...Built using the Popover and the Calendar components." (trailing clause not verbatim) | "A date picker component with range and presets." |
| templates/L1/components/date-range-picker.tsx | shadcn-ui-2026-05 | same as date-picker.tsx | same as date-picker.tsx |
| templates/L1/components/form.tsx | shadcn-ui-2026-05 | "Building forms with React Hook Form and Zod." | "Build forms with React and shadcn/ui." |
| templates/L1/components/input.tsx | shadcn-ui-2026-05 | "Displays a form input field or a component that looks like an input field." | "A text input component for forms and user data entry with built-in styling and accessibility features." |
| templates/L1/components/otp-input.tsx | input-otp-2026-05 | "One-time password input component for React. Accessible. Unstyled. Customizable." | "One-time passcode input for React." |
| templates/L1/components/otp-input.tsx | shadcn-ui-2026-05 | "...copy paste functionality." (space, not hyphen) | "...copy-paste functionality." |
| templates/L1/components/progress.tsx | shadcn-ui-2026-05 | "Displays an indicator showing the completion progress of a task." | "...task, typically displayed as a progress bar." (extended — old text was a truncated prefix, not a full match) |
| templates/L1/components/sonner.tsx | shadcn-ui-2026-05 | "An opinionated toast component for React." (stale — that was shadcn's OLD `toast` component's description) | "A succinct message that is displayed temporarily." |
| templates/L2/blocks/toast.tsx | shadcn-registry-2026-05 | same stale toast quote | same fix |
| templates/L2/blocks/toast-queue.tsx | shadcn-registry-2026-05 | same stale toast quote | same fix |
| templates/L1/components/file-dropzone.tsx | react-dropzone-2026-05 | "The primary API for integrating drag-and-drop functionality into React components." (not found on live site — doc site redesigned to a multi-page Guide) | "react-dropzone is a set of React hooks and components for creating a drag 'n' drop zone for files." |
| templates/L2/blocks/theme-switcher.tsx | next-themes-2026-05 | section "Cookie-based SSR theme — avoid flash of incorrect theme" / quote "Use cookies to store the theme..." (the real library does NOT use cookies — localStorage + blocking script instead; fabricated premise) | section **"No-flash theme abstraction"** / quote "An abstraction for themes in your React app." |
| templates/L1/components/address-search.tsx | kakao-postcode-2026-05 | "The callback receives a data object with address information. Key properties: zonecode, roadAddress, jibunAddress" (English gloss; source is Korean-only) | Korean, verbatim from the official guide: "이 함수를 정의할때 넣는 인자에는 우편번호 검색 결과 목록에서 사용자가 클릭한 주소 정보가 들어가게 됩니다." |
| templates/L2/blocks/billing-history.tsx | stripe-billing-2026-05 | "customer.subscription.updated — Plan change, quantity change, trial end." (fabricated summary — not Stripe's actual event description) | "Sent when a subscription starts or changes." |
| templates/L2/blocks/invoice-list.tsx | stripe-billing-2026-05 | "invoice.payment_succeeded — Invoice paid; renew subscription." (fabricated summary) | "Occurs whenever an invoice payment attempt succeeds." |
| templates/L2/blocks/pricing-table.tsx | stripe-billing-2026-05 | "A Price defines the recurring amount, currency, and interval." (not found; real API reference wording differs) | "The recurring components of a price such as interval and usage" |

27 of the 37 shadcn-ui-2026-05 quotes needed **zero** changes (verbatim on first fetch):
accordion, alert, alert-dialog, aspect-ratio, avatar, badge, button, card, checkbox,
collapsible, dialog, dropdown-menu, hover-card, label, popover, radio-group, resizable,
scroll-area, select, separator, sheet, skeleton, slider, switch, tabs, textarea, tooltip.

## 6. Recharts probe (residual, no snapshot authored)

5 canonical URLs probed (manifest source + the 4 chart-type API pages implied by the citing
templates' `section:` names) + 2 informational checks on the bare domain root:

| URL | HTTP | bytes (raw) | notes |
|---|---|---|---|
| `https://recharts.org/en-US/api` (manifest source) | 404 | 1938 | GitHub Pages SPA-redirect shell |
| `https://recharts.org/en-US/api/BarChart` | 404 | 1938 | same shell, byte-identical |
| `https://recharts.org/en-US/api/PieChart` | 404 | 1938 | same shell, byte-identical |
| `https://recharts.org/en-US/api/LineChart` | 404 | 1938 | same shell, byte-identical |
| `https://recharts.org/en-US/api/ResponsiveContainer` | 404 | 1938 | same shell, byte-identical |
| `https://recharts.org` (informational) | 200 | 633 | client-side redirect notice → `recharts.github.io` (matches the PRD's "633-byte" figure exactly — that figure describes THIS root URL, not `/en-US/api`) |
| `https://recharts.org/en-US` (informational) | 404 | 1938 | same shell |

All 5 canonical URLs return the identical 1938-byte SPA-shell body (sha256
`6c06d060d78489c91293b67ad5af0400d2cf7f672f81fad3679fadb3a2fa79d4`) — genuinely dead by static
fetch, exactly as the PRD predicted. Per instructions, **no `recharts-2026-05.snapshot.md` was
authored** and the manifest's existing (fabricated) `recharts-2026-05` entry was **left
untouched** — it was never "touched" in the sense of gaining a real snapshot body. All 7 probe
rows are committed in `_FETCH-RECEIPTS.yaml` (r049–r055) as the residual's provenance. Registering
the BACKLOG residual row itself is W5(c), owned by the main loop, not Lane A.

Honest anomaly vs the PRD text: the PRD says "recharts.org/en-US/api is 404 / a 633-byte
client-rendered SPA shell" as if one URL were both — the 633-byte body actually belongs to the
bare `https://recharts.org` root (a redirect notice to `recharts.github.io`), which is a
*different* URL from the 404 canonical source and returns HTTP 200, not 404. The underlying
"dead by static fetch" conclusion is unaffected; only the URL/byte-count pairing in the PRD's
prose was imprecise, recorded here for the record.

## 7. Residual findings (must be recharts-only)

```
$ bash practices/evals/evidence_quote_spotcheck_guard.sh --include-templates
... 7 finding(s) ...
```
All 7 are `TEMPLATE_SNAPSHOT_FILE_MISSING` for `upstream_id=recharts-2026-05` across
`bar-chart.tsx`, `heatmap.tsx`, `kpi-card.tsx`, `pie-chart.tsx`, `sparkline.tsx`, and
`time-series-chart.tsx` (×2, one identity, two evidence entries) — exactly the expected 6
identities / 7 findings. Zero non-recharts findings remain.

Also verified clean/unregressed:
- `--strict` (rules/ sweep): exit 0, all 185 quotes verified.
- `--strict --strict-templates --templates-only-protected`: exit 0, 18 files / 22 anchors / 0
  findings (unchanged from before this wave — nothing I touched intersects the protected ledger).
- `evidence_guard.sh`: exit 0 (1028 evidence entries, structurally sound).
- `time_decay_guard.sh` (both catalogs): exit 0.
- `quote_match_check.sh`: 2 pre-existing warnings, both in `practices/rules/*` (Java-side
  spring-boot-sql-migration) — unrelated to anything in this wave's scope.

## 8. Clean-identity list (all 52 W1 identities, now verbatim-passing)

All 52 census identities pass `--include-templates` with zero findings EXCEPT the 6 recharts
identities (7 findings), which are the documented, expected, out-of-scope-for-static-fetch
residual (§6). Every shadcn-ui-2026-05 (37), shadcn-registry-2026-05 (2), stripe-billing-2026-05
(3), react-dropzone-2026-05 (1), next-themes-2026-05 (1), kakao-postcode-2026-05 (1), and
input-otp-2026-05 (1) identity — 46 of 52 — is verbatim-clean.

## 9. Handoff to Lane B

- `practices/upstream/_FETCH-RECEIPTS.yaml` is committed and complete (63 rows: 55 fetch + 8
  assembly).
- Header/body split convention for the integrity guard: split each touched `.snapshot.md` on
  the first literal `---\n\n`; everything after is the body whose sha256 must equal both (a)
  the header's own stated `Body SHA-256` line and (b) the matching assembly (or single-URL)
  receipt's `body_sha256`/`sha256`.
- Manifest `sha`/`bytes` for all 8 touched identities already equal the true whole-file
  `shasum -a 256`/`wc -c` values — re-verified independently in §4, not just self-reported by
  the build scripts.
- Not done here (explicitly out of Lane A scope): `manifest_snapshot_integrity_guard.sh` itself,
  the baseline-71/residual-63 allowlist, `run-all-guards.sh` registration, `[87]` fixture-kill
  entry, `DECISIONS.md` entries, the five-halves ratchet to 64, and the NAMED RED demo — all
  Lane B (W1b) or the main loop (W5/W5b).
