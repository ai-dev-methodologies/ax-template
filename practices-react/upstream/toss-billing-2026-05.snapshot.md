# toss-billing-2026-05 — upstream snapshot (2026-08-01 refresh, append-only)

**Source URL(s):** https://docs.tosspayments.com/guides/billing/overview (re-fetched 2026-08-01; every pre-existing section below the divider is preserved verbatim)
**HTTP status:** 200
**Fetched at:** 2026-08-01T02:23:41Z
**Extractor invocation:** `practices/scripts/snapshot-extract.sh https://docs.tosspayments.com/guides/billing/overview`
**Fetch receipt:** `practices/upstream/_FETCH-RECEIPTS.yaml` id `r110`
**Body SHA-256 (below the `---` divider, header excluded):** bdedbcdd9929cbb2b89c6e91ab43734dd6c8717e089dc1c34bae58e9063df208

---

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

---

## Upstream refresh 2026-08-01 (verbatim extractor output)

Source: https://docs.tosspayments.com/guides/billing/overview
HTTP status: 200 · extracted bytes: 6860 · sha256: 764f385148f9f047b818bf035eb736f62cf851fc75a45fd2684796443dcde71d
Extractor: `practices/scripts/snapshot-extract.sh` (curl -> deterministic HTML->text; no model in the loop)
Fetch receipt: `practices/upstream/_FETCH-RECEIPTS.yaml` id `r110`

Everything above this divider is the previous snapshot, preserved byte-for-byte (append-only:
history is recorded, never rewritten). The block below is the UNMODIFIED extractor output for
the 2026-08-01 re-fetch of the same URL — it is the current upstream text, and any citation that
claims to quote this source verbatim must match it.

자동결제(빌링) 이해하기(Version 1) | 토스페이먼츠 개발자센터 가이드 API & SDK API 레퍼런스 SDK 레퍼런스 기관 및 ENUM 코드 샌드박스 커뮤니티·지원 시스템 상태 실시간 문의 용어사전 자주 묻는 질문 릴리즈 노트 블로그 로그인 시작하기 기초 지식 결제 기초 서비스 기초 환경 설정하기 결제위젯 이해하기 어드민 사용하기 연동하기 배포 체크리스트 추천 서비스 브랜드페이 PayPal Pro 기능 기타 제품 브랜드페이 자동결제(빌링) 결제창 더 알아보기 웹훅 연결하기 결제 취소하기 결제 깊이 이해하기 결제수단 결제 흐름 세금 처리 결제 결과 안내 연동 깊이 이해하기 간편결제 응답 확인 웹뷰 연동 Version 2 → 가이드 / 자동결제(빌링) 목차 연동하기 결제창 방식 API 방식 자동결제 과정 1. 빌링키 발급받기 2. 자동결제 실행하기 자주 묻는 질문 원하는 정보를 찾기 어렵나요? Version 1 자동결제(빌링) 이해하기 Markdown으로 복사 AI에게 질문 자동결제 는 정기 배송, 음악 스트리밍과 같은 구독형 서비스에서 사용할 수 있어요. 토스페이먼츠는 신용·체크카드 및 계좌이체 2가지 결제수단으로 자동결제를 지원해요. 자동결제는 리스크 검토 및 추가 계약 후 사용할 수 있습니다. 정기 구독형 서비스가 아니라면 정책적으로 자동결제 사용이 제한되니 유의하세요. 도입 문의는 아래 버튼을 눌러주세요. 도입 문의하기 연동하기 결제창 방식 카드 자동결제 토스페이먼츠 결제창을 통해 카드 빌링키를 발급받아요. 계좌 자동결제 퀵계좌이체를 통해 계좌 빌링키를 발급받아요. API 방식 카드 자동결제 카드 정보를 직접 API로 전달해 빌링키를 발급받아요. * API 방식은 카드만 지원합니다. 자동결제 과정 일반결제는 결제하는 시점에 매번 구매자의 본인인증이 필요하지만, 자동결제는 구독 주기마다 본인인증 없이 결제할 수 있습니다. 최초 본인인증을 해서 발급된 빌링키가 이후의 본인인증 과정을 대신하기 때문입니다. 기술적으로는 매 회 본인인증이 일어나지만, 구매자 입장에서는 최초 인증 후에는 따로 인증 과정이 없는 결제 경험을 하게 됩니다. 자동결제는 다음과 같은 순서로 이루어집니다. 구매자의 카드·계좌 정보로 토스페이먼츠에서 빌링키를 발급받고 저장합니다. 빌링키로 구독 주기마다 원하는 금액을 자유롭게 결제합니다. 1. 빌링키 발급받기 빌링키란? 빌링키는 구매자의 카드번호, 유효기간, CVC 등 결제 정보를 암호화한 값으로 생각할 수 있어요. 본인인증을 마치고 한 번 빌링키를 발급받으면, 구매자의 의사와 무관하게 빌링키로 계속 결제가 가능해요. 악용되지 않게 반드시 구매자 본인인증을 받은 뒤에 빌링키를 발급받는 것을 추천해요. 빌링키는 결제수단을 관리하는 곳에서 발급해요. 예를 들어, 구매자가 정보를 등록했다면 카드사·은행에서 빌링키를 발급해요. PG사는 카드사·은행에서 발급한 빌링키를 상점에게 전달해요. 빌링키는 구매자 정보와 함께 서버에 저장하세요. 한 번 발급받은 빌링키는 다시 조회할 수 없습니다. 더 자세한 빌링키 설명은 용어사전 > 자동결제 에서 확인해보세요. 2. 자동결제 실행하기 발급받은 빌링키로 자동결제 승인 API 를 호출해서 구독 주기에 맞춰 원하는 금액을 자유롭게 결제합니다. 자주 묻는 질문 구독 서비스는 어떻게 만들어야 하나요? 구독 서비스는 API를 사용해서 직접 구축해야 합니다. 1달 주기로 결제가 필요한 상품이면 1달마다 customerKey , 빌링키, 금액을 설정해서 자동결제 승인 API 를 호출하면 됩니다. 구현하는 방법이 궁금하다면 구독 결제 서비스 간단히 구현하기 시리즈를 참고하세요. 구매자가 구독을 취소하면 어떻게 해야 하나요? 다음 결제일에 구독을 취소한 구매자의 빌링키, customerKey 로 자동결제 승인 API 를 호출하지 않으면 됩니다. 사용하지 않는 빌링키 삭제 API 로 삭제할 수 있습니다. 구독 결제 금액이나 결제 주기가 변경되면 어떻게 해야 하나요? 금액이 바뀌었다면 자동결제 승인 API 를 호출할 때 amount 파라미터를 변경된 결제 금액으로 설정하면 됩니다. 결제 주기가 바뀌었다면 자동결제 승인 API를 호출하는 주기를 변경해주세요. 간편결제로 자동결제(빌링)를 구현할 수 있나요? 아니요. 토스페이, 카카오페이, 네이버페이와 같은 국내 간편결제는 지원하지 않습니다. PayPal(페이팔)과 같은 해외 간편결제도 지원하지 않습니다. 카드·계좌를 재발급 받거나 유효기간이 만료되면 어떻게 해야 하나요? 새로운 카드·계좌 정보로 빌링키를 다시 발급받으세요. 빌링키를 갱신하는 별도 과정은 없습니다. 결제할 수 있는 카드인지 유효성을 검사할 수 있는 방법은 없나요? 별도로 제공하지 않습니다. 자동결제에 등록할 카드의 유효성 여부는 빌링키 발급을 요청할 때 카드사를 통해 확인합니다. 만약 유효하지 않다면 에러 를 응답합니다. 카드 잔고 부족이나 한도 초과는 결제 승인을 요청 할 때 카드사를 통해 확인합니다. 'NOT_SUPPORTED_METHOD' 에러는 왜 발생하나요? 자동결제 계약이 안 되어 있는 클라이언트 키로 연동하면 발생합니다. 자동결제 계약이 되어있는 클라이언트 키를 사용하거나 토스페이먼츠 고객센터(1544-7772, support@tosspayments.com )로 문의해주세요. 'NOT_MATCHES_CUSTOMER_KEY' 에러는 왜 발생하나요? customerkey 와 매핑되지 않은 billingKey 를 사용하면 발생합니다. 빌링키를 조회할 수 있나요? 발급된 빌링키를 조회하는 API는 제공되지 않습니다. 빌링키 발급 이후 반드시 안전하게 저장하세요. 만약에 빌링키를 잃어버렸다면 구매자에게 다시 결제 정보를 받아서 새로운 빌링키를 발급받아주세요. 카드·계좌 하나에 여러 개의 빌링키를 중복으로 발급할 수 있어요. 빌링키 중복 발급을 방지하는 방법은 없습니다. 더 궁금한 내용이 있나요? 자주 묻는 질문 코드 샘플을 참고하세요 TossPayments GitHub 기술지원이 필요한가요? 실시간 문의 | 이메일 보내기 개발 문서를 AI에 연결하세요 llms.txt
