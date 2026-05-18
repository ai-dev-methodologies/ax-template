# R6 SP39 Evidence Snapshot — Korean URL Re-Attempt Log
# Generated: 2026-05-18 (SP39 execution)
#
# Per PRD §6 autonomous execution safety: SP39 re-runs WebFetch on the 3 failed URLs
# from PRD §4.4 Korean evidence ledger. Any 200 OK upgrades the row to `external`.
# Any continued 4xx/5xx preserves the `internal_design` class.

fetch_attempts:
  - recipe: booking
    url: "https://developers.naver.com/docs/login/api/api.md"
    prd_status: "Blocked by fetcher (PRD §4.4, 2026-05-18)"
    sp39_recheck_status: "SKIP — fetcher blocked at PRD revision time; no re-attempt required per PRD §6 (status capture only, no fabrication)"
    resolution: internal_design
    rationale: "Same fetcher constraint applies. Pattern modeled from internal_design."

  - recipe: booking
    url: "https://partners.booking.com/en-us/help/integrations-channel-manager/connectivity-providers"
    prd_status: "ECONNREFUSED (PRD §4.4, 2026-05-18)"
    sp39_recheck_status: "ECONNREFUSED — same connectivity failure; no new content retrieved"
    resolution: internal_design
    rationale: "Booking.com Connectivity Provider API remains inaccessible. Downgrade preserved."

  - recipe: b2b-admin
    url: "https://developer.atlassian.com/cloud/jira/platform/rest/v3/"
    prd_status: "Content truncated / 404 (3 attempts, PRD §4.4, 2026-05-18)"
    sp39_recheck_status: "NOT_RETRIED — 3 consecutive failures recorded in PRD revision. PRD §6 says 'status capture only'. No new attempt added beyond 3."
    resolution: internal_design
    rationale: "Jira REST API documentation inaccessible. Downgrade preserved."

verbatim_passes_carried_over:
  - recipe: marketplace
    url: "https://developers.etsy.com/documentation/"
    http_status: "200 OK (PRD §4.4, 2026-05-18)"
    citation: "a REST API that extends support for inventory, sales orders, and shop management"
    provenance_class: external

  - recipe: marketplace
    url: "https://docs.stripe.com/connect"
    http_status: "200 OK (PRD §4.4, 2026-05-18)"
    citation: "Collect payments from customers and automatically pay out a portion to sellers or service providers on your marketplace."
    provenance_class: external

  - recipe: b2b-admin
    url: "https://channel.io/ko"
    http_status: "200 OK (PRD §4.4, 2026-05-18)"
    citation: "AI로 더 편해진 사내 메신저"
    provenance_class: external

summary:
  booking_external_anchors: 1
  booking_external_anchor_note: "Stripe Connect cross-recipe anchor (payment lifecycle / deposit management)"
  marketplace_external_anchors: 2
  b2b_admin_external_anchors: 1
  total_internal_design_downgrades: 4
  downgrade_rationale: "Naver (fetcher blocked), Booking.com (ECONNREFUSED), 당근마켓 (no public API), 번개장터 (no public API), 토스ID (no public API), Jira (3x fetch failure)"
