"use client";
/*
---
template_id: L2/blocks/avatar-group
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 avatar group (showcase token system) codified to fill the avatar gap the persona/role UI/UX audit surfaced — the persona-driven showcase had no avatar primitive (the L1 avatar is Radix-based and uses the L1 --color-* token system, undefined under the showcase shadcn tokens). Image with initials fallback, overlapping stack with a +N overflow chip. WCAG 1.1.1/4.1.2: each avatar is a role='img' with aria-label={member name} so the FULL name (not just the initials) reaches assistive tech in BOTH the photo and the fallback path; the image is decorative (alt='') and falls back to the initials on load error (onError); initials are code-point-safe (Array.from) so non-ASCII/emoji names don't tofu. The +N chip carries aria-label so screen readers get '{n} more', not a bare '+n'. Self-contained (no @/ alias, no Radix). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import { useEffect, useState } from "react";

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

// First grapheme of the first and last word, code-point safe (Array.from iterates by code point, so
// emoji / astral-plane names produce a whole character or fall through to "?").
function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const head = (s?: string) => (s ? Array.from(s)[0] ?? "" : "");
  const first = head(parts[0]);
  const last = parts.length > 1 ? head(parts[parts.length - 1]) : "";
  return (first + last).toUpperCase() || "?";
}

const RING = "ring-2 ring-background";
const CHIP = [
  "relative inline-flex h-9 w-9 items-center justify-center overflow-hidden rounded-full",
  "bg-muted text-xs font-medium text-muted-foreground",
  RING,
].join(" ");

// One avatar: role="img" + aria-label exposes the member name once; the initials and the photo are
// decorative. The photo is shown when present and renders the initials underneath on load failure.
function Avatar({ member }: { member: AvatarMember }) {
  const [failed, setFailed] = useState(false);
  // a changed src (e.g. this list slot now holds a different member) gets a fresh chance to load,
  // so a prior member's load error never suppresses a new member's valid photo at the same index.
  useEffect(() => setFailed(false), [member.src]);
  const showImg = Boolean(member.src) && !failed;
  return (
    <span role="img" aria-label={member.name.trim() || "Member"} className={CHIP}>
      <span aria-hidden="true">{initials(member.name)}</span>
      {showImg ? (
        <img
          src={member.src}
          alt=""
          aria-hidden="true"
          onError={() => setFailed(true)}
          className="absolute inset-0 h-full w-full object-cover"
        />
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
          <span role="img" aria-label={`${overflow} more`} className={CHIP}>
            <span aria-hidden="true">+{overflow}</span>
          </span>
        </li>
      ) : null}
    </ul>
  );
}

export default AvatarGroup;
