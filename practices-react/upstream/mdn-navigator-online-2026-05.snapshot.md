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
