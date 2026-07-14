---
snapshot_id: toss-billing-2026-05
source: "https://docs.tosspayments.com/guides/billing/overview"
fetched_at: "2026-07-14T00:00:00Z"
version_observed: "as published, fetched 2026-07-14"
via: curl
tier: 2
bytes: 961
sha: "70d66b0c80e9a6707175f0bd1b8c9ac767494e9a5f11499055f0023b6a756200"
---

# toss billing 2026 05 — upstream snapshot

Source: https://docs.tosspayments.com/guides/billing/overview
Fetched: 2026-07-14

## 자동결제(빌링) 개요 — 구독 상태는 가맹점이 직접 관리
Source: https://docs.tosspayments.com/guides/billing/overview

자동결제는 정기 배송, 음악 스트리밍과 같은 구독형 서비스에서 사용할 수 있어요. 구독 서비스는 API를 사용해서 직접 구축해야 합니다. 금액이 바뀌었다면 자동결제 승인 API를 호출할 때 amount 파라미터를 변경된 결제 금액으로 설정하면 됩니다.

## 멱등키 헤더
Source: https://docs.tosspayments.com/reference/using-api/idempotency-key

멱등성은 연산을 여러 번 하더라도 결과가 달라지지 않는 성질을 뜻합니다. 요청 헤더에 Idempotency-Key를 추가하면 멱등한 요청을 보낼 수 있습니다. 멱등키는 UUID와 같이 충분히 무작위적인 고유 값으로 생성해주세요. 최대 길이는 300자입니다. 멱등키는 처음 요청에 사용한 날부터 15일간 유효합니다.
