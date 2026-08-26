# Issue #234 Apache Druid query-only 예제 lesson

## 배경

Apache Druid를 일반적인 Exposed write model로 오인하지 않고 Avatica 기반
분석 조회 전용 provider로 사용하는 예제를 추가했다. 이슈가 요구한 provider
`1.12.1`과 중앙 catalog `bluetape4k-dependencies:1.4.0`을 고정하고, 한 명이
유지하는 workshop에 필요한 최소 경계만 남겼다.

## 결정

- `DruidQueryProfile`을 immutable typed profile로 두고
  `DruidConnectionOptions`로 변환한다.
- workshop은 `DruidJdbc.query`, `querySuspend`, `listColumns`를 직접 호출한다.
  `Database`, dialect, DDL/DML, DAO, repository, migration은 범위에서 제외했다.
- 기본 검증은 MockK object mocking으로 외부 서비스 없이 실행한다. provider
  전체를 mock하더라도 `ResultSet` mapper를 slot으로 capture해
  `getLong("row_count")` 결과까지 실행해야 위임과 매핑을 함께 증명할 수 있다.
- 실제 Druid 검증은 `EXPOSED_DRUID_SMOKE=true`일 때만 활성화하고 endpoint와
  credential은 환경 변수에서만 읽는다.
- 모듈 → chapter → root README, Examples selector, 표준 test resources를
  하나의 등록 사슬로 취급한다.
- 다이어그램은 semantic ledger에 workshop facade와 실제
  `facade → DruidJdbc` 호출을 포함하고, English/Korean SVG/PNG를 같은
  topology로 렌더링한다.

## 검증

- 설계 문서: `docs/superpowers/specs/2026-08-26-issue-234-druid-query-only-design.md`
- 실행 계획: `docs/superpowers/plans/2026-08-26-issue-234-druid-query-only-plan.md`
- 모듈 test: `8 passing`, opt-in smoke `1 pending`.
- `detekt`, module `build`, `projects`, changed-examples selector,
  `git diff --check` 성공.
- 다이어그램 semantic/geometry/endpoint/visual 및 targeted asset-pair audit 성공.

## 다음 적용 원칙

새 외부 플랫폼 예제도 provider의 실제 API를 가리는 불필요한 adapter를
추가하지 말고, typed 설정·결정적 테스트·명시적 실서비스 경계를 먼저 만든다.
등록 작업은 module README만으로 끝내지 않고 chapter/root 링크, workflow task,
test resources까지 함께 확인한다. 실제 서비스 smoke는 별도 credential과
endpoint가 준비된 경우에만 수동으로 실행한다.

이번 이슈에 등록하지 않은 후속 작업은 없다.
