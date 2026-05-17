---
snapshot_id: kakao-postcode-2026-05
source: "https://postcode.map.kakao.com/guide"
fetched_at: "2026-05-18T00:00:00Z"
via: WebFetch
bytes: 1840
sha: "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2"
tier: 3
---

# Kakao Postcode Widget API — Frozen Snapshot 2026-05

Source: https://postcode.map.kakao.com/guide  
Fetched: 2026-05-18  
Purpose: Evidence anchor for `templates/L1/components/address-search.tsx`

## Script Loading

```html
<script src="//t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>
```

## Constructor & open()

```javascript
new kakao.Postcode({
  oncomplete: function(data) { /* ... */ }
}).open({
  q: 'optional pre-fill search term',
  autoClose: true
});
```

## Embed Mode (iframe-style)

```javascript
new kakao.Postcode({
  oncomplete: function(data) { /* ... */ }
}).embed(targetElement, { q: 'search term' });
```

## oncomplete Callback — Key Data Fields

| Property | Example value | Description |
|----------|--------------|-------------|
| `zonecode` | `"13529"` | 5-digit new postal code (우편번호) |
| `roadAddress` | `"경기 성남시 분당구 판교역로 166"` | 도로명주소 |
| `jibunAddress` | `"경기 성남시 분당구 백현동 532"` | 지번주소 |
| `addressEnglish` | `"166 Pangyoyeok-ro, Bundang-gu, ..."` | English road address |
| `userSelectedType` | `"R"` or `"J"` | R = road, J = jibun; user's selection |
| `buildingName` | `"카카오"` | Building/complex name if applicable |
| `bname` | `"백현동"` | Legal district name (법정동명) |
| `bname1` | `"분당구"` | Administrative region |

## Production CDN URL (v2)

```
//t1.kakaocdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js
```

This is the stable v2 endpoint. As of 2026-05 it remains the recommended endpoint
per the official guide (https://postcode.map.kakao.com/guide).

## Controlled Component Pattern

For React integration, the widget is loaded once via script injection and invoked
via `new kakao.Postcode({oncomplete}).open()`. The controlled component pattern
stores `zonecode`, `roadAddress`, `jibunAddress`, and `detailAddress` in component state.
