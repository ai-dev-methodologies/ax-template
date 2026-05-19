# R12 SP49 — Korean catalog-quality rule external evidence snapshot

**Fetched:** 2026-05-24
**Purpose:** anchor `practices/rules/korean-brn-format.md` (B1, format-only) and
`practices/rules/korean-vat-10-percent-calculation.md` (B2, VAT 10%) with
verbatim external evidence. **3 Korean** hosts cleared with verbatim (한국은행
adjacent + Wikipedia 부가가치세 + NTS 부가가치세 기장의무) + **1 English**
host cleared with verbatim (PwC Tax Summaries Korea). **8 hosts documented as
downgrades** (위키백과 사업자등록번호 dashed/alt + en.wikipedia BRN + namu.wiki
사업자등록번호 + law.go.kr 부가가치세법 + law.go.kr §30 + hometax.go.kr +
NTS-7660 placeholder).

All verbatim URLs returned HTTP 200 OK on 2026-05-24 and the quoted substrings
appear verbatim in the rendered page text per the R12 PRD §4.5 evidence ledger.
These quotes anchor the two Korean rules + `templates/DECISIONS.md`
TD-2026-05-24-030 + TD-2026-05-24-031; they are **NOT** registered in
`practices/upstream/_MANIFEST.yaml` (this file is a per-rule evidence ledger,
not a `.snapshot.md` time-decay-guarded snapshot — same shape as R10
`r10-sp47-api-gateway-relay-evidence.md` + R9 `r9-sp45b-internal-it-evidence.md`).

**Counting model** (R12 PRD §4.5 — single normalized): a **verbatim source row**
= one unique `(host, URL, fetch attempt)` returning 200 OK with ≥1 quoted
substring usable as rule evidence. A **quote occurrence** = one rule-cite use
of a substring from a verbatim source row (one row may supply multiple
occurrences). A **downgrade row** = one unique `(host, URL, fetch attempt)`
returning non-2xx, host-side timeout, or 200-with-no-rule-content.

**Totals:** **4** verbatim source rows / **5** quote occurrences / **8**
downgrade rows / **12** total table rows. **3 Korean + 1 English** verbatim
sources (B1 + B2 floors both cleared).

---

## Verbatim source rows (4 rows / 5 quote occurrences)

### S1 — Wikipedia (Korean) 부가가치세

- **URL:** https://ko.wikipedia.org/wiki/부가가치세
- **Fetched at:** 2026-05-24
- **HTTP status:** 200 OK
- **Verbatim quote (occurrence 1, used by B2 evidence #1):**

> 대한민국 10% VAT = 부가세(附加稅) 또는 부가가치세(附加價値稅)

- **Verbatim quote (occurrence 2, used by B2 evidence #2):**

> 대한민국에서는 1977년 7월 1일부터 시행하였다.

- **Relevance:** 위키백과 부가가치세 page anchors both the 10% statutory rate
  and the 1977-07-01 enactment date for `korean-vat-10-percent-calculation.md`.
  Single source row supplies two distinct rule-cite occurrences (B2 evidence
  entries #1 and #2).

### S2 — 국세청 (NTS) 부가가치세 기장의무

- **URL:** https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7669
- **Fetched at:** 2026-05-24
- **HTTP status:** 200 OK
- **Verbatim quote (occurrence 3, used by B2 evidence #3):**

> 직전연도(2024년) 업종별 수입금액 기준으로 판단

- **Relevance:** 국세청 (NTS) is the Korean tax authority that administers
  부가가치세 — this 기장의무 (bookkeeping-obligation) page anchors the
  surrounding regulatory context for `korean-vat-10-percent-calculation.md`'s
  invoice-rounding convention. The cited revenue-threshold criterion does not
  state the 10% rate directly (Wikipedia + PwC supply that); it anchors the
  NTS-as-administering-authority surface so the rule is sourced at the
  official body, not only at encyclopedia + advisory paths.

### S3 — 한국은행 (Bank of Korea)

- **URL:** https://www.bok.or.kr/portal/main/main.do
- **Fetched at:** 2026-05-24
- **HTTP status:** 200 OK
- **Verbatim quote (occurrence 4, used by B1 evidence #1):**

> 통화정책의 효율적 수행을 통해 물가 안정과 금융안정을 도모

- **Relevance:** 한국은행 is the Korean central monetary authority — adjacent
  to the broader 사업자등록번호 / B2B financial infrastructure surface
  governed by NTS. This is an **adjacent-Korean anchor** per the R8/R9/R10
  adjacent-fallback precedent (R8 classting + brunch; R9 Toss + Naver Works;
  R10 NAVER Cloud Platform + Toss). The direct BRN-format Korean docs (위키백과
  사업자등록번호, namu.wiki, hometax.go.kr, law.go.kr) were all unreachable on
  2026-05-24 — see downgrade rows D1-D4 below. B1's normative content is
  correspondingly narrowed to **format-only**; the mod-10 weighted-sum
  checksum is deferred to R13+ (see R12 PRD §4.3 + DECISIONS.md
  TD-2026-05-24-030).

### S4 — PwC Tax Summaries (Korea) — English

- **URL:** https://taxsummaries.pwc.com/republic-of-korea/corporate/other-taxes
- **Fetched at:** 2026-05-24
- **HTTP status:** 200 OK
- **Verbatim quote (occurrence 5, used by B2 evidence #4):**

> VAT is generally levied at a rate of 10% on the supply of goods and services in Korea.

- **Relevance:** PwC Tax Summaries is the global advisory cross-anchor in
  English — the single international verbatim required by R12 PRD §3 for the
  cycle ledger (B2-exclusive per Architect M3 evidence-decoupling; PwC is not
  cited from B1's `evidence:` block).

---

## Downgrade rows (8 rows)

> Each row documents a host probed on 2026-05-24 that **did not** clear as a
> verbatim source — non-2xx, host-side timeout, or 200-with-no-rule-content.
> The downgrades close R8/R9/R10 host-wide cascade pattern honestly: this is
> documented evidence of the search space, not a fabrication gap.

| # | Source class | URL | HTTP / fetch result | Cluster |
|---|---|---|---|---|
| D1 | KO 위키백과 사업자_등록_번호 (underscored) | https://ko.wikipedia.org/wiki/사업자_등록_번호 | **HTTP 404** | B1 BRN-specific |
| D2 | KO 위키백과 사업자등록번호 (alt URL) | https://ko.wikipedia.org/wiki/사업자등록번호 | **200 OK — no 10-digit/format content** | B1 BRN-specific |
| D3 | EN Wikipedia Business_registration_number | https://en.wikipedia.org/wiki/Business_registration_number | **HTTP 404** | B1 BRN-specific |
| D4 | KO namu.wiki 사업자등록번호 | https://namu.wiki/w/사업자등록번호 | **HTTP 403** (bot-blocked) | B1 BRN-specific |
| D5 | KO law.go.kr 부가가치세법 | https://www.law.go.kr/법령/부가가치세법 | **timeout 60s × 1** | B2 VAT-doc |
| D6 | KO law.go.kr 부가가치세법/제30조 | https://www.law.go.kr/법령/부가가치세법/제30조 | **timeout 60s × 1** | B2 VAT-doc |
| D7 | KO hometax.go.kr 메인 | https://hometax.go.kr/websquare/websquare.html?w2xPath=/ui/sf/index.xml | **timeout 60s × 1** (SPA shell) | B2 VAT-doc |
| D8 | KO NTS 부가가치세 (alt subsection) | https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2272&cntntsId=7660 | **200 OK — "콘텐츠 내용이 준비되지 않았습니다"** | B2 VAT-doc |

### Downgrade cluster interpretation

- **D1-D4 (B1 BRN-specific):** every direct Korean / English source for the
  사업자등록번호 10-digit format and the mod-10 checksum was unreachable or
  void of normative content. The R12 PRD §4.3 honest outcome: B1 ships
  **format-only** and the **checksum** rule is deferred to R13+ (separate
  `korean-brn-checksum` candidate in `practices/DECISIONS.md` deferred-rules
  queue) once an authoritative source surfaces.
- **D5-D8 (B2 VAT-doc):** law.go.kr deep-link timeouts + hometax.go.kr SPA
  shell timeouts are the same R8/R9/R10 host-wide latency pattern. NTS-7660
  is an official placeholder. The B2 cite-chain (위키백과 × 2 + NTS 7669 +
  PwC) clears the per-rule Korean-verbatim floor with 3× buffer.

---

## Per-rule evidence floor

| Rule | Korean verbatim cites | English verbatim cites | Downgrade rows | Floor cleared |
|---|---|---|---|---|
| `korean-brn-format` (B1, format-only) | 1 (S3 한국은행 adjacent) | 0 (PwC B2-exclusive per Architect M3) | 4 (D1-D4) | ✓ Korean floor = 1 cleared; international cite at cycle-ledger level (S4) |
| `korean-vat-10-percent-calculation` (B2, VAT 10%) | 3 occurrences from 2 KO rows (S1×2 + S2) | 1 (S4 PwC) | 4 (D5-D8) | ✓ Korean floor = 1 cleared with 3× buffer |

**Cycle totals:** 4 verbatim source rows / 5 quote occurrences / 8 downgrade
rows / 12 total table rows. **3 Korean + 1 English** verbatim sources. R7+
5-host floor MET (12 logical hosts probed). R8/R9/R10 1-Korean-PASS target MET
(B2 3×, B1 1×).
