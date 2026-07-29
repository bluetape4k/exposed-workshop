# WIP 감사와 Routing Datasource lifecycle

## 배경

2026-05-18 GNO-backed exposed-workshop audit는 이전 README/WIP refresh lesson,
live GitHub issue 상태, 현재 source marker를 비교했다.

## 결정 또는 발견

11장 routing datasource 예제는 tenant-owned `HikariDataSource` instance를
등록하지만, registry나 Spring lifecycle hook에 close ownership을 주지 않았다.
기존 config에는 `@PreDestroy` cleanup TODO가 있고, unit test가 `finally`에서 pool을
수동으로 닫고 있어 명시적 cleanup이 필요함을 확인했다.

## 결과

GitHub issue #70을 등록하고 `WIP.md`를 할당 이슈 0건에서 live assigned issue
20건으로 갱신했다.

## 검증

- `gno query ... --no-rerank -c bluetape4k-docs` surfaced the prior
  exposed-workshop WIP refresh lesson.
- `gh issue list --assignee debop` confirmed 20 live assigned open issues after
  registering #70.
- `gh issue list --search "Hikari routing datasource close PreDestroy"` found
  no duplicate.
- `./gradlew :03-routing-datasource:test --tests "exposed.examples.routing.config.RoutingDataSourceConfigTest"`
  completed with `BUILD SUCCESSFUL` and `2 passing`.

## 향후 지침

Datasource-routing 예제에서는 sample 자체에 resource shutdown ownership을 정의한다.
production Spring context shutdown에 deterministic cleanup이 아직 없다면 test의
수동 pool close에 기대지 않는다.
