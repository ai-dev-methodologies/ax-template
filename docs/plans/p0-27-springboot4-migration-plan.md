# P0-27 실행 계획 — Spring Boot 3.2.12 → 4.1 마이그레이션

> 2026-07-14 freshness 감사에서 등재된 expiry-bound 항목의 실행 계획.
> **착수 조건: 새 세션 + 최소 600k~1M 출력 토큰 여유.** 비용이 "깨지는 개수"에 비례하는데
> 그 수가 사전에 안 보이므로, **census를 첫 게이트로 두고 결과가 나쁘면 그 시점에 중단**한다.

## 배경 (검증된 사실, 2026-07-14 기준)

- Spring Boot 3.x 전 라인 OSS 지원 종료 (3.2는 2024-11, 마지막 minor 3.5도 2026-06-30 EOL)
  → 현재 pin(3.2.12)은 보안패치 무수신. 3.x 내 범프는 무의미 — **4.1 직행**만 유효.
- 4.0.0 GA 2025-11-20 (EOL 2026-12) / **4.1.0 GA 2026-06-10 (EOL 2027-07)** ← 타깃.
- Java 베이스라인 17 (25까지 1급) — **JDK 21 그대로 사용**. Gradle 8.14+ 필요 — **8.14.5 완료(선행조건 해소)**.

## 마이그레이션 표면 (예상 큰 순서)

1. **Hibernate major bump** — 이 repo는 `@Check` DB backstop을 다수 도메인이 사용(backlog-100
   wave로 표면 대폭 증가). H2 DDL 생성 동작 재검증이 최대 리스크.
2. **Jackson 2→3** — 좌표 `com.fasterxml`→`tools.jackson`. 커스텀 ObjectMapper 설정 전수 점검.
3. **Spring Security 6→7 + OAuth2 자동설정 조건 클래스 재배치** — SecurityConfig 직접 영향.
4. Jakarta EE 11 / Servlet 6.1 — 임베디드 Tomcat 세대 교체(BOM 자동, 명시 pin 없음 확인됨).
5. 3.x deprecated API 제거분 — census가 드러냄.

## 실행 절차 (메인 세션 = 계획·검증·위임만)

| 단계 | 담당 | 모델·추론강도 | 산출 |
|---|---|---|---|
| 0. 릴리스노트 전수(4.0+4.1) + 의존성 감사 | 서브에이전트 | sonnet·medium | 변경점 체크리스트 |
| 1. worktree 격리 + 버전 pin 범프 | 메인 | — | feat/p0-27-sb4 |
| 2. **깨짐 census**: compileJava/compileTestJava + 전 per-domain task 1회 실행, 실패 분류 | 서브에이전트 | haiku·low(집계) + sonnet·medium(분류) | **GO/STOP 게이트** — 실패 도메인 수·유형 표 |
| 3a. 기계적 수정 (Jackson 좌표, import 이동, deprecated 치환) | 서브에이전트 병렬 | sonnet·low~medium | |
| 3b. **SecurityConfig/OAuth2 재배치 + @Check DDL 검증** | 서브에이전트 | **opus·high** (보안·인증 = Hard 고정) | |
| 4. TDD 프레임: 기존 121 per-domain task가 RED→GREEN 루프 그 자체 (/tdd-workflow 취지 — 신규 테스트 발명 금지, 기존 게이트가 시험) | — | — | 전 태스크 GREEN |
| 5. 적대 리뷰 (refute-by-default, 특히 authz 도달성 매트릭스 — Security 7 전환 후 필수) | 서브에이전트 | opus·high | ACCEPT/REJECT |
| 6. R25 full + /verification-loop (실패 시 수정→재실행 루프) | 메인(기계) | — | PASS 10/0/0 |
| 7. **PD7 cross-family 게이트** — codex 단일패스 리뷰 (인증 스택 필수; OpenAI 토큰이라 Anthropic 예산 무부담) | codex | **xhigh** (max 아님 — 검증된 값) | APPROVE |
| 8. push origin HEAD:main + BACKLOG P0-27 closure | 메인 | — | |

## 운영 규칙 (backlog-100 wave에서 검증된 것 재사용)

- gradle 동시 실행 금지 — mkdir-spinlock(/tmp/ax-gradle-lock) 직렬화.
- 공유 파일(build.gradle.kts·SecurityConfig·checklist·BACKLOG)은 메인 루프 전담.
- R25 실행 전제: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`.
- 커밋은 census GREEN 이후에만; 중간 상태로 세션이 끊길 상황이면 worktree 보존 + MEMORY 체크포인트.

## 중단 기준 (STOP 게이트)

census에서 ①실패 도메인 > 40개 또는 ②Hibernate DDL 비호환이 @Check 표면 전반에서 구조적으로
확인되면: 즉시 중단하고 "수정 규모 견적 + 분할 전략(도메인 그룹별 단계 마이그레이션)"만 보고서로
남긴 뒤 사람 판단으로 넘긴다. 반쯤 마이그레이션된 인증 스택을 남기는 것이 최악의 결과다.

## 집행 결과 부기 (2026-07-16)

계획 대비 1건 의도적 이탈: Jackson은 tools.jackson 이관 대신 **Jackson 2 bridge
(spring-boot-jackson2, deprecated) 유지**를 선택 — 돈/입력엄격성 SPI 서브클래스 4개의
동작 보존을 우선(적대 리뷰가 단일 클래스패스 실증). 잔여 부채는 BACKLOG P1-63으로 등재.
Groovy 4→5 GMM 강제정렬 결함(536건 위장 실패)은 dependencySubstitution으로 봉합.
