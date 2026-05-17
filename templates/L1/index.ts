// L1 component barrel — 39 components (32 shadcn/ui + 7 SP14 P0 primitives)
// Fork receivers: copy this directory and run `npm install` with PEER_DEPS.md

// Form primitives (10)
export * from './components/button'
export * from './components/input'
export * from './components/textarea'
export * from './components/label'
export * from './components/select'
export * from './components/checkbox'
export * from './components/radio-group'
export * from './components/switch'
export * from './components/slider'
export * from './components/form'

// Display primitives (8)
export * from './components/card'
export * from './components/badge'
export * from './components/avatar'
export * from './components/separator'
export * from './components/skeleton'
export * from './components/progress'
export * from './components/aspect-ratio'
export * from './components/scroll-area'

// Layout primitives (4)
export * from './components/tabs'
export * from './components/accordion'
export * from './components/collapsible'
export * from './components/resizable'

// Overlay primitives (6)
export * from './components/dialog'
export * from './components/alert-dialog'
export * from './components/popover'
export * from './components/tooltip'
export * from './components/hover-card'
export * from './components/sheet'

// Feedback primitives (4)
export * from './components/sonner'
export * from './components/alert'
export * from './components/command'
export * from './components/dropdown-menu'

// SP14 P0 Primitives (7) — combobox, date cluster, file/otp/address
export * from './components/combobox'
export * from './components/calendar'
export * from './components/date-picker'
export * from './components/date-range-picker'
export * from './components/file-dropzone'
export * from './components/otp-input'
export * from './components/address-search'

// Utilities
export { cn } from './lib/utils'
