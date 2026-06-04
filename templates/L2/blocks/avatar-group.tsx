/*
---
template_id: L2/blocks/avatar-group
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 avatar group (showcase token system) codified to fill the avatar gap the persona/role UI/UX audit surfaced — the persona-driven showcase had no avatar primitive (the L1 avatar is Radix-based and uses the L1 --color-* token system, undefined under the showcase shadcn tokens). Image with initials fallback, overlapping stack with a +N overflow chip. WCAG 1.1.1: every image carries alt text; decorative overlap is presentational; the group exposes an aria-label. Self-contained (no @/ alias, no Radix). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
export interface AvatarMember {
  name: string;
  src?: string;
}

export interface AvatarGroupProps {
  members: AvatarMember[];
  /** cap the number of visible avatars; the rest collapse into a +N chip */
  max?: number;
  label?: string;
}

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? "";
  const last = parts.length > 1 ? parts[parts.length - 1][0] : "";
  return (first + last).toUpperCase() || "?";
}

const RING = "ring-2 ring-background";

// An avatar with an image and an initials fallback. When `src` is absent (or while it loads) the
// initials render; the image, when present, carries the member name as alt for WCAG 1.1.1.
function Avatar({ member }: { member: AvatarMember }) {
  return (
    <span
      className={[
        "relative inline-flex h-9 w-9 items-center justify-center overflow-hidden rounded-full",
        "bg-muted text-xs font-medium text-muted-foreground",
        RING,
      ].join(" ")}
    >
      <span aria-hidden={member.src ? true : undefined}>{initials(member.name)}</span>
      {member.src ? (
        <img src={member.src} alt={member.name} className="absolute inset-0 h-full w-full object-cover" />
      ) : null}
    </span>
  );
}

// Overlapping avatar stack with a +N overflow chip. The group is a labelled list so assistive tech
// reads it as "N people"; visual overlap is purely presentational.
export function AvatarGroup({ members, max = 4, label = "Members" }: AvatarGroupProps) {
  const visible = members.slice(0, max);
  const overflow = members.length - visible.length;
  return (
    <ul aria-label={label} className="flex items-center -space-x-2">
      {visible.map((m, i) => (
        <li key={i} className="list-none">
          <Avatar member={m} />
        </li>
      ))}
      {overflow > 0 ? (
        <li className="list-none">
          <span
            className={[
              "relative inline-flex h-9 w-9 items-center justify-center rounded-full",
              "bg-muted text-xs font-medium text-muted-foreground",
              RING,
            ].join(" ")}
          >
            +{overflow}
          </span>
        </li>
      ) : null}
    </ul>
  );
}

export default AvatarGroup;
