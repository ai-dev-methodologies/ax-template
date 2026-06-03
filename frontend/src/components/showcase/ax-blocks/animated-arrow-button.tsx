"use client";
/*
---
template_id: L2/blocks/animated-arrow-button
layer: L2
provenance_class: internal_design
evidence:
  - source_type: internal
    rationale: "Codified from the community 21st.dev component nextjsshop/animated-arrow-button: hardcoded hex extracted to --ax-c-* design tokens, provenance-stamped, verified to pass all 7 ax/* own-block rules (gen_verify T1-T4). Governed by practices-react/rules/ux-block-uses-design-tokens-and-a11y.md."
dependencies: []
imports_from: []
imports_forbidden: [L4, app/, lib/]
---
*/
/**
 * @ax-codified-from 21st.dev/nextjsshop/animated-arrow-button
 * @ax-layer L2/blocks/button
 * Deterministic codify (codify.py): hex extracted to --ax-c-* tokens; provenance stamped.
 * REMAINING semantic pass (see .notes.md): a11y:add-role-aria, types:string-literal-variants
 */
import React from 'react';

export const Button04 = ({ text = "Nothing-Plop" }) => {
  // Define dot indices for the two icon variations
  const firstIconDots = [0, 2, 2, 1, 2, 0, 1, 1, 2, 2, 0, 1, 0, 2, 2, 1, 0, 2, 2, 2, 2, 0, 1, 0, 2];
  const secondIconDots = [0, 2, 2, 1, 2, 0, 1, 1, 2];

  return (
    <a href="#" className="button04 w-inline-block">
      <span className="button04_bg"></span>
      <span data-text={text} className="button04_inner">
        <span className="button04_text">{text}</span>
        <span className="button04_icon-wrap">
          <span 
            style={{ '--index-parent': 0 } as React.CSSProperties} 
            className="button04_icon"
          >
            {firstIconDots.map((index, i) => (
              <span
                key={`first-dot-${i}`}
                style={{ '--index': index }as React.CSSProperties}
                className="button04_dot"
              ></span>
            ))}
          </span>
          <span 
            style={{ '--index-parent': 1 }as React.CSSProperties} 
            className="button04_icon is-arrow"
          >
            {secondIconDots.map((index, i) => (
              <span
                key={`second-dot-${i}`}
                style={{ '--index': index }as React.CSSProperties}
                className="button04_dot"
              ></span>
            ))}
          </span>
        </span>
      </span>
    </a>
  );
};
