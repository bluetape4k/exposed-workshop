# Issue #234 Apache Druid query-only 예제 리뷰

## 리뷰 범위

- 기준 이슈: [#234](https://github.com/bluetape4k/exposed-workshop/issues/234)
- 브랜치: `feat/issue-234-druid-query-only`
- 구현 기준: `200b46b0` 이후의 현재 working tree
- 운영 제약: 단일 개발자, 순차 실행, 외부 Druid 호출 기본 비활성

## 결과

| 등급 | 개수 | 상태 |
|---|---:|---|
| P0 | 0 | 차단 사항 없음 |
| P1 | 0 | 이전 finding 해소 |
| P2 | 0 | 이전 finding 해소 |

아키텍처 및 구현 리뷰는 `CLEAR`다. 커밋·push와 exact live head/CI 확인은
PR 생성 후 delivery gate에서 별도로 마친다.

독립 리뷰 lane도 최신 exact HEAD를 확인했다. `architect`는 `CLEAR`,
`code-reviewer`는 `APPROVE`를 보고했으며 두 lane 모두 `P0=0, P1=0, P2=0`이다.

## 이전 finding과 조치

1. `DruidJdbc.query`/`querySuspend`의 `mapper = any()`만 검증하던 테스트를
   `slot<(ResultSet) -> Long>`로 바꾸고, mocked `ResultSet`에서
   `getLong("row_count")`를 실행해 alias와 반환값을 검증했다.
2. semantic ledger와 양 언어 SVG에 `Workshop query-only facade` node,
   `profile → facade`, `facade → DruidJdbc` edge를 추가하고 PNG를 재생성했다.
3. root `README.md`/`README.ko.md` ecosystem index에 모듈 링크와
   `DruidQueryProfile → DruidConnectionOptions` 설명을 추가했다.
4. 새 모듈에 `src/test/resources/junit-platform.properties`와
   `logback-test.xml`을 추가해 인접 모듈의 실행 convention을 닫았다.

## 여섯 렌즈 점검

1. **API/아키텍처** — `DruidQueryProfile`이 `DruidConnectionOptions`를 만들고,
   workshop facade가 `DruidJdbc.query`, `querySuspend`, `listColumns`에 직접
   위임한다. `Database`, dialect, table DSL, DAO, repository, migration,
   DDL/DML 경로는 추가하지 않았다. semantic audit는 7 nodes/7 edges로 통과했다.
2. **Kotlin 패턴** — immutable `data class`, `requireNotBlank`, nullable
   credential, 명시적 `CoroutineDispatcher`, Korean KDoc을 사용하며 `!!`가
   없다. `:10-druid-query-only:detekt`가 성공했다.
3. **테스트** — production code 이전 targeted RED는 query facade symbol
   unresolved compile failure였고, 수정 후 workshop test는 8 passing이다.
   `DruidQueryOnlySmokeTest`는 환경 변수가 없을 때 1 pending으로 비활성이다.
   sync/suspend mapper 결과와 provider query-only rejection을 모두 검증한다.
4. **보안/운영** — endpoint 기본값은 local Avatica이고 credential은 환경 변수
   입력만 허용한다. datasource 식별자는 단순 identifier 정규식으로 제한하며,
   real-service 호출은 `EXPOSED_DRUID_SMOKE=true`에서만 실행된다.
5. **문서/다이어그램** — module/chapter/root English·Korean README 링크와
   설명을 동기화했다. SVG/PNG는 opaque canvas와 source-equivalent locale을
   유지한다. semantic, XML, text-normalize, connector, arrowhead, geometry,
   endpoint, mixed-corner, visual audit가 양쪽에서 통과했고 targeted
   asset-pair audit도 통과했다.
6. **범위/워크플로** — 중앙 `bluetape4k-dependencies:1.4.0` alias,
   `:10-druid-query-only:build` selector, chapter/root 등록, 계획·설계·lesson을
   포함한다. workflow run `20260826T132408Z-21a64538`는 단일 `main/root` lane과
   requirements/design/plan/implementation/docs check를 기록했다.

## 검증 근거

- `./gradlew :10-druid-query-only:test --no-daemon --no-configuration-cache` —
  `8 passing`, smoke `1 pending`, `processTestResources` 실행.
- `./gradlew :10-druid-query-only:detekt --no-daemon --no-configuration-cache` —
  `BUILD SUCCESSFUL`.
- `./gradlew :10-druid-query-only:build --no-daemon --no-configuration-cache` —
  `BUILD SUCCESSFUL`.
- `./gradlew projects --no-daemon --no-configuration-cache` — `:10-druid-query-only`
  발견, `BUILD SUCCESSFUL`.
- `bash .github/scripts/select-changed-examples.sh origin/develop..HEAD` —
  `:10-druid-query-only:build` 포함.
- `git diff --check` — 통과.

## 잔여 위험

- 실제 Druid Router/Broker smoke는 자격 증명과 운영 endpoint가 필요하므로
  실행하지 않았다. 이는 의도된 opt-in 경계이며 기본 테스트 실패가 아니다.
- 전체 `docs/images/readme-diagrams`에 `--require-all-referenced`를 적용하면
  기존 자산의 pre-existing pair gap이 보고된다. 이번 변경은 no-require
  pair audit와 새 네 쌍의 명시적 존재/README PNG 참조로 검증했다.

## 판정

`P0=0, P1=0, P2=0` — 코드와 문서 범위는 merge-ready 후보이다. 단, 최종
커밋 후 push하고 live PR head, CI, review thread를 다시 읽기 전까지 delivery
상태는 `PENDING`으로 둔다.
