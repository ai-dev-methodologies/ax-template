---
snapshot_id: shadcn-ui-2026-05
source: "https://ui.shadcn.com/docs"
fetched_at: "2026-05-17T13:00:00Z"
version_observed: "shadcn-ui@2026-05"
via: WebFetch
sha: "d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7"
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
