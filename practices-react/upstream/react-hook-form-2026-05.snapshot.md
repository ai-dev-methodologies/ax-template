# React Hook Form — Snapshot (2026-05)

**Source:** https://react-hook-form.com/docs/useform
**Fetched at:** 2026-05-18
**Via:** WebFetch
**Snapshot ID:** react-hook-form-2026-05

---

## Overview

React Hook Form (RHF) is the canonical form library for React. It uses uncontrolled inputs and a subscription model to minimize re-renders.

> "Performant, flexible and extensible forms with easy-to-use validation."
> — react-hook-form.com

Current stable version: **7.x** (compatible with React 18/19).

---

## Core API — useForm

```typescript
const {
  register,
  handleSubmit,
  watch,
  formState: { errors, isDirty, isSubmitting, submitCount },
  control,
  reset,
  setValue,
  getValues,
} = useForm<TFieldValues>({
  resolver: zodResolver(schema),   // Zod integration (recommended)
  defaultValues: {},
  mode: 'onBlur',                 // validate on blur (default: onSubmit)
})
```

---

## FormProvider + useFormContext

For deeply nested components, wrap the form with `FormProvider` and read context with `useFormContext`:

```typescript
<FormProvider {...form}>
  <form onSubmit={form.handleSubmit(onSubmit)}>
    <NestedInput />
  </form>
</FormProvider>

// Inside NestedInput:
function NestedInput() {
  const { register } = useFormContext()
  return <input {...register('name')} />
}
```

> "FormProvider / useFormContext allows consuming RHF context without prop drilling."
> — RHF Docs, Advanced Usage / FormProvider

---

## useFieldArray

Dynamic list of repeating fields:

```typescript
const { fields, append, remove, prepend, move } = useFieldArray({
  control,
  name: 'addresses',
})
```

> "useFieldArray provides tools to manage a list of fields within your form."
> — RHF Docs, useFieldArray

Each field has a stable `id` (UUID) from RHF — use `field.id` as React key, NOT `index`.

---

## useWatch

Subscribe to specific field values without re-rendering the whole form:

```typescript
const watchedValue = useWatch({
  control,
  name: 'country',
  defaultValue: '',
})
```

> "useWatch subscribes to specific field changes, minimizing re-renders compared to form.watch()."
> — RHF Docs, useWatch

Use for conditional fields and dependent fields.

---

## useController

Full control over an input: access `field` (ref, value, onChange, onBlur) and `fieldState` (error, isDirty, isTouched):

```typescript
const { field, fieldState: { error } } = useController({
  control,
  name: 'email',
  rules: { required: 'Email is required' },
})
```

Preferred over `register` for custom input components.

---

## Zod Integration (zodResolver)

```typescript
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'

const schema = z.object({
  email: z.string().email(),
  age: z.number().int().min(18),
  addresses: z.array(z.object({ street: z.string(), city: z.string() })).min(1),
})

const form = useForm<z.infer<typeof schema>>({
  resolver: zodResolver(schema),
})
```

---

## formState.isDirty

> "`isDirty` is set to `true` when any field differs from its `defaultValues`. It's reactive and updates on every field change."
> — RHF Docs, formState

```typescript
const { formState: { isDirty } } = useForm()
// isDirty: false on mount, true after first field change, false after reset()
```

Used by `DirtyGuard` to intercept navigation.

---

## Auto-Save Pattern

Recommended pattern for debounced auto-save:

```typescript
// Watch all values (or specific fields)
const values = useWatch({ control })

useEffect(() => {
  const timer = setTimeout(() => saveFn(getValues()), 1000)
  return () => clearTimeout(timer)
}, [JSON.stringify(values)])  // serialize to detect actual changes
```

---

## Error Summary Pattern

Flatten `formState.errors` recursively to build a summary:

```typescript
// formState.errors is a nested object matching the schema shape
// Recursive flatten needed for arrays and nested objects
function flattenErrors(errors, prefix = '') {
  return Object.entries(errors).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    if (value?.message) return [{ path, message: value.message }]
    if (typeof value === 'object') return flattenErrors(value, path)
    return []
  })
}
```

---

## References

- RHF Documentation: https://react-hook-form.com/docs/useform
- useFieldArray: https://react-hook-form.com/docs/usefieldarray
- useWatch: https://react-hook-form.com/docs/usewatch
- useController: https://react-hook-form.com/docs/usecontroller
- FormProvider: https://react-hook-form.com/docs/formprovider
- Zod resolver: https://github.com/react-hook-form/resolvers#zod
