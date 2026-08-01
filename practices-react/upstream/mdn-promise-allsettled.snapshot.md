# mdn-promise-allsettled — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:46:47Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r073`
**Body SHA-256 (below the `---` divider, header excluded):** 098b4cb18da6698e6859a35aab57719352b1573143c6769087ee732eb35bd906

---

# Snapshot: MDN — Promise.allSettled()

- **source**: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled
- **role**: primitive-semantics
- **fetched_at**: 2026-05-16T00:00:00Z
- **via**: WebFetch

## When to use (verbatim)

> "Promise.allSettled() is typically used when you have multiple asynchronous tasks that are not dependent on one another to complete successfully, or you'd always like to know the result of each promise."

> "In comparison, the Promise returned by Promise.all() may be more appropriate if the tasks are dependent on each other, or if you'd like to immediately reject upon any of them rejecting."

## Result shape

Each outcome object has:

- `status`: `"fulfilled"` or `"rejected"`
- `value` (only if `status` === `"fulfilled"`)
- `reason` (only if `status` === `"rejected"`)

## Example (verbatim)

```javascript
Promise.allSettled([
  Promise.resolve(33),
  new Promise((resolve) => setTimeout(() => resolve(66), 0)),
  99,
  Promise.reject(new Error("an error")),
]).then((values) => console.log(values));

// [
//   { status: 'fulfilled', value: 33 },
//   { status: 'fulfilled', value: 66 },
//   { status: 'fulfilled', value: 99 },
//   { status: 'rejected', reason: Error: an error }
// ]
```

## Settle semantics

- Empty iterable → already fulfilled
- Non-empty with no pending promises → still asynchronously (not synchronously) fulfilled
- Waits for ALL inputs to settle regardless of any individual rejection

## Audit implication

Any rule about Promise.all must cross-reference allSettled for partial-failure handling. Vercel's seed rule omitted this; Next.js docs explicitly call it out.

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/allSettled
HTTP status: 200 · extracted bytes: 6607 · sha256: 410c0ea45c40d3885fe615eb36c31e405d8af38409d263e24048cc847747346e
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r073`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Promise.allSettled() - JavaScript | MDN Skip to main content Skip to search MDN HTML HTML: Markup language HTML reference Elements Global attributes Attributes See all… HTML guides Responsive images HTML cheatsheet Date & time formats See all… Markup languages SVG MathML XML CSS CSS: Styling language CSS reference Properties Selectors At-rules Values See all… CSS guides Box model Animations Flexbox Colors See all… Layout cookbook Column layouts Centering an element Card component See all… JavaScript JS JavaScript: Scripting language JS reference Standard built-in objects Expressions & operators Statements & declarations Functions See all… JS guides Control flow & error handing Loops and iteration Working with objects Using classes See all… Web APIs Web APIs: Programming interfaces Web API reference File system API Fetch API Geolocation API HTML DOM API Push API Service worker API See all… Web API guides Using the Web animation API Using the Fetch API Working with the History API Using the Web speech API Using web workers All All web technology Technologies Accessibility HTTP URI Web extensions WebAssembly WebDriver See all… Topics Media Performance Privacy Security Progressive web apps Learn Learn web development Frontend developer course Getting started modules Core modules MDN Curriculum Check out the video course from Scrimba, our partner Learn HTML Structuring content with HTML module Learn CSS CSS styling basics module CSS layout module Learn JavaScript Dynamic scripting with JavaScript module Tools Discover our tools Playground HTTP Observatory Border-image generator Border-radius generator Box-shadow generator Color format converter Color mixer Shape generator About Get to know MDN better About MDN Advertise with us Community MDN on GitHub Blog Toggle sidebar Web JavaScript Reference Standard built-in objects Promise allSettled() Theme OS default Light Dark English (US) Remember language Learn more Deutsch English (US) Français 日本語 한국어 Português (do Brasil) Русский 中文 (简体) Promise.allSettled() Baseline Widely available This feature is well established and works across many devices and browser versions. It’s been available across browsers since July 2020. Learn more See full compatibility The Promise.allSettled() static method takes an iterable of promises as input and returns a single Promise . This returned promise fulfills when all of the input's promises settle (including when an empty iterable is passed), with an array of objects that describe the outcome of each promise. In this article Try it Syntax Description Examples Specifications Browser compatibility See also Try it const promise1 = Promise.resolve(3); const promise2 = new Promise((resolve, reject) => setTimeout(reject, 100, "foo"), ); const promises = [promise1, promise2]; Promise.allSettled(promises).then((results) => results.forEach((result) => console.log(result.status)), ); // Expected output: // "fulfilled" // "rejected" Syntax js Promise.allSettled(iterable) Parameters iterable An iterable (such as an Array ) of promises. Return value A Promise that is: Already fulfilled , if the iterable passed is empty. Asynchronously fulfilled , when all promises in the given iterable have settled (either fulfilled or rejected). The fulfillment value is an array of objects, each describing the outcome of one promise in the iterable , in the order of the promises passed, regardless of completion order. Each outcome object has the following properties: status A string, either "fulfilled" or "rejected" , indicating the eventual state of the promise. value Only present if status is "fulfilled" . The value that the promise was fulfilled with. reason Only present if status is "rejected" . The reason that the promise was rejected with. If the iterable passed is non-empty but contains no pending promises, the returned promise is still asynchronously (instead of synchronously) fulfilled. Description The Promise.allSettled() method is one of the promise concurrency methods. Promise.allSettled() is typically used when you have multiple asynchronous tasks that are not dependent on one another to complete successfully, or you'd always like to know the result of each promise. In comparison, the Promise returned by Promise.all() may be more appropriate if the tasks are dependent on each other, or if you'd like to immediately reject upon any of them rejecting. Examples Using Promise.allSettled() js Promise.allSettled([ Promise.resolve(33), new Promise((resolve) => setTimeout(() => resolve(66), 0)), 99, Promise.reject(new Error("an error")), ]).then((values) => console.log(values)); // [ // { status: 'fulfilled', value: 33 }, // { status: 'fulfilled', value: 66 }, // { status: 'fulfilled', value: 99 }, // { status: 'rejected', reason: Error: an error } // ] Specifications Specification ECMAScript® 2027 Language Specification # sec-promise.allsettled Browser compatibility Enable JavaScript to view this browser compatibility table. See also Polyfill of Promise.allSettled in core-js es-shims polyfill of Promise.allSettled Using promises guide Graceful asynchronous programming with promises Promise Promise.all() Promise.any() Promise.race() Help improve MDN Was this page helpful to you? Yes No Learn how to contribute This page was last modified on Jul 10, 2025 by MDN contributors . View this page on GitHub • Report a problem with this content Filter sidebar Clear filter input Standard built-in objects Promise Constructor Promise() Static methods all() allSettled() any() race() reject() resolve() try() withResolvers() Static properties [Symbol .species] Instance methods catch() finally() then() Inheritance Object/Function Static methods apply() bind() call() toString() [Symbol .hasInstance]() Static properties displayName length name prototype arguments caller Instance methods __defineGetter__() __defineSetter__() __lookupGetter__() __lookupSetter__() hasOwnProperty() isPrototypeOf() propertyIsEnumerable() toLocaleString() toString() valueOf() Instance properties __proto__ constructor MDN Your blueprint for a better internet. MDN About Blog Mozilla careers Advertise with us MDN Plus Product help Contribute MDN Community Community resources Writing guidelines MDN Discord MDN on GitHub Developers Web technologies Learn web development Guides Tutorials Glossary Hacks blog Mozilla Website Privacy Notice Telemetry Settings Legal Community Participation Guidelines Portions of this content are ©1998–2026 by individual mozilla.org contributors. Content available under a Creative Commons license .
