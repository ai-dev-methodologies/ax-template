"use client";
/*
---
template_id: L2/blocks/activity-feed
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 social activity feed, codified to fill the feed gap the per-persona consumer app surfaced — the catalog had AvatarGroup + StatusBadge but no list block that renders an actor/verb/object timeline with per-item read state. Presentational only (data + callbacks injected by the consuming app; no fetch, no store, no @/ alias). Colors/radii come exclusively from the host theme's design tokens (--ax-status-*, --radius, the shadcn --foreground/--muted/--border surface vars), so a persona that overrides those tokens (e.g. .ax-consumer raising --radius + accent saturation) re-skins every row with no block edit. A11y: the feed is an aria-label list (role defaults to list via <ul>); each row is an <li>; the unread dot is aria-hidden and the unread state is also surfaced as text (aria-label) so it is not color-only (WCAG 1.4.1); the mark-read control is a real <button> with an accessible name. Motion: a single staggered entrance per row via CSS animation-delay, fully disabled under prefers-reduced-motion by the consuming app's reduce block — the block itself sets only transform/opacity (compositor-friendly) and never animates layout. Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md (spec REACT-PRACTICES-UX-001)."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import type { ReactNode } from "react";

/** One feed row. The consuming app maps its domain DTO to this shape. */
export interface ActivityFeedItem {
  id: string;
  /** Display name of the actor (already resolved by the app). */
  actorName: string;
  /** Optional avatar url for the actor. */
  actorAvatarUrl?: string;
  /** The action, e.g. "님이 게시물을 올렸어요". Pre-localized by the app. */
  verbText: string;
  /** Optional primary content / preview line. */
  preview?: ReactNode;
  /** Pre-formatted relative time, e.g. "3분 전". */
  timeText: string;
  /** Unread when true — surfaced as a dot AND as text for non-color signalling. */
  unread?: boolean;
}

export interface ActivityFeedProps {
  items: ActivityFeedItem[];
  /** aria-label for the list landmark. */
  label?: string;
  /** Invoked when a row body is activated (e.g. open detail). */
  onSelect?: (id: string) => void;
  /** When provided, an unread row shows a "읽음 처리" control. */
  onMarkRead?: (id: string) => void;
  /** Disable the per-row mark-read control while a mutation is in flight. */
  markReadPendingId?: string | null;
  className?: string;
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const head = (s?: string): string => (s ? Array.from(s)[0] ?? "" : "");
  const a = head(parts[0]);
  const b = parts.length > 1 ? head(parts[parts.length - 1]) : "";
  return (a + b).toUpperCase() || "?";
}

/**
 * Presentational social activity feed. The host app owns data + callbacks; this
 * block owns layout, the read-state affordance, a11y, and the entrance motion.
 */
export function ActivityFeed({
  items,
  label = "활동 피드",
  onSelect,
  onMarkRead,
  markReadPendingId,
  className,
}: ActivityFeedProps) {
  return (
    <ul
      aria-label={label}
      className={["ax-activity-feed flex flex-col gap-3", className].filter(Boolean).join(" ")}
    >
      {items.map((item, index) => {
        const rowBody = (
          <>
            <span
              aria-hidden="true"
              className="ax-activity-feed__avatar relative grid h-11 w-11 shrink-0 place-items-center overflow-hidden rounded-full bg-secondary text-sm font-semibold text-secondary-foreground"
            >
              {initials(item.actorName)}
              {item.actorAvatarUrl ? (
                <img
                  src={item.actorAvatarUrl}
                  alt=""
                  className="absolute inset-0 h-full w-full object-cover"
                />
              ) : null}
            </span>
            <span className="min-w-0 flex-1">
              <span className="block text-sm leading-snug text-foreground">
                <span className="font-semibold">{item.actorName}</span>{" "}
                <span className="text-muted-foreground">{item.verbText}</span>
              </span>
              {item.preview ? (
                <span className="mt-1 block truncate text-sm text-foreground/90">{item.preview}</span>
              ) : null}
              <span className="mt-1 block text-xs text-muted-foreground">{item.timeText}</span>
            </span>
          </>
        );

        return (
          <li
            key={item.id}
            className="ax-activity-feed__row"
            style={{ animationDelay: `${Math.min(index, 8) * 45}ms` }}
          >
            <div className="flex items-start gap-3 rounded-[var(--radius)] border border-border bg-card p-4 shadow-sm transition-transform duration-200 ease-out hover:-translate-y-0.5">
              {onSelect ? (
                <button
                  type="button"
                  onClick={() => onSelect(item.id)}
                  className="flex min-w-0 flex-1 items-start gap-3 rounded-[var(--radius)] text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card"
                >
                  {rowBody}
                </button>
              ) : (
                <div className="flex min-w-0 flex-1 items-start gap-3">{rowBody}</div>
              )}

              <span className="flex shrink-0 flex-col items-end gap-2">
                {item.unread ? (
                  <span
                    className="inline-flex items-center gap-1.5 rounded-full px-2 py-0.5 text-[0.7rem] font-medium"
                    style={{
                      color: "var(--ax-status-accent-fg)",
                      background: "var(--ax-status-accent-bg)",
                    }}
                  >
                    <span aria-hidden="true" className="h-1.5 w-1.5 rounded-full bg-current" />
                    안 읽음
                  </span>
                ) : null}
                {item.unread && onMarkRead ? (
                  <button
                    type="button"
                    onClick={() => onMarkRead(item.id)}
                    disabled={markReadPendingId === item.id}
                    className="rounded-[var(--radius)] px-2 py-1 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-card disabled:opacity-50"
                  >
                    {markReadPendingId === item.id ? "처리 중" : "읽음 처리"}
                  </button>
                ) : null}
              </span>
            </div>
          </li>
        );
      })}
    </ul>
  );
}

export default ActivityFeed;
