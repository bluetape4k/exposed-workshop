# StarRocks Local-First OLAP

[English](README.md) | 한국어

이 예제는 StarRocks 지향 OLAP projection 코드를 기본값으로 StarRocks cluster 없이
테스트하는 방법을 보여줍니다. `bluetape4k-exposed-starrocks` helper surface로
StarRocks JDBC 경계와 table DDL 형태를 드러내고, 기본 테스트는 H2로 SQL 생성과
aggregation을 검증합니다.

![StarRocks local-first OLAP boundary](../../docs/images/readme-diagrams/04-starrocks-olap-local-architecture-01.png)

다이어그램은 세 가지 경계를 분리합니다.

- CI에서 StarRocks 없이 실행되는 local deterministic projection test.
- JDBC option과 OLAP table DDL 형태를 소유하는 StarRocks helper API.
- 명시적으로 opt-in 해야 하는 실제 StarRocks validation.

## 목적

OLAP connector는 성능, 분산, 저장소 동작을 신뢰하려면 결국 실제 analytical backend로
검증해야 합니다. 하지만 모든 workshop test가 그 backend를 기본으로 시작할 필요는
없습니다. 이 모듈은 기본 feedback loop를 local deterministic 경로로 유지합니다.

- `StarRocksAnalyticsProfile`은 network connection을 열기 전에 Connector/J 경계를
  검증합니다.
- `StarRocksRegionalSalesRollups`는 `StarRocksTable`을 확장해 생성된 DDL에서
  StarRocks `ENGINE=OLAP`과 replication option이 보이도록 합니다.
- `LocalOrderEvents`는 projection과 SQL rendering을 위한 local fixture store로만 H2를
  사용합니다.
- `projectDailyRegionalRevenue`는 실제 StarRocks smoke test를 별도 lane에서 켜기 전에
  aggregation 형태를 먼저 검증합니다.

## Local Validation Boundary

기본 테스트를 실행합니다.

```bash
./gradlew :04-starrocks-olap-local:test
```

예상 결과: 이 명령은 Docker를 시작하지 않고 StarRocks에도 접속하지 않습니다. typed
connection option, StarRocks rollup DDL, regional aggregation SQL, H2 fixture 기반
aggregation을 검증합니다.

## Real StarRocks Boundary

실제 StarRocks validation lane이 필요하면 profile이 만든 URL을 사용합니다.

```kotlin
val profile = defaultStarRocksAnalyticsProfile()

val jdbcUrl = profile.jdbcUrl()
val options = profile.toConnectionOptions()
```

기본 URL은 다음과 같습니다.

```text
jdbc:starrocks://localhost:9030/default_catalog.analytics
```

먼저 StarRocks database를 만든 뒤, `StarRocksDatabase`로 연결하는 별도 opt-in
validation을 실행합니다. 이 workshop은 해당 조건을 기본 CI 경로 뒤에 숨기지 않습니다.

## Projection Example

```kotlin
val db = createLocalProjectionDatabase()

seedLocalOrderEvents(
    db = db,
    events = listOf(
        OrderEvent(1001L, "apac", "2026-06-29", BigDecimal("42.50")),
        OrderEvent(1002L, "apac", "2026-06-29", BigDecimal("17.25")),
    ),
)

val rollups = projectDailyRegionalRevenue(db)
```

같은 projection query는 실행하지 않고 SQL로만 렌더링할 수도 있습니다.

```kotlin
val sql = generateDailyRegionalRevenueSql()
```

나중에 실제 StarRocks validation query와 비교할 contract로 이 SQL을 사용할 수 있습니다.

## Tested Behavior

테스트는 다음을 검증합니다.

- connection profile validation이 blank host, catalog, database, user와 범위를 벗어난
  port를 network access 전에 잡아냅니다.
- StarRocks rollup DDL에 `ENGINE=OLAP`과 `replication_num = 1`이 포함됩니다.
- 생성된 SQL에서 projection, `SUM`, `COUNT`, `GROUP BY`, `ORDER BY`가 보입니다.
- local fixture data가 deterministic regional revenue rollup으로 집계됩니다.

## Out of Scope

이 모듈은 StarRocks distribution key, partitioning, stream load, external catalog,
storage engine 성능, StarRocks Cloud 동작을 검증하지 않습니다. 그런 검증은 실제
StarRocks lane이 필요하며 명시적 opt-in으로 유지하는 편이 맞습니다.
