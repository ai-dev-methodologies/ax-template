# PRD — residual-18 (post remaining-19 잔여 전량)

Base: main @ c377d1c. 대상 18행. 목표: **정직한 closure 18/18** — 단, 구조적으로
막힌 항목(네트워크 필요 등)은 사유와 함께 open 유지가 정직한 결과다(Driver:
"honest closure including justified re-scope", remaining-19 합의 원칙 승계).

## 레인 (표면 기준, 충돌 0 설계)
| Lane | 항목 | 표면 |
|---|---|---|
| α | P2-54, P2-55, P2-56 | frontend/tests/auth-shape-lock.vitest.ts 단독 |
| β | P2-50, P2-52, P3-99 | fixture_kill_proof_guard.sh · domain_mode_consistency_guard.sh |
| γ | P2-51, P3-94, (P3-93 판정) | evidence_protected_template_anchors.txt(+가드 상수) · JwtAuthenticationFilter evidence |
| δ | P2-53, P3-98, P3-97 | approval view/vitest · use-caller-id 관할 · background_poll_refresh_state_guard |
| ε | P2-48, P2-49, P3-92 | scheduledtask LockingPolicy(+동시성 테스트, gradle window 단독) · auth 골든 재생성 경로 · webhook 계약 |
| ζ | P2-47 | specs/{webhook,scheduled-task,email-outbox}-frontend-l0.yaml 신규 3 + allowlist flip + [100] known_gap 제거 |
| main | P3-100 + 통합 | engine coverage-report 재생성 · BACKLOG/카운트 · freeze → R25 → push → codex 게이트 |

## 규율 (remaining-19에서 실증된 것 승계)
- 레인 gradle 금지(ε만 /tmp/ax-gradle-lock spinlock), 파괴 실험은 워크트리 밖 rsync 사본,
  git stash/reset/checkout-- 전면 금지, 공유 파일(run-all·kill manifest) 등재는 main 경유.
- 모든 신규/변경 검사엔 뮤테이션 증거(RED-on-revert), fail 픽스처 + [87] 등재(불가 시 사유).
- 정직 규칙: P3-93은 network-필요 클래스 — 이번에도 오프라인이면 open 유지가 옳다.
  P3-94는 in-repo canonical(specs/auth-asvs-l1.yaml, V3.5 토큰 항목 17건)로만 재앵커 —
  무검증 § 주조 금지.
