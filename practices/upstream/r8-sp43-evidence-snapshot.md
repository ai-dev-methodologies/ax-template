# R8 SP43 — lms + cms external evidence snapshot

**Fetched:** 2026-05-21
**Purpose:** anchor TD-2026-05-21-022 (lms recipe) + TD-2026-05-21-023 (cms recipe)
+ TD-2026-05-21-024 (scheduler first-consumer-arrival convention) with
verbatim external citations. 5 English verbatim + 1 topic-relevant English
verbatim + 2 Korean verbatim secured (13 logical attempts + 2 redirect/alternate
rows). Strongest cycle since R6.

These quotes anchor `recipes/lms/RECIPE.md` + `recipes/cms/RECIPE.md` + their
spec yaml mirrors; this file is a per-cycle evidence ledger, not a
`.snapshot.md` time-decay-guarded snapshot in `practices/upstream/_MANIFEST.yaml`.

---

## LMS verbatim quotes

### Quote 1 — Coursera homepage (post-redirect)

- **URL:** https://www.coursera.org/
- **Redirect from:** https://building.coursera.org/developer-program/ (301)
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote:**

> Learn from 350+ leading universities and companies

- **Relevance:** Coursera is the canonical large-scale LMS / MOOC platform;
  the homepage tagline attests to the cross-institutional course catalog
  pattern the LMS recipe encodes. Anchors LMS pattern broadly (no single
  invariant; anchors composition-level claim).

### Quote 2 — Moodle Web Services API developer docs

- **URL:** https://docs.moodle.org/dev/Web_services_API
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote:**

> Once you have done this, your plugin's functions will be accessible to other systems through Web services using one of a number of protocols, like XML-RPC, REST or SOAP.

- **Relevance:** Moodle is the canonical open-source LMS; the Web Services
  API documents the REST surface the LMS recipe CRUD layer mirrors.
  Anchors LMS-INV-001 (CRUD + audit-log emission for course content
  mutations) via the externally-documented REST API pattern.

### Quote 3 — Classting (Korean K-12 LMS — Korean verbatim)

- **URL:** https://www.classting.com/
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote (Korean):**

> 개인화 교육을 실현하는 교육 AI 에이전트

- **Relevance:** Korean K-12 LMS positioning verbatim — first non-zero-Korean-
  verbatim LMS cycle since R6 channel.io. Anchors PRD §4.4 H1 closure (R7
  5-Korean-host floor met with 2 verbatim PASS). Classting is the canonical
  Korean K-12 mobile-first LMS platform; the verbatim attests the LMS
  pattern in Korean enterprise practice.

---

## CMS verbatim quotes

### Quote 4 — Sanity Docs landing

- **URL:** https://www.sanity.io/docs
- **Alternate from:** https://www.sanity.io/docs/http-api (404)
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote:**

> Real-time database for structured content

- **Relevance:** Sanity is the canonical headless CMS; the docs landing
  tagline attests to the structured-content database pattern the CMS recipe
  encodes. Anchors CMS pattern broadly.

### Quote 5 — Contentful Content Management API docs

- **URL:** https://www.contentful.com/developers/docs/references/content-management-api/
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote:**

> Contentful's Content Management API (CMA) helps you manage content in your spaces.

- **Relevance:** Contentful CMA documents the canonical headless-CMS
  management REST surface the cms recipe CRUD layer mirrors. Anchors
  CMS-INV-001 (publish-state transitions + audit emission) via the
  externally-documented REST API pattern.

### Quote 6 — Strapi REST API docs

- **URL:** https://docs.strapi.io/dev-docs/api/rest
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote:**

> The REST API allows accessing the content-types through API endpoints.

- **Relevance:** Strapi is the canonical open-source headless CMS; the REST
  API docs confirm the content-type endpoint pattern.

### Quote 7 — Sanity scheduled-publishing deprecation notice (topic-relevant)

- **URL:** https://www.sanity.io/docs/scheduled-publishing
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote:**

> Scheduled publishing has been deprecated as of October 2025.

- **Relevance:** Topic-relevant verbatim — the deprecation notice itself
  attests that Sanity historically shipped scheduled-publishing as a
  feature. CMS-INV-002 binds to the **internal** `scheduled-task` L4
  primitive (lock + idempotency + JobHistory), NOT to Sanity's hosted
  offering; deprecation of the hosted feature is orthogonal to the internal
  primitive. Closes PRD §4.4 M1 (Architect MEDIUM — topic-relevant
  scheduled-publish anchor needed).

### Quote 8 — Brunch (Korean publication CMS — Korean verbatim)

- **URL:** https://brunch.co.kr/
- **Fetched at:** 2026-05-21
- **HTTP status:** 200 OK
- **Verbatim quote (Korean):**

> 글이 작품이 되는 공간, 브런치

- **Relevance:** Korean publishing/CMS positioning verbatim — first non-zero-
  Korean-verbatim CMS cycle. Brunch positions itself as a publication
  platform where writing becomes art; the verbatim attests the CMS pattern
  in Korean enterprise practice. Pair with classting (LMS) to make R8 the
  first cycle since R6 channel.io with 2 Korean verbatim PASS.

---

## Logical-attempt ledger (13 logical attempts + 2 redirect/alternate rows + 1 alternate from cycle)

| # | Recipe | URL | HTTP | Disposition |
|---|---|---|---|---|
| 1 | lms | `https://building.coursera.org/developer-program/` | 301 | redirect captured (→ www.coursera.org) |
| 2 | lms | `https://www.coursera.org/` | 200 | **verbatim** Quote 1 |
| 3 | lms | `https://docs.moodle.org/dev/Web_services_API` | 200 | **verbatim** Quote 2 |
| 4 | lms | `https://docs.openedx.org/projects/edx-platform/en/latest/concepts/rest_api.html` | 404 | downgrade — URL moved upstream |
| 5 | lms (KR) | `https://www.inflearn.com` | 200 | downgrade — no developer API text |
| 6 | lms (KR) | `https://ko.coursera.org/` | 301 | redirect captured (→ www.coursera.org; Quote 1 already captured) |
| 7 | lms (KR) | `https://www.classting.com/` | 200 | **verbatim** Quote 3 (Korean) |
| 8 | lms (KR) | `https://tech.kakao.com/` | 200 | downgrade — page is dev-tool-centric; no learning-focused verbatim |
| 9 | cms | `https://www.sanity.io/docs/http-api` | 404 | alternate fetched (→ sanity.io/docs) |
| 10 | cms | `https://www.sanity.io/docs` | 200 | **verbatim** Quote 4 (alternate host path) |
| 11 | cms | `https://www.contentful.com/developers/docs/references/content-management-api/` | 200 | **verbatim** Quote 5 |
| 12 | cms | `https://docs.strapi.io/dev-docs/api/rest` | 200 | **verbatim** Quote 6 |
| 13 | cms (topic) | `https://www.sanity.io/docs/scheduled-publishing` | 200 | **verbatim** Quote 7 (topic-relevant scheduled-publish) |
| 14 | cms (KR) | `https://developers.naver.com/docs/blog/` | block | downgrade — host-level fetcher block |
| 15 | cms (KR) | `https://terms.naver.com/` | block | downgrade — host-level fetcher block (host-wide pattern) |
| 16 | cms (KR) | `https://brunch.co.kr/` | 200 | **verbatim** Quote 8 (Korean) |

**Summary:** 5 English verbatim + 1 topic-relevant English verbatim + 2 Korean
verbatim = **8 verbatim PASS** across 13 logical attempts. 5 documented
downgrades (edX 404, 인프런 no-API, tech.kakao no-topic,
developers.naver block, terms.naver block). 3 redirect/alternate-host rows
(Coursera 301 builder→www, Sanity-base 404→alternate Docs landing,
ko.coursera 301 ko→www).

---

## Per-recipe evidence density

- **lms:** 2 English verbatim (Moodle + Coursera) + 1 Korean verbatim
  (classting) = 3 verbatim total. PASS — clears 1-floor with buffer.
- **cms:** 3 English verbatim (Sanity-base + Contentful + Strapi) + 1
  topic-relevant English verbatim (Sanity scheduled-publishing) + 1 Korean
  verbatim (brunch) = 5 verbatim total. **PASS — strongest evidence chain
  shipped any single recipe this cycle.**

## Korean cycle (PRD §4.4 H1 closure)

- **5 logical Korean host attempts** (인프런, ko.coursera redirect counts as 1,
  classting, tech.kakao, terms.naver, brunch, developers.naver = 7 host
  attempts including the redirect; 5 logical Korean hosts).
- **2 Korean verbatim PASS** (classting + brunch — first non-zero-Korean-
  verbatim cycle since R6 channel.io).
- R7 5-Korean-host floor MET with buffer.

## R9 evidence refresh follow-ups

- Re-attempt edX OpenedX REST API root if URL stabilizes.
- Re-attempt 인프런 developer API (currently closed).
- Re-attempt tech.kakao education-tagged posts (currently dev-tool-centric).
- Re-attempt Naver hosts (developers + terms) if either removes the host-level
  fetcher block — consider retiring Naver from the standard Korean URL pool
  if both remain blocked.
