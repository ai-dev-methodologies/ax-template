# react-hook-form-2026-05 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://react-hook-form.com/docs/useform (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:47:00Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://react-hook-form.com/docs/useform`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r101`
**Body SHA-256 (below the `---` divider, header excluded):** 5f57aa34f50bd477becd60214a7c97a30f450f9475517d18d225ef94f0498181

---

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

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://react-hook-form.com/docs/useform
HTTP status: 200 · extracted bytes: 22171 · sha256: 4896102340023536c2773717b0c0604028867426cdd37e5f81f424dac4a686fb
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r101`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

useForm | React Hook Form Skip to content Home Get Started API TS TS Advanced FAQs Sponsors Resources ▼ Articles Videos Newsletters 3rd Party Bindings Form Builder DevTools Form Builder Releases More useForm React Hook Form's primary hook for form initialization. Menu </> useForm </> register </> unregister </> formState </> watch </> subscribe </> handleSubmit </> reset </> resetField </> resetDefaultValues </> setError </> clearErrors </> setValue </> setValues </> setFocus </> getValues </> getFieldState </> trigger </> control </> Form </> useController </> Controller </> useFormContext </> FormProvider </> useWatch </> Watch </> useFormState </> ErrorMessage </> FormStateSubscribe </> useFieldArray </> FieldArray </> useLens </> createFormControl Select page... register unregister formstate watch subscribe handlesubmit reset resetField resetDefaultValues setError clearErrors setValue setValues setFocus getValues getFieldState trigger control Form </> useForm: UseFormProps useForm is a custom hook for managing forms with ease. It takes one object as an optional argument. The following example demonstrates all of its properties along with their default values. Generic props: Option Description mode Validation strategy for before submission behavior. reValidateMode Validation strategy for after submission behavior. defaultValues Default values for the form; this value will be cached. values Reactive values to update the form values. errors Server returns errors to update form. ⚠ Important: Keep the errors object reference-stable to avoid infinite re-renders. resetOptions Option to reset form state when updating with new form values. criteriaMode Display all validation errors or one at a time. shouldFocusError Enable or disable built-in focus management. delayError Delay error from appearing instantly. validate Form-level validation is limited to built-in validation methods. shouldUseNativeValidation Use browser built-in form constraint API. shouldUnregister Enable and disable input unregister after unmount. progressive Forward validation attributes ( required , min , max , etc.) as native HTML attributes on inputs. Required when using shouldUseNativeValidation so the browser can display built-in validation UI. Also enables progressive enhancement for the Form component. disabled Disable the entire form with all associated inputs. formControl Supply a pre-created form control object (from createFormControl ) instead of letting useForm create one internally. Schema validation props: Option Description resolver Integrates with your preferred schema validation library. context A context object to supply for your schema validation. Props mode: onChange | onBlur | onSubmit | onTouched | all = 'onSubmit' ! React Native: compatible with Controller This option allows you to configure the validation strategy for before a user submits the form. Validation occurs during the onSubmit event, which is triggered by invoking the handleSubmit function. Name Type Description onSubmit string Validation is triggered on the submit event, and inputs attach onChange event listeners to re-validate themselves. onBlur string Validation is triggered on the blur event. onChange string Validation is triggered on the change event for each input, leading to multiple re-renders. Warning: this often comes with a significant impact on performance. onTouched string Validation is initially triggered on the first blur event. After that, it is triggered on every change event. Note: when using with Controller , make sure to wire up onBlur with the render prop. all string Validation is triggered on both blur and change events. reValidateMode: onChange | onBlur | onSubmit = 'onChange' ! React Native: Custom register or using Controller This option allows you to configure the validation strategy for when inputs with errors get re-validated after a user submits the form (the onSubmit event occurs and the handleSubmit function is executed). By default, re-validation occurs during the input change event. Note: Both mode and reValidateMode are reactive — you can update them after form initialization and the new strategy will take effect on subsequent validations. defaultValues: FieldValues | () => Promise<FieldValues> The defaultValues prop populates the entire form with default values. It supports both synchronous and asynchronous assignment of default values. While you can set an input's default value using defaultValue or defaultChecked (as detailed in the official React documentation) , it is recommended to use defaultValues for the entire form. Copy useForm ( { defaultValues : { firstName : '' , lastName : '' } } ) // set default value async useForm ( { defaultValues : async ( ) => fetch ( '/api-endpoint' ) ; } ) RULES You should avoid providing undefined as a default value, as it conflicts with the default state of a controlled component. defaultValues are cached. To reset them, use the reset API. defaultValues will be included in the submission result by default. It's recommended to avoid using custom objects containing prototype methods, such as Moment or Luxon , as defaultValues . There are other options for including form data: // adding a hidden input < input { ... register ( "hidden" , { value : "data" } ) } type = "hidden" / > // include data onSubmit const onSubmit = ( data ) => { const output = { ... data , others : "others" , } } values: FieldValues The values prop will react to changes and update the form values, which is useful when your form needs to be updated by external state or server data. The values prop will overwrite the defaultValues prop, unless resetOptions: { keepDefaultValues: true } is also set for useForm . Copy // set default value sync function App ( { values } ) { useForm ( { values , // will get updated when values props updates } ) } function App ( ) { const values = useFetch ( "/api" ) useForm ( { defaultValues : { firstName : "" , lastName : "" , } , values , // will get updated once values returns } ) } errors: FieldErrors The errors prop will react to changes and update the server errors state, which is useful when your form needs to be updated with errors returned from the server. Copy function App ( ) { const { errors , data } = useFetch ( "/api" ) useForm ( { errors , // will get updated once errors returns } ) } resetOptions: KeepStateOptions This property is related to value update behaviors. When values or defaultValues are updated, the reset API is invoked internally. It's important to specify the desired behavior after values or defaultValues are asynchronously updated. The configuration option itself is a reference to the reset method's options. Copy // by default, an asynchronous update to values or defaultValues will reset the form values useForm ( { values } ) useForm ( { defaultValues : async ( ) => await fetch ( ) } ) // options to configure the behavior // eg: I want to keep user-interacted/dirty values and not remove any user errors useForm ( { values , resetOptions : { keepDirtyValues : true , // user-interacted input will be retained keepErrors : true , // input errors will be retained with value update } , } ) context: object This context object is mutable and will be injected into the resolver 's second argument or Yup validation's context object. CodeSandbox criteriaMode: firstError | all When set to firstError (default), only the first error from each field will be gathered. When set to all , all errors from each field will be gathered. CodeSandbox shouldFocusError: boolean = true When set to true (default), and the user submits a form that fails validation, focus is set on the first field with an error. NOTE Only registered fields with a ref will work. Custom registered inputs do not apply. For example: register('test') // doesn't work The focus order is based on the register order. delayError: number This configuration delays the display of error states to the end user by a specified number of milliseconds. If the user corrects the input with errors, the error is removed instantly, and the delay is not applied. CodeSandbox validate: Function This example demonstrates how to use the new validate API in combination with useForm to perform form-level validation in a React application. The validate function receives the entire form object and allows you to return a structured error that integrates with formState.errors . Examples: Copy const { register , formState : { errors } , } = useForm ( { validate : async ( { formValues } : FormValidateResult ) => { if ( formValues . test1 . length > formValues . test . length ) { return { type : "formError" , message : "something is wrong here" , } } if ( formValues . test === "test" ) { return "direct error message" } return true } , } ) shouldUnregister: boolean = false By default, an input value will be retained when an input is removed. However, you can set shouldUnregister to true to unregister the input during unmount. This is a global configuration that overrides child-level configurations. To have individual behavior, set the configuration at the component or hook level, not at useForm . By default, shouldUnregister: false means unmounted fields are not validated by built-in validation. By setting shouldUnregister to true at useForm level, defaultValues will not be merged against submission result. Setting shouldUnregister: true makes your form behave more closely to native forms. Form values are stored within the inputs themselves. Unmounting an input removes its value. Hidden inputs should use the hidden attribute for storing hidden data. Only registered inputs are included as submission data. Unmounted inputs must be notified at either useForm or useWatch 's useEffect for the hook form to verify that the input is unmounted from the DOM. const NotWork = ( ) => { const [ show , setShow ] = React . useState ( false ) // ❌ won't get notified; you need to invoke unregister return show && < input { ... register ( "test" ) } / > } const Work = ( { control } ) => { const { show } = useWatch ( { control } ) // ✅ gets notified in useEffect return show && < input { ... register ( "test1" ) } / > } const App = ( ) => { const [ show , setShow ] = React . useState ( false ) const { control } = useForm ( { shouldUnregister : true } ) return ( < div > // ✅ gets notified in useForm's useEffect { show && < input { ... register ( "test2" ) } / > } < NotWork / > < Work control = { control } / > < / div > ) } shouldUseNativeValidation: boolean = false This config will enable browser native validation . It will also enable CSS selectors :valid and :invalid , making input styling easier. You can still use these selectors even when client-side validation is disabled. Only works with onSubmit and onChange modes, as the reportValidity execution will focus the error input. Each registered field's validation message must be a string to be displayed natively. This feature only works with the register API and useController/Controller that are connected with actual DOM references. progressive: true is required alongside this option. Without it, React Hook Form does not forward validation attributes (e.g. required , minLength ) as native HTML attributes on the input, so the browser has nothing to validate against and no native UI is shown. useForm ( { shouldUseNativeValidation : true , progressive : true , // required — forwards validation rules as native HTML attributes } ) Examples: Copy import { useForm } from "react-hook-form" export default function App ( ) { const { register , handleSubmit } = useForm ( { shouldUseNativeValidation : true , progressive : true , } ) const onSubmit = async ( data ) => { console . log ( data ) } return ( < form onSubmit = { handleSubmit ( onSubmit ) } > < input { ... register ( "firstName" , { required : "Please enter your first name." , } ) } // custom message / > < input type = "submit" / > < / form > ) } disabled: boolean = false This config allows you to disable the entire form and all associated inputs when set to true . This can be useful for preventing user interaction during asynchronous tasks or other situations where inputs should be temporarily unresponsive. Examples: Copy import { useForm , Controller } from "react-hook-form" const App = ( ) => { const [ disabled , setDisabled ] = useState ( false ) const { register , handleSubmit , control } = useForm ( { disabled , } ) return ( < form onSubmit = { handleSubmit ( async ( ) => { setDisabled ( true ) await sleep ( 100 ) setDisabled ( false ) } ) } > < input type = { "checkbox" } { ... register ( "checkbox" ) } data - testid = { "checkbox" } / > < select { ... register ( "select" ) } data - testid = { "select" } / > < Controller control = { control } render = { ( { field } ) => < input disabled = { field . disabled } / > } name = "test" / > < button type = "submit" > Submit < / button > < / form > ) } resolver: Resolver This function allows you to use any external validation library such as Yup , Zod , Joi , Vest , Ajv and many others. The goal is to make sure you can seamlessly integrate whichever validation library you prefer. If you're not using a library, you can always write your own logic to validate your forms. Copy npm install @hookform/resolvers Props Name Type Description values object This object contains the entire form values. context object This is the context object which you can provide to the useForm config. It is a mutable object that can be changed on each re-render. options { "criteriaMode": "string", "fields": "object", "names": "string[]" } This is the options object containing information about the validated fields, names and criteriaMode from useForm . RULES Schema validation focuses on field-level error reporting. Parent-level error checking is limited to the direct parent level, which is applicable for components such as group checkboxes. This function will be cached. Re-validation of an input will only occur one field at a time during a user’s interaction. The library itself will evaluate the error object to trigger re-renders accordingly. A resolver cannot be used with the built-in validators (e.g.: required, min, etc.) When building a custom resolver: Make sure that you return an object with both values and errors properties. Their default values should be an empty object. For example: {} . The keys of the errors object should match the name values of your fields, but they must be hierarchical rather than a single key for deep errors: ❌ { "participants.1.name": someErr } will not set or clear properly - instead, use ✅ { participants: [null, { name: someErr } ] } as this is reachable as errors.participants[1].name - you can still prepare your errors using flat keys, and then use a function like this one from the zod resolver: toNestErrors(flatErrs, resolverOptions) Examples: Yup Zod Joi Ajv Vest Custom Copy CodeSandbox TS import { useForm } from "react-hook-form" import { yupResolver } from "@hookform/resolvers/yup" import * as yup from "yup" const schema = yup . object ( ) . shape ( { name : yup . string ( ) . required ( ) , age : yup . number ( ) . required ( ) , } ) . required ( ) const App = ( ) => { const { register , handleSubmit } = useForm ( { resolver : yupResolver ( schema ) , // yup, joi and even your own. } ) return ( < form onSubmit = { handleSubmit ( ( d ) => console . log ( d ) ) } > < input { ... register ( "name" ) } /> < input type = " number " { ... register ( "age" ) } /> < input type = " submit " /> </ form > ) } Copy CodeSandbox TS import { useForm } from "react-hook-form" import { zodResolver } from "@hookform/resolvers/zod" import * as z from "zod" const schema = z . object ( { name : z . string ( ) , age : z . number ( ) , } ) type Schema = z . infer < typeof schema > const App = ( ) => { const { register , handleSubmit } = useForm ( { resolver : zodResolver ( schema ) , } ) return ( < form onSubmit = { handleSubmit ( ( data ) => { // handle inputs console . log ( data ) } ) } > < input { ... register ( "name" ) } /> < input { ... register ( "age" , { valueAsNumber : true } ) } type = " number " /> < input type = " submit " /> </ form > ) } Copy CodeSandbox TS import { useForm } from "react-hook-form" import { joiResolver } from "@hookform/resolvers/joi" import Joi from "joi" interface IFormInput { name : string age : number } const schema = Joi . object ( { name : Joi . string ( ) . required ( ) , age : Joi . number ( ) . required ( ) , } ) const App = ( ) => { const { register , handleSubmit , formState : { errors } , } = useForm < IFormInput > ( { resolver : joiResolver ( schema ) , } ) const onSubmit = ( data : IFormInput ) => { console . log ( data ) } return ( < form onSubmit = { handleSubmit ( onSubmit ) } > < input { ... register ( "name" ) } /> < input type = " number " { ... register ( "age" ) } /> < input type = " submit " /> </ form > ) } Copy CodeSandbox TS import { useForm } from "react-hook-form" import { ajvResolver } from "@hookform/resolvers/ajv" // must use `minLength: 1` to implement required field const schema = { type : "object" , properties : { username : { type : "string" , minLength : 1 , errorMessage : { minLength : "username field is required" } , } , password : { type : "string" , minLength : 1 , errorMessage : { minLength : "password field is required" } , } , } , required : [ "username" , "password" ] , additionalProperties : false , } const App = ( ) => { const { register , handleSubmit , formState : { errors } , } = useForm ( { resolver : ajvResolver ( schema ) , } ) return ( < form onSubmit = { handleSubmit ( ( data ) => console . log ( data ) ) } > < input { ... register ( "username" ) } /> { errors . username && < p > { errors . username . message } </ p > } < input { ... register ( "password" ) } /> { errors . password && < p > { errors . password . message } </ p > } < button type = " submit " > submit </ button > </ form > ) } Copy CodeSandbox TS import * as React from "react" import { useForm } from "react-hook-form" import { vestResolver } from "@hookform/resolvers/vest" import vest , { test , enforce } from "vest" const validationSuite = vest . create ( ( data = { } ) => { test ( "username" , "Username is required" , ( ) => { enforce ( data . username ) . isNotEmpty ( ) } ) test ( "username" , "Must be longer than 3 chars" , ( ) => { enforce ( data . username ) . longerThan ( 3 ) } ) test ( "password" , "Password is required" , ( ) => { enforce ( data . password ) . isNotEmpty ( ) } ) test ( "password" , "Password must be at least 5 chars" , ( ) => { enforce ( data . password ) . longerThanOrEquals ( 5 ) } ) test ( "password" , "Password must contain a digit" , ( ) => { enforce ( data . password ) . matches ( / [ 0 - 9 ] / ) } ) test ( "password" , "Password must contain a symbol" , ( ) => { enforce ( data . password ) . matches ( / [ ^ A - Z a - z 0 - 9 ] / ) } ) } ) const App = ( ) => { const { register , handleSubmit } = useForm ( { resolver : vestResolver ( validationSuite ) , } ) return ( < form onSubmit = { handleSubmit ( ( data ) => console . log ( data ) ) } > < input { ... register ( "username" ) } /> < input { ... register ( "password" ) } /> < input type = " submit " /> </ form > ) } Copy CodeSandbox TS import * as React from "react" import { useForm } from "react-hook-form" import * as Joi from "joi" interface IFormInputs { username : string } const validationSchema = Joi . object ( { username : Joi . string ( ) . alphanum ( ) . min ( 3 ) . max ( 30 ) . required ( ) , } ) const App = ( ) => { const { register , handleSubmit , formState : { errors } , } = useForm < IFormInputs > ( { resolver : async ( data ) => { const { error , value : values } = validationSchema . validate ( data , { abortEarly : false , } ) return { values : error ? { } : values , errors : error ? error . details . reduce ( ( previous , currentError ) => { return { ... previous , [ currentError . path [ 0 ] ] : currentError , } } , { } ) : { } , } } , } ) const onSubmit = ( data : IFormInputs ) => console . log ( data ) return ( < div className = " App " > < h1 > resolver </ h1 > < form onSubmit = { handleSubmit ( onSubmit ) } > < label > Username </ label > < input { ... register ( "username" ) } /> { errors . username && < p > errors.username.message </ p > } < input type = " submit " /> </ form > </ div > ) } Need more? See Resolver Documentation TIP You can debug your schema via the following code snippet: Copy resolver : async ( data , context , options ) => { // you can debug your validation schema here console . log ( "formData" , data ) console . log ( "validation result" , await anyResolver ( schema ) ( data , context , options ) ) return anyResolver ( schema ) ( data , context , options ) } useForm return and useEffect dependencies In a future major release, the useForm return value will be memoized to optimize performance and reflect changes in the formState . As a result, adding the entire return value of useForm to a useEffect dependency list may lead to infinite loops. WARNING The following code is likely to create this situation: const methods = useForm ( ) useEffect ( ( ) => { methods . reset ( { ... } ) } , [ methods ] ) Passing only the relevant methods, as shown below, should avoid this kind of issue: const methods = useForm ( ) useEffect ( ( ) => { methods . reset ( { ... } ) } , [ methods . reset ] ) TIP The recommended way is to pass destructured methods to the dependencies of a useEffect const { reset } = useForm ( ) useEffect ( ( ) => { reset ( { ... } ) } , [ reset ] ) More info can be found on this issue Return The following list contains references to useForm return props. register unregister formState watch subscribe handleSubmit reset resetField resetDefaultValues setError clearErrors setValue setValues setFocus getValues getFieldState trigger control Form Thank you for your support If you find React Hook Form to be useful in your project, please consider starring and supporting it. Star us on GitHub Home Get Started API TS Advanced FAQs Form Builder DevTools Resources About us Media V8(Beta) Please support us by leaving a ★ @github SUPPORTED AND BACKED BY Powered by ▲ Vercel Edit ▲
