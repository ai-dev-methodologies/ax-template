# L4 Composition — community

> Which L4 domains to enable and how they wire together.

## Domain Wiring

```
auth
 └── authenticated post creation (anti-automation rate-limit per ASVS-V2.2.1)
      ↓
crud (Post)
 └── POST /api/posts → soft-delete-aware
      ↓
crud (Comment)
 └── POST /api/posts/{id}/comments (threaded reply chain)
      ↓
notification
 └── ReplyCreated event → fanout filtered by NotificationPreferences
      ↓ moderation path
crud (Post / Comment)
 └── PATCH /api/posts/{id}/status (HIDE | RESTORE | LOCK) by moderator
      ↓
audit-log
 └── @Audited("post.status.changed") + @Audited("comment.status.changed")
      records operator + before/after status
      ↓
search
 └── full-text index excludes soft-deleted threads (SEARCH-AUTHZ-001)
      ↓ sanitize path
co-shipped community-html-sanitization
 └── server-side HTML sanitize before persist (XSS prevention)
      ↓ co-shipped invariant test asserts <script> stripped on POST/PATCH
```

## Domain Configuration Notes

### `crud`
- Two CRUD entities: `Post` and `Comment`
- Soft-delete (`deleted_at`) — guards listing + search queries
- Threading: `Comment.parent_id` for nested replies (max depth advisory)
- Lifecycle states: `DRAFT` → `PUBLISHED` → `HIDDEN` | `LOCKED` | `DELETED`
- Reference: `templates/L4/crud/`

### `auth`
- Authenticated post + comment creation; anonymous read-only allowed where override applies
- **Rate-limit at endpoint level** — `POST /api/posts` and `POST /api/posts/{id}/comments`
  capped per user per minute per ASVS-V2.2.1 anti-automation anchor
- Idempotency key required on POST per `practices/rules/idempotency-key-on-mutations.md`
- Reference: `templates/L4/auth/`

### `notification`
- Subscribe to `ReplyCreatedEvent` (comment posted on watched thread)
- Honor `NotificationPreferences` (NOTIF-PREF-001) — opt-out and per-channel toggle
- Channels: email (via email-outbox), in-app push (via notification-bell L2)
- Templates: `reply_created`, `post_hidden_by_moderator`, `mention_received`
- Reference: `templates/L4/notification/`

### `audit-log`
- Annotate moderation service with `@Audited(action = "post.status.changed")`
- Records: `operator_id`, `target_id`, `before_status`, `after_status`, `at`
- Retention: ≥90 days per `specs/audit-log-l0.yaml`
- Reference: `templates/L4/audit-log/`

### `search`
- Full-text index over `Post.title` + `Post.body` + `Comment.body`
- Soft-deleted filter applied at index-time AND query-time (defense-in-depth)
- Authorization-aware — query-builder injects `deleted_at IS NULL` + visibility scope
- Reference: `templates/L4/search/`

### co-shipped — `community-html-sanitization` (INV-005)

This recipe-level invariant is NOT a new `practices/rules/*.md` file (PRD §1.8 +
§10). It is authored inline in `specs/recipes/community-recipe-l0.yaml` and
asserted by `frontend/tests/recipes/community-sanitize.spec.ts`. Server-side
sanitize is the canonical defense — see `recipes/community/RECIPE.md` evidence
block for the Discourse / Reddit-archive verbatim that anchors the
community-content-storage pattern.

## Applied Recipe Annotation

Every L4 domain wired under this recipe **must** declare in its README.md:
```
applied_recipes:
  - community
```
(Enforced by rule `business-domain-must-declare-applied-recipe` — SP37/R6 dual-form guard)

The 5 affected L4 READMEs (alphabetical: `audit-log`, `auth`, `crud`,
`notification`, `search`) append `community` to their existing
`applied_recipes:` plural list in this same atomic SP41b commit.
