# Session Log — ax-template

> ax-template 개발 세션의 롤링 기록(git-tracked 미러). 원본(canonical)은 Claude Code의
> auto-memory (`~/.claude/projects/.../memory/MEMORY.md`)이며, 이 문서는 그 **Sessions 섹션의
> 공유용 미러**다. Codex/AGENTS 등 교차 에이전트가 참조할 수 있도록 프로젝트 트리에 둔다.
> 규칙 충돌 시 우선순위는 `CLAUDE.md` > auto-memory > 이 문서다.

## Project Info (고정)

- ax-template = `/ax-transform` skill package source. public fork-base composition kit (React + Spring). 상세는 repo `CLAUDE.md`.
- **1인 프로젝트 — PR 생략, main 직접 push** (사용자 override 2026-07-10): worktree 격리 작업 → R25 PASS → `git push origin HEAD:main`. 게이트(pre-commit 4 gates · R25 · pre-push per-ref)는 로컬 훅으로 그대로 강제.
- R25 실행 전제: `JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home` (이 맥의 `/usr/bin/java`는 stub — 미지정 시 backend-build FAIL). worktree에서 돌릴 땐 main checkout의 `frontend/node_modules`를 `cp -Rc`로 복사(lockfile 미커밋).
- pre-push는 push-spec의 ref별 local_sha를 `completion_checklist_recency_guard.sh --expect-sha`로 검증 (2026-07-10 재설계). delete-only push는 스킵.

## Sessions (롤링)

### 2026-07-10 — ultracode dogfood 감사 → 강제 구멍 전량 봉합 (main = ee8164f)

- **dogfood 감사**(13 agents, 6 lanes + 적대검증 + gap critic): "규칙/템플릿 밖 구현 기계 차단 + 체크리스트 강제 + 피드백 루프" 검증. 메커니즘·non-vacuity는 진짜, 강제 경계에 구멍 확인.
- **PR #74**: pre-push path-filter 뒤 R25(범위밖 push 스킵) + `--step` 부분실행이 full PASS와 구분불가(→audit line `full_run` 필드 + `AUDIT_PARTIAL_RUN` + fixture + kill-proof 등재) + `ledger --since today` lexicographic 버그.
- **PR #75+#77**: P3 신규 등재 후 당일 봉합 (crud-l0 죽은참조 4곳→crud-security / guard 카운트 disk-truth / settings.local.json / ax-prove 이중 echo). 수렴 ~69%.
- **PR #76**: delete-only push R25 스킵 (over-blocking — ledger capture→resolve 한 사이클 완주).
- **Codex 교차리뷰**(gpt-5.6-sol xhigh): pre-push가 HEAD 검증(비체크아웃 브랜치 push 무임승차) + multi-ref fail-open + 이중echo 잔존 + P3 3건 → 전건 봉합, main 직접 push (`ee8164f`). 교훈: Codex 병렬 lane은 read-only 샌드박스에서 stall — 단일패스 강제 필요; effort는 "max"가 아니라 `xhigh`.
- 게이트 실전 작동 3회: backlog integrity guard가 테이블 미집계 차단, 강화된 pre-push가 stale push 차단, per-ref 게이트가 main push 검증.

### 2026-07-14 — backlog-100 wave — ✅ 완결 (main = 7e695a2)

- **목표**: 백로그 45건 전량(155/155 100%) + freshness 최신화. ultragoal plan `backlog-100`.
- **완료 (worktree `ax-template-backlog-100`)**: census(45건 판정) · P2 wave 5건→P2 100% · P3 41건 전량 구현/검증 — 9 lanes 병렬(sonnet 구현+opus 리뷰), 신규 도메인 25+확장 8, per-domain task 27개 신설, 적대 리뷰 2회(전량 봉합), 실버그 8건 발견·수정(락타임아웃→500, JPA `updatable=false` 오용, 오염 영속성 컨텍스트 재삽입 2건, flaky keystone 등). BACKLOG 155/155=100% + integrity guard PASS.
- **완결**: Gradle wrapper 8.14.5 범프 + freshness 등재 → 최종 R25 FULL PASS(10/0/0, `7e695a2`, 도중 structural-pregate가 unbounded-repo-read 2건 실전 적발→봉합) → push → ultragoal 9/9 → 정리 완료. 최종 수렴 155/157 ≈ 99%.
- **P0-28 closed 2026-07-15** (`59802e6`): ESLint 10.7.0 범프 — 코드 수정 0, 14룰+6앱 green, EOL 노출 해소. 수렴 156/157.
- **P0-27 closed 2026-07-16** (SB4 wave, `add7d19`): Boot 3.2.12→4.1.0 완료 — census-first 집행, Jackson은 deprecated bridge(spring-boot-jackson2) 의도 선택(돈 SPI 동작 보존; 후속 부채 P1-63 등재), Groovy GMM 강제정렬 결함 test-스코프 substitution 봉합, 위장 403 3건=Data JPA null-Specification 규약(`unrestricted()` 치환, SecurityConfig 무변경+도달성 delta 전수 무변화). R25 2회 PASS + opus 10렌즈 + codex xhigh PD7. 수렴 157/158.
- **운영 교훈**: 공유 worktree 다중 lane은 ①공유파일 main-loop 전담 + V-range 선할당 ②gradle mkdir-spinlock(`/tmp/ax-gradle-lock`) 직렬화 ③컴파일 간섭 90s 재시도 프로토콜로 안정화.

### 2026-07-16 — P1-63 Jackson 2→3 완전 이관 — ✅ 완결 (main = 413ecd4)

- **목표**: SB4 wave가 의도 선택한 deprecated bridge(spring-boot-jackson2) 제거 = Jackson 2→3(tools.jackson) 완전 이관. ultragoal `p1-63-jackson3` 4/4.
- **이관(`2ed6da9`)**: 38파일 tools.jackson 3.1.4로. API 매핑 jar-verified(`JsonDeserializer`→`ValueDeserializer`, `SerializerProvider`→`SerializationContext`, `JsonMappingException`→`DatabindException`, `JacksonException`=unchecked→`throws IOException` 제거, `ObjectMapper` immutable→`JsonMapper.builder`, java.time는 databind3 내장=jsr310 불필요, jackson-annotations 패키지 무변경). 거부 메시지 byte-identical. **RestAssured 테스트 하네스는 클라측 직렬화에 Jackson2(com.fasterxml) 필요 → test-scope `jackson-databind:2.21.4` + groovy substitution test/pitest 스코프**(앱 classpath는 Jackson3 단독).
- **codex xhigh 게이트 8라운드 수렴**: 돈/입력 표면이라 라운드마다 **입력증폭/DoS 클래스**를 팠음 — R1 money-string, R2 currency echo+독립 JsonMapper+off-by-one, R3 **동일테마 3연속→선제 batch-audit**(4개 chokepoint 균일방어: 전송 body cap 필터·공유 bounded-error advice·setProperty truncate·@Size 스위프), R4 잔여 Jackson 레버(`maxTokenCount`/`maxDocumentLength` 둘 다 -1)→**StreamReadConstraints 전체 envelope pin**(양 mapper)+필터 회귀2(`max-swallow-size:-1` 무제한드레인·locale `toLowerCase`), R5 **증폭클래스 CLOSED 확인**+정합성3(idempotency canonicalization 회귀=제약초과 body는 degrade 말고 REJECT 413/payment locale fail-open/swallow테스트 강화), R6 프로덕션 correct+테스트 non-vacuity 3(뮤테이션 생존자→RED-on-revert 증명), R7 config 되돌림 미킬→**설정불변식 테스트**(TomcatServerProperties bean), **R8 APPROVE**(225/225·314/314).
- **교훈**: same-theme distinct 3+는 즉시 batch-audit로 수렴(1건씩 핑퐁 금지) / codex는 repo 자체 방법론(falsification·ViolationProof)을 적용해 "green이어도 fix 되돌리면 통과=뮤테이션 생존"을 잡음 → 뮤테이션 락 필수 / **`codex exec` 백그라운드는 stdin이 열려있으면 "Reading additional input from stdin"에서 hang → `< /dev/null` 필수**.
- **완결**: R25 FULL 1차서 testRateLimit 5개 동일 dead-port 에러(`NoHttpResponseException`) 간헐실패 — **R22 ContextCache eviction flake**(신규 `@SpringBootTest` 추가로 캐시압력↑). isolation+aggregate green으로 flake 확증 → `@DirtiesContext(BEFORE_CLASS)` 문서화 mitigation → R25 FULL PASS 10/10 → push `add7d19..413ecd4` → 사용자 요청으로 최종 head에서 codex 1회 더(R9) → APPROVE(델타=annotation only, production/config/build diff=0). **codex 승인 head == 최종 head == main == `413ecd4`.** 수렴 158/158.

### 2026-07-17 — ultracode 적대감사 → 신규 11건 발견·전량 봉합 — ✅ 완결 (main = b6594d5)

- **감사(Workflow 8차원·23에이전트, refute-by-default)**: 백로그 158/158 닫힌 상태에서 신규 버그/개선 발굴. 14후보→11 confirmed(2 webhook 정확히 refuted: receiver de-dup은 spec이 advisory·fork-receiver 영역 명시 / 기본시크릿은 config-no-secret-in-yaml 룰이 허용한 placeholder). MONEY-01은 verify agent가 transient 403로 죽어 누락될 뻔→journal 복구→opus 재검증서 2→8파일 확대. 등재 158→169(93%), integrity guard PASS.
- **신규 11건**: P0-29(useOptimisticUpdate 공유 isPending/snapshotRef 더블클릭 데이터손실) / P1-64(돈 8서비스 idempotency catch-and-requery가 Postgres aborted-tx 25P02서 500; H2 `MODE=PostgreSQL`은 statement-level rollback이라 테스트 통과=검증사각) / P1-65(save()-무flush 死catch 2서비스) / P1-66(audit-export GET authz 무검사 IDOR) / P1-67(SearchPalette `navigator.platform` SSR hydration mismatch) / P2-21(R25가 trio_integrity/cross_trio를 live 아닌 fixture로만 검사=Spec Trio 미게이트) / P2-22(full_trio 선언 29 spec이 contract·blueprint 0) / P2-23(templates/L4 137 .tsx가 ax/* ESLint·tsc 제외) / P3-48(RRN 하이픈필수→무하이픈 누출) / P3-49(228룰 stale) / P3-50(가드카운트 문서 drift).
- **전량 봉합(worktree `fix/audit-seal-11`, 5 lane 병렬)**: P1-64=공유 `common/IdempotentInsert`(@Transactional REQUIRES_NEW) bean-경계 격리(8 terminal-insert). **적대리뷰(opus)가 자체 REQUIRES_NEW fix의 원자성 회귀 2건 적발** — ①OrgScope: outer `FOR UPDATE` 락 across REQUIRES_NEW FK-child insert = Postgres cross-connection self-hang(정상경로!) → lock+recheck 이미 멱등이라 outer-tx insert로 환원 ②Reconciliation: non-terminal insert 독립커밋→outer 실패시 고아 empty run → catch-outside-tx 원자패턴. uomconversion은 FK 없어(V067) 안전 확인. P2-21 live 게이트 승격(RED-on-revert). P2-22 29 spec 재분류+`full_trio_artifact_completeness_guard` 신설(가드 91→92). P2-23 lint는 L1+L4 확장·실버그 2건 봉합, tsc는 by-design 미배선(템플릿=copy-target 미벤더 deps). 프론트 뮤테이션락 RED-on-revert.
- **완결**: R25 FULL PASS 10/10(`b6594d5`) → codex xhigh **1라운드 APPROVE**(6영역 NONE, uomconversion no-FK까지 V067 읽어 독립확인) → push `413ecd4..b6594d5` → 정리 완료. 수렴 169/169=100%.
- **교훈**: ①적대감사는 닫힌 100% repo에서도 실사각 발견(특히 강제/검증 seam·프론트 템플릿층=기계커버리지 얇은 곳) ②**REQUIRES_NEW-inner-insert는 terminal insert에만 안전** — 락 보유/post-insert write 있으면 cross-connection self-hang(FK child)이나 고아; 비-terminal은 catch-outside-tx→fresh-tx requery 패턴 ③H2 `MODE=PostgreSQL`은 25P02 미재현→Postgres tx-abort 계열 버그는 구조적 락으로 방어(테스트로 못 잡음) ④codex verify agent가 transient 403로 죽으면 journal에서 finding 복구(silent drop 방지) ⑤Workflow는 read-only 감사 fan-out에 적합, 파일편집 wave는 Agent+worktree+spinlock(공유파일 main 조율).

### 2026-07-19 — completeness 적대감사 → freshness/보안 잔여 6건 봉합 — ✅ 완결 (main = 4f6e870)

- **질문**: "SB4 메이저 업그레이드·다른 작업 다 했나?" → Workflow 5-lane completeness 감사(버전최신성 live web 검증 강제·SB4잔재·wave잔재·신규잠복버그·유예갭재판정). **정직한 답**: SB4(3.2.12→4.1.0, Framework 7.0.8)·Jackson3·169백로그는 진짜 완결 — 4.1.0이 최신 major/patch임을 endoflife.date + GitHub releases로 live 확인(4.1.1/4.2/5.0 없음, EOL 2027-07-31; "구버전" 후보 refuted). **하지만 everything_done=false**, 6건 actionable 발견.
- **봉합 6건 (worktree `fix/completeness-followups`)**: P2-24 auth가 VERIFY/RESET **bearer 토큰을 stdout 무가드 출력**(승격 gotcha 정면충돌)→DevTokenSink prod-gated sink / P2-25 rest-assured 5.5.7→**6.0.1로 마지막 Jackson-2 test 잔재+Groovy substitution 제거**(Jackson 이관 test classpath까지 완결, DSL 5→6 source-compatible 무변경) / P2-26 **POI 5.2.5 CVE-2025-31672**→5.5.1 / P3-51 **전수 locale 스윕**(14 fold/12파일 — R5가 changed-files라 잔재 반복한 근본원인 종결 + CircuitBreakerPolicy `%.2f` JSON 감사메타 독/프 로케일 깨짐) / P3-52 testcontainers 死의존성 제거 / P3-53 node engines `>=26`→`>=24` LTS.
- **codex gpt-5.6-sol xhigh 전수 리뷰(사용자 지정 모델)**: R1 **REJECT 1 HIGH** — DevTokenSink `enabled=exposeOptIn||(isDev&&!isProd)`서 **opt-in이 prod veto override** → `--profiles=prod --expose-dev-tokens=true`가 프로덕션서 RESET 토큰 INFO 로그 유출(자체 focused 적대리뷰는 "opt-in fine"으로 통과시켰으나 opt-in>prod 우선순위를 안 따짐; codex가 잡음). 봉합: `!isProd && (isDev||opt-in)` **prod 절대veto**(bearer 덤프는 R47 email보다 엄격, escape hatch 불가), ViolationProof를 prod+opt-in→false로 뒤집음. R2 **APPROVE**.
- **완결**: R25 FULL 10/10(`4f6e870`) → push `2d6d52f..4f6e870` → 정리. 수렴 **175/175=100%**.
- **교훈**: ①"다 했나" 완결성 질문엔 버전을 **live 소스로 실증**(학습컷 이후 릴리스 존재 가능) — 최신확인은 refute, 미봉합은 confirm ②**bearer/자격증명 게이트는 prod 절대veto**(opt-in override 금지) — 자체 focused 리뷰가 놓친 걸 cross-family codex가 잡음=교차검증 가치 ③codex exec 모델 지정은 `-c model="gpt-5.6-sol"` ④freshness는 EOL/CVE(=confirm 봉합) vs 단순 신규(=fork-receiver 자율 defer) 구분; 미사용 dep은 upgrade 아닌 제거가 정답 ⑤rest-assured 6.0.1이 P1-63의 마지막 test-classpath Jackson-2 잔재를 제거해 이관을 진짜 완결.
