# L1 Peer Dependencies

When you fork ax-template and copy `templates/L1/` into your project, install these packages.

Updated SP14: +5 packages for 7 P0 primitives (combobox, date-picker, calendar,
date-range-picker, file-dropzone, otp-input, address-search).

## Install Command

```bash
npm install \
  @radix-ui/react-accordion@^1.2.0 \
  @radix-ui/react-alert-dialog@^1.1.0 \
  @radix-ui/react-aspect-ratio@^1.1.0 \
  @radix-ui/react-avatar@^1.1.0 \
  @radix-ui/react-checkbox@^1.1.0 \
  @radix-ui/react-collapsible@^1.1.0 \
  @radix-ui/react-dialog@^1.1.0 \
  @radix-ui/react-dropdown-menu@^2.1.0 \
  @radix-ui/react-hover-card@^1.1.0 \
  @radix-ui/react-label@^2.1.0 \
  @radix-ui/react-popover@^1.1.0 \
  @radix-ui/react-progress@^1.1.0 \
  @radix-ui/react-radio-group@^1.2.0 \
  @radix-ui/react-scroll-area@^1.1.0 \
  @radix-ui/react-select@^2.1.0 \
  @radix-ui/react-separator@^1.1.0 \
  @radix-ui/react-slider@^1.2.0 \
  @radix-ui/react-slot@^1.1.0 \
  @radix-ui/react-switch@^1.1.0 \
  @radix-ui/react-tabs@^1.1.0 \
  @radix-ui/react-tooltip@^1.1.0 \
  class-variance-authority@^0.7.0 \
  clsx@^2.1.0 \
  cmdk@^1.0.0 \
  date-fns@^3.0.0 \
  input-otp@^1.2.0 \
  lucide-react@^0.400.0 \
  react-day-picker@^9.0.0 \
  react-dropzone@^14.0.0 \
  react-hook-form@^7.53.0 \
  react-resizable-panels@^2.1.0 \
  sonner@^1.7.0 \
  tailwind-merge@^2.3.0
```

## For `_stories/` (Playwright test templates)

If you copy `_stories/` to your project's test directory, also install:

```bash
npm install --save-dev @playwright/test
```

`_stories/` contains template Playwright specs that are excluded from the component
library's TypeScript compilation. They require a running dev server on `localhost:3000`.

## Package Table

| Package | Version | Components |
|---------|---------|------------|
| `@radix-ui/react-accordion` | `^1.2.0` | Accordion |
| `@radix-ui/react-alert-dialog` | `^1.1.0` | AlertDialog |
| `@radix-ui/react-aspect-ratio` | `^1.1.0` | AspectRatio |
| `@radix-ui/react-avatar` | `^1.1.0` | Avatar |
| `@radix-ui/react-checkbox` | `^1.1.0` | Checkbox |
| `@radix-ui/react-collapsible` | `^1.1.0` | Collapsible |
| `@radix-ui/react-dialog` | `^1.1.0` | Dialog, Sheet |
| `@radix-ui/react-dropdown-menu` | `^2.1.0` | DropdownMenu |
| `@radix-ui/react-hover-card` | `^1.1.0` | HoverCard |
| `@radix-ui/react-label` | `^2.1.0` | Label |
| `@radix-ui/react-popover` | `^1.1.0` | Popover |
| `@radix-ui/react-progress` | `^1.1.0` | Progress |
| `@radix-ui/react-radio-group` | `^1.2.0` | RadioGroup |
| `@radix-ui/react-scroll-area` | `^1.1.0` | ScrollArea |
| `@radix-ui/react-select` | `^2.1.0` | Select |
| `@radix-ui/react-separator` | `^1.1.0` | Separator |
| `@radix-ui/react-slider` | `^1.2.0` | Slider |
| `@radix-ui/react-slot` | `^1.1.0` | Button (asChild) |
| `@radix-ui/react-switch` | `^1.1.0` | Switch |
| `@radix-ui/react-tabs` | `^1.1.0` | Tabs |
| `@radix-ui/react-tooltip` | `^1.1.0` | Tooltip |
| `class-variance-authority` | `^0.7.0` | Button, Badge, Alert, Sheet |
| `clsx` | `^2.1.0` | cn() utility |
| `cmdk` | `^1.0.0` | Command |
| `date-fns` | `^3.0.0` | DatePicker, DateRangePicker |
| `input-otp` | `^1.2.0` | OtpInput |
| `lucide-react` | `^0.400.0` | All SP14 components (icons) |
| `react-day-picker` | `^9.0.0` | Calendar, DatePicker, DateRangePicker |
| `react-dropzone` | `^14.0.0` | FileDropzone |
| `react-hook-form` | `^7.53.0` | Form |
| `react-resizable-panels` | `^2.1.0` | Resizable |
| `sonner` | `^1.7.0` | Toaster |
| `tailwind-merge` | `^2.3.0` | cn() utility |

## Tailwind CSS

L1 components use CSS custom properties (design tokens) from `blueprints/ui-tokens-manifest.yaml`. Configure your project to define these variables in your root CSS. See `blueprints/ui-tokens-manifest.yaml` for the full token schema.

Tailwind CSS v3+ is required for utility classes and animation directives (`animate-in`, `animate-out`, `fade-in-0`, etc.).
