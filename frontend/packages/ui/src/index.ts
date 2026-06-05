// @ax/ui — the ax-template design-system kit (the SHARED component catalog).
//
// Every per-persona app under frontend/apps/** MUST consume its UI primitives
// from this barrel. Defining app-local copies (a local components/ui/** or a
// component named Button/Input/Card/etc.) is mechanically blocked by the
// ax/no-app-local-ui-primitives ESLint rule. This barrel is the single source
// of truth for the primitives + the `cn` class-merge helper.

export { cn } from './utils';

export { Alert, alertVariants } from './alert';
export { Badge, badgeVariants } from './badge';
export { Button, buttonVariants } from './button';
export type { ButtonProps } from './button';
export { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from './card';
export {
  Dialog,
  DialogTrigger,
  DialogClose,
  DialogPortal,
  DialogOverlay,
  DialogContent,
  DialogHeader,
  DialogFooter,
  DialogTitle,
  DialogDescription,
} from './dialog';
export type { DialogContentProps } from './dialog';
export { ConfirmDialog } from './confirm-dialog';
export type { ConfirmDialogProps } from './confirm-dialog';
export { Field } from './field';
export { Input } from './input';
export { Label } from './label';
export { Spinner } from './spinner';
export { Switch } from './switch';
