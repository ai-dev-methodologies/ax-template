# shadcn/ui — Overview + Frozen L1 Component Descriptions

**Source URL(s):** https://ui.shadcn.com/docs (overview); https://ui.shadcn.com/docs/components/{accordion..tooltip} (36 per-component pages — full list in practices/upstream/_FETCH-RECEIPTS.yaml)
**HTTP status:** 200 (all 37 URLs)
**Fetched at:** 2026-07-30T00:51:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh <url>` (run once per URL above; PRD-final-4 W1)
**Body SHA-256 (below the `---` divider, header excluded):** 5b5dfa2f1d4703c62bc43a3683502fb24de78b8314ed995e9363001670f7fbbe

---

# shadcn/ui — Overview (2026-05 snapshot)

Source: https://ui.shadcn.com/docs  
Fetched: 2026-05-17

## What it is

> "This is not a component library. It is how you build your component library."

shadcn/ui provides "a set of beautifully-designed, accessible components and a code distribution platform."
The key difference from traditional libraries: developers receive the **actual component source code**,
enabling direct modification without wrapping or style overrides.

## Core Principles

1. **Open Code** — Full transparency and control over component implementation
2. **Composition** — Standardized, predictable interfaces across all components
3. **Distribution** — Schema-based CLI system for sharing components across projects
4. **Beautiful Defaults** — Pre-styled components that work cohesively without customization
5. **AI-Ready** — Code structure designed for language models to read and understand

## Installation Model

The copy-paste / `shadcn add` model means components become part of your codebase, not a
dependency. Run `npx shadcn@latest add <component>` to add a component; it writes directly
to your `components/ui/` directory.

## Why This Matters for ax-template

- Components under `templates/L1/` are frozen copies of the 32 blessed components.
- Updates require explicit re-copying; this is intentional (zero surprise upgrades).
- `time_decay_guard.sh` flags when the frozen registry snapshot is > 90 days old.

## Available Component Categories (2026-05)

Accordion, Alert, Alert Dialog, Aspect Ratio, Avatar, Badge, Breadcrumb, Button,
Calendar, Card, Carousel, Chart, Checkbox, Collapsible, Combobox, Command,
Context Menu, Data Table, Date Picker, Dialog, Drawer, Dropdown Menu,
Hover Card, Input, Input OTP, Label, Menubar, Navigation Menu, Pagination,
Popover, Progress, Radio Group, Resizable, Scroll Area, Select, Separator,
Sheet, Sidebar, Skeleton, Slider, Sonner, Switch, Table, Tabs, Textarea,
Toast, Toggle, Toggle Group, Tooltip

## Frozen L1 Component Descriptions (curl+extractor refresh, 2026-07-30)

The per-component sections below were fetched fresh via `practices/scripts/snapshot-extract.sh` against each component's live `https://ui.shadcn.com/docs/components/<slug>` page (PRD-final-4 W1). Each heading is the lowercase slug cited by the matching `templates/L1/components/*.tsx` evidence block's `section:` field.

### accordion

Source: https://ui.shadcn.com/docs/components/accordion

A vertically stacked set of interactive headings that each reveal a section of content.

### alert

Source: https://ui.shadcn.com/docs/components/alert

Displays a callout for user attention.

### alert-dialog

Source: https://ui.shadcn.com/docs/components/alert-dialog

A modal dialog that interrupts the user with important content and expects a response.

### aspect-ratio

Source: https://ui.shadcn.com/docs/components/aspect-ratio

Displays content within a desired ratio.

### avatar

Source: https://ui.shadcn.com/docs/components/avatar

An image element with a fallback for representing the user.

### badge

Source: https://ui.shadcn.com/docs/components/badge

Displays a badge or a component that looks like a badge.

### button

Source: https://ui.shadcn.com/docs/components/button

Displays a button or a component that looks like a button.

### calendar

Source: https://ui.shadcn.com/docs/components/calendar

A calendar component that allows users to select a date or a range of dates.

### card

Source: https://ui.shadcn.com/docs/components/card

Displays a card with header, content, and footer.

### checkbox

Source: https://ui.shadcn.com/docs/components/checkbox

A control that allows the user to toggle between checked and not checked.

### collapsible

Source: https://ui.shadcn.com/docs/components/collapsible

An interactive component which expands/collapses a panel.

### combobox

Source: https://ui.shadcn.com/docs/components/combobox

Autocomplete input with a list of suggestions.

### command

Source: https://ui.shadcn.com/docs/components/command

Command menu for search and quick actions.

### date-picker

Source: https://ui.shadcn.com/docs/components/date-picker

A date picker component with range and presets.

### dialog

Source: https://ui.shadcn.com/docs/components/dialog

A window overlaid on either the primary window or another dialog window, rendering the content underneath inert.

### dropdown-menu

Source: https://ui.shadcn.com/docs/components/dropdown-menu

Displays a menu to the user — such as a set of actions or functions — triggered by a button.

### form

Source: https://ui.shadcn.com/docs/components/form

Build forms with React and shadcn/ui.

### hover-card

Source: https://ui.shadcn.com/docs/components/hover-card

For sighted users to preview content available behind a link.

### input

Source: https://ui.shadcn.com/docs/components/input

A text input component for forms and user data entry with built-in styling and accessibility features.

### input-otp

Source: https://ui.shadcn.com/docs/components/input-otp

Accessible one-time password component with copy-paste functionality.

### label

Source: https://ui.shadcn.com/docs/components/label

Renders an accessible label associated with controls.

### popover

Source: https://ui.shadcn.com/docs/components/popover

Displays rich content in a portal, triggered by a button.

### progress

Source: https://ui.shadcn.com/docs/components/progress

Displays an indicator showing the completion progress of a task, typically displayed as a progress bar.

### radio-group

Source: https://ui.shadcn.com/docs/components/radio-group

A set of checkable buttons—known as radio buttons—where no more than one of the buttons can be checked at a time.

### resizable

Source: https://ui.shadcn.com/docs/components/resizable

Accessible resizable panel groups and layouts with keyboard support.

### scroll-area

Source: https://ui.shadcn.com/docs/components/scroll-area

Augments native scroll functionality for custom, cross-browser styling.

### select

Source: https://ui.shadcn.com/docs/components/select

Displays a list of options for the user to pick from—triggered by a button.

### separator

Source: https://ui.shadcn.com/docs/components/separator

Visually or semantically separates content.

### sheet

Source: https://ui.shadcn.com/docs/components/sheet

Extends the Dialog component to display content that complements the main content of the screen.

### skeleton

Source: https://ui.shadcn.com/docs/components/skeleton

Use to show a placeholder while content is loading.

### slider

Source: https://ui.shadcn.com/docs/components/slider

An input where the user selects a value from within a given range.

### sonner

Source: https://ui.shadcn.com/docs/components/sonner

A succinct message that is displayed temporarily.

### switch

Source: https://ui.shadcn.com/docs/components/switch

A control that allows the user to toggle between checked and not checked.

### tabs

Source: https://ui.shadcn.com/docs/components/tabs

A set of layered sections of content—known as tab panels—that are displayed one at a time.

### textarea

Source: https://ui.shadcn.com/docs/components/textarea

Displays a form textarea or a component that looks like a textarea.

### tooltip

Source: https://ui.shadcn.com/docs/components/tooltip

A popup that displays information related to an element when the element receives keyboard focus or the mouse hovers over it.
