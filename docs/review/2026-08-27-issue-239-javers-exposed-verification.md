# 이슈 #239 JaVers + Exposed 감사 이력 예제 검증 보고서

## 검증 대상

- 이슈: [#239](https://github.com/bluetape4k/exposed-workshop/issues/239)
- 작업 브랜치: `feat/issue-239-javers-exposed`
- 코드 검증 기준 커밋: `ed71c0fec27bbf378438a789bcb36fae3da8d25d`
- 범위: `13-ecosystem-integrations/12-javers-exposed-audit`의 JDBC 예제와 문서·다이어그램·예제 선택기 연동
- 제외 범위: R2DBC 구현. R2DBC는 `exposed-r2dbc-workshop#235`에서 다룬다.

## 구현 DoD 검증

| 항목 | 증거 | 결과 |
|---|---|---|
| Gradle 모듈 인식 | `./gradlew projects` 결과에 `:12-javers-exposed-audit` 포함 | PASS |
| BOM/provider 정합성 | `dependencyInsight`에서 `io.github.bluetape4k.javers:javers-exposed:0.3.0 (selected by rule)` 및 BOM 경로 확인 | PASS |
| JDBC 감사 흐름 | 생성·수정 변경 기준 데이터, diff, history와 actor/requestId/changeType 검증 | PASS |
| 트랜잭션 원자성 | 커밋 전 예외 시 비즈니스 행·commit·변경 기준 데이터가 남지 않음 | PASS |
| 중복 억제 | 변경 없는 재커밋에서 변경 기준 데이터와 commit 증가 없음 | PASS |
| 민감정보 제외 | JaVers raw `state`와 `changedProperties`에 `secret`이 없음 | PASS |
| 컨텍스트 수명 | 중첩·예외 cleanup, context 없는 fail-closed, idempotent close 검증 | PASS |
| 데이터베이스 격리 | 전역 hook이 다른 `Database` 이벤트를 거부하고 교차 DB 행을 남기지 않음 | PASS |
| 전역 hook 소유권 | 중복 subscription을 거부하고 `close()` 뒤 재구독 가능 | PASS |
| JaVers 저장소 결합 | 다른 `Database`로 만든 JaVers instance를 구독 단계에서 거부 | PASS |
| 문서/자산 | EN/KO README, 아키텍처·시퀀스·ERD SVG/PNG, 선택기 등록 | PASS |

## 자동 검증 결과

작업 브랜치에서 다음 명령을 실행했고 모두 성공했다.

```text
./gradlew :12-javers-exposed-audit:test --tests \
  'exposed.examples.javers.audit.JaversExposedAuditWorkshopTest' \
  --no-daemon --no-configuration-cache
```

- `JaversExposedAuditWorkshopTest`: 12개 테스트 성공
- Gradle 결과: `BUILD SUCCESSFUL`

```text
./gradlew :12-javers-exposed-audit:detekt \
  :12-javers-exposed-audit:build --no-daemon --no-configuration-cache
```

- detekt, 컴파일, 테스트, Kover 산출 단계 성공
- Gradle 결과: `BUILD SUCCESSFUL`

```text
./gradlew :12-javers-exposed-audit:koverXmlReport \
  --no-daemon --no-configuration-cache
```

- XML 산출물: `13-ecosystem-integrations/12-javers-exposed-audit/build/reports/kover/report.xml`
- Gradle 결과: `BUILD SUCCESSFUL`

```text
./gradlew :12-javers-exposed-audit:dependencyInsight \
  --dependency javers-exposed \
  --configuration testRuntimeClasspath \
  --no-daemon --no-configuration-cache
```

- provider 버전 `0.3.0`이 중앙 BOM 규칙으로 선택됨
- Gradle 결과: `BUILD SUCCESSFUL`

```text
actionlint .github/workflows/examples.yml
```

- 결과: `ACTIONLINT_PASS`

```text
FORCE_ALL=false .github/scripts/select-changed-examples.sh \
  "$(git rev-parse HEAD^)...$(git rev-parse HEAD)"
```

- `all=true`
- `:12-javers-exposed-audit:build`가 선택 대상에 포함됨

```text
git diff --check
```

- 공백·패치 오류 없음

## 다이어그램·문서 검증

- SVG/PNG 쌍 3종(아키텍처·시퀀스·ERD), EN/KO 합계 6개를 생성했다.
- 시맨틱 원장 3개가 모두 `ok: true`였다.
- SVG 텍스트 정규화: `files=6 text_hazards=0 code_without_highlight=0 changed=0`
- arrowhead, endpoint, connector, geometry, mixed-corner 감사가 EN/KO 6개 모두 PASS였다.
- sequence style 감사는 시퀀스 SVG 2개에 한정해 PASS였다.
- 최종 PNG 시각 검증에서 6개 모두 레이아웃 실패가 없었고 KO 글꼴도 판독 가능했다.
- 모듈 README의 EN/KO 링크와 자산 경로를 확인했으며 Mermaid/Graphviz 잔여 블록이 없다.
- README는 database identity guard, 단일 global-hook subscription 소유권, raw
  `javers.commit` 우회 금지, actor 인증·조회 권한 책임, 교육용 평문 `secret` 경계를
  EN/KO에 동등하게 설명한다.
- 전체 공유 자산 디렉터리에 `--require-all-referenced`를 적용하면 기존 다른 예제의 미참조 자산 때문에 실패하므로 이 모듈의 DoD로 사용하지 않았다. 모듈 README 기준 자산 쌍·링크 검증은 PASS다.
- 한국어 용어 감사 결과: 8개 문서, findings 0.

## CI/실행 범위 판단

- `examples.yml`은 chapter 13 경로를 포괄하고, Nightly H2 매트릭스는 모듈 목록을 동적으로 탐색하므로 별도 Nightly 행을 추가하지 않았다.
- 로컬에서는 H2를 사용해 결정적 JDBC 검증을 수행했다.
- PostgreSQL/MySQL 원격 매트릭스, 성능 벤치마크, 코루틴/R2DBC 검증은 이 이슈의 범위가 아니며 각각 별도 실행·저장소에서 다룬다.
- 초기 테스트 컴파일 시도는 120초 제한으로 색인 단계에서 종료됐다. 이후 database
  guard·구독 소유권·현재 상태 계약을 반영한 최종 강제 재실행에서 12개 성공과
  `BUILD SUCCESSFUL`을 확인했으며, 초기 중단은 최종 결과에 포함하지 않는다.

## 남은 게이트

- 독립 정확성·보안·완료성 검토 결과는
  `docs/review/2026-08-27-issue-239-javers-exposed-final-review.md`에 반영했고,
  초기 P1은 모두 수정 후 재검증했다.
- PR 생성 후 실제 PR head 기준 CI·review·thread·메타데이터를 다시 읽는다.
- 머지는 PR head와 CI/review를 새로 확인한 뒤 별도의 최신 `승인`이 있을 때만 수행한다.
