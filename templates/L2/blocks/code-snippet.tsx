"use client";
/*
---
template_id: L2/blocks/code-snippet
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 code snippet (showcase token system) codified to fill the code/terminal gap the persona/role UI/UX audit surfaced — the persona-driven showcase had no code surface for the developer persona (the L1 code-block uses the L1 --color-* token system, undefined under the showcase shadcn tokens). Semantic <pre><code> for WCAG 1.3.1 programmatic code semantics; copy is a native <button> (keyboard operable). Copy success is announced through a SINGLE channel — the focused button's accessible name changes Copy->Copied (the decorative check glyph is aria-hidden so the name stays clean and WCAG 2.5.3 holds) — deliberately avoiding a redundant second aria-live region that would double-announce. The reset timer is held in a ref, cleared on re-copy and on unmount (no leaked timer / no early revert on rapid re-copy). Self-contained (no @/ alias, no clsx). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import { useCallback, useEffect, useRef, useState } from "react";

export interface CodeSnippetProps {
  code: string;
  language?: string;
  filename?: string;
}

// A semantic <pre><code> surface with a copy button. The button is a native control (keyboard
// operable); copy success is announced by the focused button's accessible-name change (Copy->Copied),
// a single channel. The 2s reset timer is cleared on re-copy and on unmount.
export function CodeSnippet({ code, language, filename }: CodeSnippetProps) {
  const [copied, setCopied] = useState(false);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => () => {
    if (timer.current) clearTimeout(timer.current);
  }, []);

  const onCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      if (timer.current) clearTimeout(timer.current);
      timer.current = setTimeout(() => setCopied(false), 2000);
    } catch {
      // clipboard unavailable (non-HTTPS / old browser) — no-op
    }
  }, [code]);

  return (
    <div className="group relative w-full overflow-hidden rounded-lg border bg-muted/40 text-sm">
      <div className="flex items-center justify-between border-b px-3 py-1.5">
        <span className="truncate font-mono text-xs text-muted-foreground">{filename ?? language ?? "code"}</span>
        <button
          type="button"
          onClick={onCopy}
          className={[
            "rounded-md border px-2 py-0.5 text-xs font-medium",
            "text-muted-foreground transition-colors",
            "hover:bg-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring",
            copied ? "text-[var(--ax-status-success-fg)]" : "",
          ]
            .filter(Boolean)
            .join(" ")}
        >
          {copied ? (
            <>
              <span aria-hidden="true">✓ </span>Copied
            </>
          ) : (
            "Copy"
          )}
        </button>
      </div>
      <div className="overflow-x-auto">
        <pre className="px-3 py-3 leading-relaxed">
          <code data-language={language} className="block font-mono">
            {code}
          </code>
        </pre>
      </div>
    </div>
  );
}

export default CodeSnippet;
