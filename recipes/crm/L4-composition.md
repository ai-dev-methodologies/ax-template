# L4 Composition — crm

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
crud (lead/contact/deal/activity)
 └── pipeline entities with stage state machine
      ├── search ← index lead/contact/deal on save
      ↓
      deal.stage_changed event
      ↓
notification
 └── receives DealStageChangedEvent → sends alert to deal owner
audit-log
 └── records every pipeline mutation (lead.converted, deal.stage.changed, activity.logged)
```

## Domain Configuration Notes

### `crud`
- Four CRUD entities in this recipe: `Lead`, `Contact`, `Deal`, `Activity`
- Deal has a state machine: PROSPECT → QUALIFIED → PROPOSAL → NEGOTIATION → WON / LOST
- Lead → Contact conversion: copy `source_attribution` field to Contact; log conversion in audit-log
- All currency fields (deal.amount) must use `BigDecimal` with scale=2 (ISO 4217 precision)
- Reference: `templates/L4/crud/`

### `audit-log`
- Annotate lead service: `@Audited(action = "lead.converted")`
- Annotate deal service: `@Audited(action = "deal.stage.changed")`
- Annotate activity service: `@Audited(action = "activity.logged")`
- `source_attribution` field must be preserved in conversion audit entry
- Retention: ≥90 days per `spec_ref: specs/audit-log-l0.yaml`
- Reference: `templates/L4/audit-log/`

### `notification`
- Subscribe to `DealStageChangedEvent` via `@EventListener`
- Subscribe to `ActivityReminderEvent` via `@Scheduled` check
- Channels: email + in-app push
- Templates: `deal_stage_changed`, `deal_won`, `deal_lost`, `activity_due`
- Reference: `templates/L4/notification/`

### `search`
- Index entities: Lead (name, company, email), Contact (name, email), Deal (title, stage, owner)
- Trigger re-index on every CREATE / UPDATE of these entities via `@SearchIndexed`
- Full-text + saved views support per `saved-view.tsx` block
- Reference: `templates/L4/search/`

## Pipeline State Machine

```
PROSPECT
    │ qualify
    ↓
QUALIFIED
    │ propose
    ↓
PROPOSAL
    │ negotiate
    ↓
NEGOTIATION
    │ close_won          close_lost
    ↓                         ↓
  WON                       LOST
```

Transitions are validated in `DealService.transitionStage()`. Invalid transitions throw `IllegalDealStageTransitionException` (HTTP 422). Every valid transition emits a `DealStageChangedEvent` AND an audit log entry.

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md or Spec Trio metadata:
```
applied_recipe: crm
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37)
