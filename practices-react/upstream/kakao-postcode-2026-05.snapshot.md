# Kakao Postcode Widget API — Frozen Snapshot + oncomplete Callback Refresh

**Source URL(s):** https://postcode.map.kakao.com/guide
**HTTP status:** 200
**Fetched at:** 2026-07-30T00:51:30Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://postcode.map.kakao.com/guide`
**Body SHA-256 (below the `---` divider, header excluded):** abb7d582ae297e778a931f4f2b5ad272c8a02b61d15d5db3f56a0951d5dfe767

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

## oncomplete-callback

Source: https://postcode.map.kakao.com/guide (재확인 2026-07-30, curl+snapshot-extract.sh)

카카오 우편번호 서비스 공식 가이드의 `oncomplete` 콜백 설명 원문(한국어, verbatim):

이 함수를 정의할때 넣는 인자에는 우편번호 검색 결과 목록에서 사용자가 클릭한 주소 정보가 들어가게 됩니다.

(위 문장은 `oncomplete` 콜백에 전달되는 인자가 사용자가 검색 결과에서 클릭한 주소 정보를
담고 있음을 설명한다 — zonecode/roadAddress/jibunAddress 등 실제 필드는 위 "oncomplete
Callback — Key Data Fields" 표 참고.)
