# mdn-navigator-online-2026-05 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T01:47:00Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r099`
**Body SHA-256 (below the `---` divider, header excluded):** 0a8b3a1682b80d8207d56394e287c134e73311d9a9a928f6cc247641d4d0801c

---

# Upstream Snapshot — MDN: navigator.onLine + online/offline events

**id:** mdn-navigator-online-2026-05  
**source:** https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine  
**fetched_at:** 2026-05-18T00:00:00Z  
**via:** WebFetch  
**tier:** 1  
**version_observed:** Living Standard (MDN 2026-05)

---

## navigator.onLine

> "`Navigator.onLine` returns the online status of the browser. The property returns a boolean
> value, with `true` meaning online and `false` meaning offline."

```js
if (navigator.onLine) {
  console.log('online')
} else {
  console.log('offline')
}
```

### Caveats

- A `true` value does not guarantee access to the internet — only that the device is connected
  to *some* network. A connected-but-captive-portal situation returns `true`.
- A `false` value reliably means offline (no network interface up).
- Initial value at page load may differ from the actual state if the network changed before
  the page was fully loaded.

## online / offline events

Listen for network state changes via `window` events:

```js
window.addEventListener('online', () => {
  console.log('Back online')
})

window.addEventListener('offline', () => {
  console.log('Lost network connection')
})
```

## visibilitychange pattern

Recheck `navigator.onLine` when the document becomes visible again to catch transitions
that occurred while the tab was in the background:

```js
document.addEventListener('visibilitychange', () => {
  if (!document.hidden) {
    updateOnlineStatus()
  }
})
```

## React hook pattern

```tsx
function useOnlineStatus(): boolean {
  const [isOnline, setIsOnline] = React.useState(navigator.onLine)

  React.useEffect(() => {
    const goOnline = () => setIsOnline(true)
    const goOffline = () => setIsOnline(false)
    const onVisible = () => setIsOnline(navigator.onLine)

    window.addEventListener('online', goOnline)
    window.addEventListener('offline', goOffline)
    document.addEventListener('visibilitychange', onVisible)

    return () => {
      window.removeEventListener('online', goOnline)
      window.removeEventListener('offline', goOffline)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [])

  return isOnline
}
```

## Source

- navigator.onLine: https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine
- online event: https://developer.mozilla.org/en-US/docs/Web/API/Window/online_event
- offline event: https://developer.mozilla.org/en-US/docs/Web/API/Window/offline_event

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://developer.mozilla.org/en-US/docs/Web/API/Navigator/onLine
HTTP status: 200 · extracted bytes: 7679 · sha256: 03ff900fe5e4fb9f79688b127b3f4c2e1ae6069d67402cc7d4023a80ede8d2a0
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r099`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

Navigator: onLine property - Web APIs | MDN Skip to main content Skip to search MDN HTML HTML: Markup language HTML reference Elements Global attributes Attributes See all… HTML guides Responsive images HTML cheatsheet Date & time formats See all… Markup languages SVG MathML XML CSS CSS: Styling language CSS reference Properties Selectors At-rules Values See all… CSS guides Box model Animations Flexbox Colors See all… Layout cookbook Column layouts Centering an element Card component See all… JavaScript JS JavaScript: Scripting language JS reference Standard built-in objects Expressions & operators Statements & declarations Functions See all… JS guides Control flow & error handing Loops and iteration Working with objects Using classes See all… Web APIs Web APIs: Programming interfaces Web API reference File system API Fetch API Geolocation API HTML DOM API Push API Service worker API See all… Web API guides Using the Web animation API Using the Fetch API Working with the History API Using the Web speech API Using web workers All All web technology Technologies Accessibility HTTP URI Web extensions WebAssembly WebDriver See all… Topics Media Performance Privacy Security Progressive web apps Learn Learn web development Frontend developer course Getting started modules Core modules MDN Curriculum Check out the video course from Scrimba, our partner Learn HTML Structuring content with HTML module Learn CSS CSS styling basics module CSS layout module Learn JavaScript Dynamic scripting with JavaScript module Tools Discover our tools Playground HTTP Observatory Border-image generator Border-radius generator Box-shadow generator Color format converter Color mixer Shape generator About Get to know MDN better About MDN Advertise with us Community MDN on GitHub Blog Toggle sidebar Web Web APIs Navigator onLine Theme OS default Light Dark English (US) Remember language Learn more Deutsch English (US) Español Français 日本語 한국어 中文 (简体) Navigator: onLine property Baseline Widely available This feature is well established and works across many devices and browser versions. It’s been available across browsers since July 2015. Learn more See full compatibility The onLine property of the Navigator interface returns whether the device is connected to the network, with true meaning online and false meaning offline. The property's value changes after the browser checks its network connection, usually when the user follows links or when a script requests a remote page. For example, the property should return false when users click links soon after they lose internet connection. When its value changes, an online or offline event is fired on the window . Browsers and operating systems leverage different heuristics to determine whether the device is online. In general, connection to LAN is considered online, even though the LAN may not have Internet access. For example, the computer may be running a virtualization software that has virtual ethernet adapters that are always "connected". On Windows, the online status is determined by whether it can reach a Microsoft home server, which may be blocked by firewalls or VPNs, even if the computer has Internet access. Therefore, this property is inherently unreliable, and you should not disable features based on the online status, only provide hints when the user may seem offline. In this article Value Examples Specifications Browser compatibility Value A boolean. Examples Basic usage To check if you are online, query window.navigator.onLine , as in the following example: js if (navigator.onLine) { console.log("online"); } else { console.log("offline"); } If the browser doesn't support navigator.onLine the above example will always come out as false / undefined . Listening for changes in network status To see changes in the network state, use addEventListener to listen for the events on window.online and window.offline , as in the following example: js window.addEventListener("offline", (e) => { console.log("offline"); }); window.addEventListener("online", (e) => { console.log("online"); }); Specifications Specification HTML # dom-navigator-online-dev Browser compatibility Enable JavaScript to view this browser compatibility table. Help improve MDN Was this page helpful to you? Yes No Learn how to contribute This page was last modified on Feb 15, 2025 by MDN contributors . View this page on GitHub • Report a problem with this content Filter sidebar Clear filter input HTML DOM API Navigator Instance properties activeVRDisplays appCodeName appName appVersion audioSession bluetooth buildID clipboard connection contacts cookieEnabled credentials deviceMemory devicePosture doNotTrack geolocation globalPrivacyControl gpu hardwareConcurrency hid ink keyboard language languages locks login maxTouchPoints mediaCapabilities mediaDevices mediaSession mimeTypes onLine oscpu pdfViewerEnabled permissions platform plugins preferences presentation product productSub scheduling serial serviceWorker storage usb userActivation userAgent userAgentData vendor vendorSub virtualKeyboard wakeLock webdriver windowControlsOverlay xr Instance methods canShare() clearAppBadge() deprecatedReplaceInURN() getAutoplayPolicy() getBattery() getGamepads() getInstalledRelatedApps() getUserMedia() getVRDisplays() javaEnabled() registerProtocolHandler() requestMediaKeySystemAccess() requestMIDIAccess() sendBeacon() setAppBadge() share() taintEnabled() unregisterProtocolHandler() vibrate() Related pages for HTML DOM BeforeUnloadEvent DOMStringMap ErrorEvent HTMLAnchorElement HTMLAreaElement HTMLAudioElement HTMLBRElement HTMLBaseElement HTMLBodyElement HTMLButtonElement HTMLCanvasElement HTMLDListElement HTMLDataElement HTMLDataListElement HTMLDialogElement HTMLDivElement HTMLDocument HTMLElement HTMLEmbedElement HTMLFieldSetElement HTMLFormControlsCollection HTMLFormElement HTMLFrameSetElement HTMLGeolocationElement HTMLHRElement HTMLHeadElement HTMLHeadingElement HTMLHtmlElement HTMLIFrameElement HTMLImageElement HTMLInputElement HTMLLIElement HTMLLabelElement HTMLLegendElement HTMLLinkElement HTMLMapElement HTMLMediaElement HTMLMenuElement HTMLMetaElement HTMLMeterElement HTMLModElement HTMLOListElement HTMLObjectElement HTMLOptGroupElement HTMLOptionElement HTMLOptionsCollection HTMLOutputElement HTMLParagraphElement HTMLPictureElement HTMLPreElement HTMLProgressElement HTMLQuoteElement HTMLScriptElement HTMLSelectElement HTMLSourceElement HTMLSpanElement HTMLStyleElement HTMLTableCaptionElement HTMLTableCellElement HTMLTableColElement HTMLTableElement HTMLTableRowElement HTMLTableSectionElement HTMLTemplateElement HTMLTextAreaElement HTMLTimeElement HTMLTitleElement HTMLTrackElement HTMLUListElement HTMLUnknownElement HTMLVideoElement HashChangeEvent History ImageData Location MessageChannel MessageEvent MessagePort PageRevealEvent PageSwapEvent PageTransitionEvent Plugin PluginArray PromiseRejectionEvent RadioNodeList TimeRanges UserActivation ValidityState Window WorkletGlobalScope Guides Using microtasks in JavaScript with queueMicrotask() In depth: Microtasks and the JavaScript runtime environment MDN Your blueprint for a better internet. MDN About Blog Mozilla careers Advertise with us MDN Plus Product help Contribute MDN Community Community resources Writing guidelines MDN Discord MDN on GitHub Developers Web technologies Learn web development Guides Tutorials Glossary Hacks blog Mozilla Website Privacy Notice Telemetry Settings Legal Community Participation Guidelines Portions of this content are ©1998–2026 by individual mozilla.org contributors. Content available under a Creative Commons license .
