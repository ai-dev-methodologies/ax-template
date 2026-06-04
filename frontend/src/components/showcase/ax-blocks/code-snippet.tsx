"use client";
/*
---
template_id: L2/blocks/code-snippet
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "ax-native L2 code snippet (showcase token system) codified to fill the code/terminal gap the persona/role UI/UX audit surfaced — the persona-driven showcase had no code surface for the developer persona (the L1 code-block uses the L1 --color-* token system, undefined under the showcase shadcn tokens). Semantic <pre><code> for WCAG 1.3.1 programmatic code semantics; copy button is keyboard-operable (native <button>) and announces success through an aria-live region (WCAG 4.1.3). Self-contained (no @/ alias, no clsx). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
import { useCallback, useRef, useState } from "react";

export interface CodeSnippetProps {
  code: string;
  language?: string;
  filename?: string;
}

// A semantic <pre><code> surface with a copy button. The button is a native control (keyboard
// operable) and copy success is announced via a polite aria-live region for screen readers.
export function CodeSnippet({ code, language, filename }: CodeSnippetProps) {
  const [copied, setCopied] = useState(false);
  const liveRef = useRef<HTMLSpanElement>(null);

  const onCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      if (liveRef.current) liveRef.current.textContent = "Copied";
      window.setTimeout(() => {
        setCopied(false);
        if (liveRef.current) liveRef.current.textContent = "";
      }, 2000);
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
          aria-label={copied ? "Copied" : "Copy code"}
        >
          {copied ? "✓ Copied" : "Copy"}
        </button>
      </div>
      <div className="overflow-x-auto">
        <pre className="px-3 py-3 leading-relaxed">
          <code data-language={language} className="block font-mono">
            {code}
          </code>
        </pre>
      </div>
      <span ref={liveRef} role="status" aria-live="polite" aria-atomic="true" className="sr-only" />
    </div>
  );
}

export default CodeSnippet;
