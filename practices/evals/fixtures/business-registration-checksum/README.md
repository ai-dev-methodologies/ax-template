# Fixture: business-registration-checksum

## Purpose

Verifies that `validateBusinessRegistration()` from
`templates/L1/components/business-registration-input.tsx` correctly implements the
국세청 (National Tax Service) 사업자등록번호 checksum algorithm.

## Algorithm Reference

**Source:** 국세청 사업자등록번호 검증 알고리즘
**URL:** https://www.nts.go.kr/nts/cm/cntnts/cntntsView.do?mi=2227&cntntsId=7870
**Fetched:** 2026-05-18
**License:** 공공저작물 자유이용허락 (Public Domain — Korean Government Open Data)

### Algorithm specification

```
weights = [1, 3, 7, 1, 3, 7, 1, 3, 5]
sum  = Σ(digits[i] × weights[i]) for i = 0..7
sum += floor(digits[8] × 5 / 10)   // 9th digit integer part
sum += (digits[8] × 5) % 10        // 9th digit remainder part
checkDigit = (10 - (sum % 10)) % 10
valid = (checkDigit === digits[9])
```

## Fixture Data Sources

All business registration numbers in `pass/` are from **publicly registered Korean corporations**
whose business registration numbers are part of the public record (공시 자료, 금융감독원 DART,
사업자등록증명원 공개 정보). No mock or fabricated data is used.

**Source 1:** 금융감독원 전자공시시스템 (DART) — https://dart.fss.or.kr
**License:** 공공저작물 자유이용허락 (CC0-equivalent for government data)

**Source 2:** 공공데이터포털 사업자등록 정보 — https://www.data.go.kr/data/15081808/fileData.do
**License:** 공공데이터 제공 및 이용활성화에 관한 법률 §7 (Public Domain)

| 사업자등록번호 | 법인명 | 출처 | 알고리즘 검증 |
|---|---|---|---|
| 124-81-00998 | 삼성전자 주식회사 | DART 공시 (Samsung Electronics) | checksum=8 ✓ |
| 120-81-47521 | 카카오 주식회사 | DART 공시 (Kakao Corp.) | checksum=1 ✓ |
| 220-81-62517 | 네이버 주식회사 | DART 공시 (NAVER Corp.) | checksum=7 ✓ |
| 107-86-14075 | 엘지전자 주식회사 | DART 공시 (LG Electronics) | checksum=5 ✓ |
| 120-81-20653 | 현대자동차 주식회사 | DART 공시 (Hyundai Motor) | checksum=3 ✓ |

## Verification

```
checksum(124-81-00998) = (10 - (1+6+28+8+3+0+0+27+floor(45/10)+(45%10)) % 10) % 10
                       = (10 - (1+6+28+8+3+0+0+27+4+5) % 10) % 10
                       = (10 - (82 % 10)) % 10
                       = (10 - 2) % 10 = 8 ✓

checksum(120-81-47521) = 1+6+0+8+3+28+7+15+floor(10/10)+(10%10)
                       = 1+6+0+8+3+28+7+15+1+0 = 69
                       = (10 - 9) % 10 = 1 ✓

checksum(220-81-62517) = 2+6+0+8+3+42+2+15+floor(5/10)+(5%10)
                       = 2+6+0+8+3+42+2+15+0+5 = 83
                       = (10 - 3) % 10 = 7 ✓

checksum(107-86-14075) = 1+0+49+8+18+42+4+0+floor(35/10)+(35%10)
                       = 1+0+49+8+18+42+4+0+3+5 = 130 → 130%10=0 wait...
                       Digits: 1,0,7,8,6,1,4,0,7,5
                       Weights: 1,3,7,1,3,7,1,3,5
                       = 1×1+0×3+7×7+8×1+6×3+1×7+4×1+0×3+floor(7×5/10)+(7×5%10)
                       = 1+0+49+8+18+7+4+0+3+5 = 95
                       = (10 - 5) % 10 = 5 ✓

checksum(120-81-20653) = 1×1+2×3+0×7+8×1+1×3+2×7+0×1+6×3+floor(5×5/10)+(5×5%10)
                       = 1+6+0+8+3+14+0+18+2+5 = 57
                       = (10 - 7) % 10 = 3 ✓
```

## fail_invalid_checksum/

Same 5 business numbers with the **last digit mutated by +1 (mod 10)**.
`validateBusinessRegistration()` must return `false` for all of them.

## fail_format_violation/

Inputs with letters, wrong length, or special characters.
`validateBusinessRegistration()` must throw `FormatViolationError` for all of them.
