---
snapshot_id: wcag-2-2
source: "https://www.w3.org/TR/WCAG22/"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "WCAG 2.2 (W3C Recommendation, 2023-10-05)"
via: WebFetch
sha: "a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0"
---

# WCAG 2.2 — Key Accessibility Success Criteria

Source: https://www.w3.org/TR/WCAG22/  
Version: W3C Recommendation 2023-10-05  
Fetched: 2026-05-17

## SC 1.4.3 — Contrast (Minimum) — Level AA

> "The visual presentation of text and images of text has a contrast ratio of at least 4.5:1, except for the following:
> Large Text: Large-scale text and images of large-scale text have a contrast ratio of at least 3:1;
> Incidental: Text or images of text that are part of an inactive user interface component, that are pure decoration, that are not visible to anyone, or that are part of a picture that contains significant other visual content, have no contrast requirement.
> Logotypes: Text that is part of a logo or brand name has no contrast requirement."

**Minimum ratio: 4.5:1** for normal text. **3:1** for large text (≥ 18pt or 14pt bold).

## SC 1.4.11 — Non-text Contrast — Level AA

> "The visual presentation of the following have a contrast ratio of at least 3:1 against adjacent color(s):
> User Interface Components: Visual information required to identify user interface components and states, except for inactive components or where the appearance of the component is determined by the user agent and not modified by the author;
> Graphical Objects: Parts of graphics required to understand the content, except when a particular presentation of graphics is essential to the information being conveyed."

**Minimum ratio: 3:1** for UI components (buttons, inputs, checkboxes) and meaningful graphics.

## SC 2.5.3 — Label in Name — Level A

> "For user interface components with labels that include text or images of text, the name contains the text that is presented visually."

Ensures voice control users can activate components using visible label text.

## SC 2.4.7 — Focus Visible — Level AA

Focus indicator must be visible. Applies to all keyboard-focusable components.

## Application to ax-template

- `blueprints/<domain>-ui-manifest.yaml` MUST carry `a11y.contrast_min: 4.5` (enforces SC 1.4.3).
- `trio_integrity_guard.sh` validates the field exists and is ≥ 4.5.
- L1 shadcn/ui components are accessible-by-default but must be audited with axe-core in CI.
- `blueprints/<domain>-ui-manifest.yaml` carries `a11y.axe_rules` list.
