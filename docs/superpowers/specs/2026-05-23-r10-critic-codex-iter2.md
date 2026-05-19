# Codex Critic R10 iter 2

## Verdict

ITERATE.

Six of seven closures are closed. M1 reopens: §4.4 source-class arithmetic is restated, but the restated counts are still wrong against the visible table. This is narrow and surgical: fix the row-count paragraph/summary only.

## Closure check (7)

| # | Finding | Status | Evidence |
|---|---|---|---|
| 1 | H1 Korean fresh-vendor | CLOSED | §4.4 L210 adds NAVER Cloud Platform `https://www.ncloud.com/product` with verbatim Korean: `"API 호출, 관리, 모니터링 등 API와 관련된 모든 작업을 실행할 수 있는 서비스"`. It is a fresh vendor distinct from R9 Toss. |
| 2 | H2 ratelimit guard audit sentence | CLOSED | §6 L258 states the iter 1 audit result: `spec_ref` resolution checks file existence + ID presence only, not L4 directory presence; `specs/ratelimit-l0.yaml#RATELIMIT-1/2` is guard-compatible without `templates/L4/ratelimit/`. |
| 3 | M1 §4.4 arithmetic restated by source class | REOPEN | §4.4 L218-L220 restates arithmetic, but the visible table L190-L210 has 19 data rows and 21 physical table rows including header+separator, not updated total 20. It also has 8 `Downgrade` rows (L200-L202, L204-L208), while L218/L220 still say 7. |
| 4 | M2 TD-028 Korean vendor rotation Follow-up | CLOSED | §8 L303 adds R9 Toss -> R10 NAVER Cloud Platform rotation precedent and the R12 escalation path. |
| 5 | M3 disambiguation pre-committed verbatim | CLOSED | Exact sentence appears at §4.1 L141, §7 P3 L284, §8 TD-028 L294, and changelog L410. The tdd anchor at L172 asserts the same sentence verbatim. |
| 6 | M4 §3 Must-Have wording | CLOSED | §3 L113 says: "5 INVs, each with >=1 anchor; all anchors disk-resolvable." §4.1 L147 echoes that all 5 are disk-resolved and each has >=1 anchor. |
| 7 | L Option (a) clean-revert unified | CLOSED | Clean-revert/no-deferred-entry policy is consistent across §3 L115/L132, §6 L260-L274, §8 L298/L310-L311, §9 L318/L329, §10 L337, and §11 L361/L364. The §6 FAIL row L272 explicitly leaves api-gateway-relay absent from both active and `deferred_recipes:`. |

## Disk validation

- `wc -l docs/superpowers/specs/2026-05-23-r10-api-gateway-relay-prd.iter2.md` -> 415.
- NAVER Cloud verbatim text is present at L210.
- §6 FAIL row L272 is consistent with §9 deferred-queue invariant L318/L329 and §10 L337.
- Iter 2 changelog is present at L402-L415.
- Source-table count check fails M1: L190-L210 contains 21 physical table rows including header+separator and 19 data rows. Semantic source-class counts are 6 rows containing `Verbatim cite` text, 1 `final-verbatim-via-alternate`, 8 `Downgrade`, 3 `Followed redirect`, and 1 `alternate-fetched-as-bridge`.

## Independent attack

**BLOCKING - §4.4 arithmetic still miscounts the evidence ledger.**

Architect iter 2 approved M1 based on the presence of a source-class restatement, but the restatement does not match the table:

- L218 says the iter 2 NAVER row updates the total from 19 -> 20 "incl. header+separator."
- The table at L190-L210 has header L190, separator L191, and data rows L192-L210: 21 physical table rows including header+separator, or 19 data rows.
- L218/L220 say `Downgrade rows = 7`, but visible downgrade rows are L200, L201, L202, L204, L205, L206, L207, and L208: 8 rows.
- The "7 verbatim" claim is only correct if it means verbatim-bearing rows: 6 rows with `Verbatim cite` text plus Cloudflare's `final-verbatim-via-alternate` row L196. If `final-verbatim-via-alternate` remains a separate source class, the paragraph should not also fold it into "Verbatim cite rows" without naming that distinction.

Concrete fix: replace L218-L220 and the §12 evidence summary L396 with unambiguous arithmetic, e.g. "19 data rows; 21 physical table rows including header+separator; verbatim-bearing rows = 7 (6 `Verbatim cite` + 1 `final-verbatim-via-alternate`); downgrade rows = 8; followed-redirect rows = 3; alternate bridge rows = 1."

## Final reasoning

H1, H2, M2, M3, M4, and Codex L Option (a) are closed with line evidence. The deferred-recipes contradiction has been resolved cleanly, and the n=1 partial-tag policy correctly collapses to PASS/tag or FAIL/clean-revert.

Do not approve yet because M1's target was arithmetic clarity, and the PRD still carries wrong ledger totals. This is a narrow iter 3 edit: fix the §4.4 arithmetic paragraph and the §12 evidence summary; no architecture changes needed.

## ADR (if APPROVE)

N/A - not ADR-ready until M1 arithmetic is corrected.
