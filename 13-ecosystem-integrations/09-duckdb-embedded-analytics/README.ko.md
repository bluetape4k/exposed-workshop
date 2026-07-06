# DuckDB Embedded Analytics with Exposed

[English](README.md) | 한국어

이 예제는 DuckDB와 Exposed로 작은 분석 작업을 애플리케이션 프로세스 안에서 실행하는
방법을 보여줍니다. BigQuery, Trino, StarRocks 예제와 달리 여기서 DuckDB는 원격
warehouse를 호출하는 경계가 아닙니다. 파일 기반 DuckDB database를 열고 로컬 이벤트 row를
저장한 뒤 집계 query를 실행해서, 결과를 materialized list 또는 coroutine `Flow`로
넘깁니다.

![DuckDB embedded analytics architecture](../../docs/images/readme-diagrams/13-duckdb-embedded-analytics-architecture-01.png)

아키텍처 다이어그램은 세 가지 책임을 분리합니다.

- 애플리케이션 코드는 event fixture와 Exposed DSL을 다룹니다.
- `DuckDbAnalyticsSession`은 DuckDB root connection을 유지하고, Exposed transaction에는
  duplicated connection을 넘깁니다.
- DuckDB는 server process 없이 local file 안에서 analytical SQL을 실행합니다.

## 목적

DuckDB는 개발, CI, data preparation, 작은 offline job처럼 애플리케이션 가까이에서 분석
엔진이 필요할 때 유용합니다. 모든 warehouse를 대체하자는 이야기는 아닙니다. 다만 이
부류의 예제에서는 network와 credential 경계를 걷어내고 분석 query 자체에 집중할 수
있습니다.

이 모듈은 다음 내용을 보여줍니다.

- `openDuckDbAnalyticsSession(path)`로 file-backed DuckDB session을 여는 방법.
- Exposed 기반 schema 생성과 batch insert.
- `COUNT`, `SUM`, `GROUP BY`, `ORDER BY`를 포함한 집계 query.
- coroutine 코드에서 blocking DuckDB JDBC 작업을 실행하는 `suspendTransaction`.
- transaction 안에서 안전하게 materialize한 뒤 애플리케이션 pipeline으로 넘기는
  `queryFlow`.

## 예제 흐름

![DuckDB embedded analytics example flow](../../docs/images/readme-diagrams/13-duckdb-embedded-analytics-flow-01.png)

흐름은 고정된 `DuckDbOrderEvent` row에서 시작합니다. 이 row를 file-backed DuckDB
table에 저장하고 `DailyCategorySales` row로 projection합니다. 같은 query는 실행하지
않고 SQL로만 렌더링해 모양을 확인할 수도 있습니다.

```kotlin
openDuckDbAnalyticsSession(Path.of("/tmp/orders.duckdb")).use { session ->
    val db = session.db

    seedDuckDbOrderEvents(
        db = db,
        events = listOf(
            DuckDbOrderEvent(1001L, "apac", "book", "2026-06-29", BigDecimal("42.50")),
            DuckDbOrderEvent(1002L, "apac", "book", "2026-06-29", BigDecimal("10.00")),
        ),
    )

    val rows = projectDailyCategorySales(db)
    val sql = generateDailyCategorySalesSql(db)
}
```

## Sequence

![DuckDB embedded analytics sequence](../../docs/images/readme-diagrams/13-duckdb-embedded-analytics-sequence-01.png)

Sequence diagram은 헷갈리기 쉬운 지점을 강조합니다. `queryFlow`는 `Flow`를 반환하지만,
DuckDB JDBC row는 transaction 안에서 먼저 materialize됩니다. 즉 이 Flow는 coroutine에
잘 붙이기 위한 소비 경계이지, JDBC transaction 밖에서 row-by-row streaming을 한다는
뜻은 아닙니다.

## 파일 기반 모드

이 예제는 의도적으로 file-backed database를 사용합니다.

```kotlin
openDuckDbAnalyticsSession(Path.of("/tmp/orders.duckdb")).use { session ->
    val db = session.db
    // use db across separate Exposed transactions
}
```

`DuckDBDatabase.inMemory()`는 JDBC connection마다 독립적인 in-memory database를 만듭니다.
한 transaction 안에서만 쓴다면 괜찮지만, 여러 Exposed transaction 사이에서 row가 유지되어야
하는 예제의 기본값으로는 맞지 않습니다. file-backed DuckDB database를 쓰고 root
connection을 유지하면 예제 동작을 예측하기 쉽습니다. Exposed transaction에는
duplicated connection을 넘기고, 예제가 끝나면 session이 root connection을 닫습니다.

## 로컬 검증 경계

모듈 테스트를 실행합니다.

```bash
./gradlew :09-duckdb-embedded-analytics:test
```

이 명령은 Docker를 시작하지 않고, credential도 사용하지 않으며, remote service에도
접속하지 않습니다. schema 생성, file-backed persistence, aggregate result, rendered
SQL shape, validation failure, `queryFlow` 소비 경계를 검증합니다.

## 검증한 동작

테스트는 다음을 검증합니다.

- file-backed DuckDB row가 별도 Exposed transaction 사이에서도 유지됩니다.
- `DailyCategorySales` aggregation이 deterministic하게 계산됩니다.
- `streamDailyCategorySales`가 materialized aggregate row를 `Flow`로 emit합니다.
- rendered SQL에 `COUNT`, `SUM`, `GROUP BY`, `ORDER BY`가 드러납니다.
- 비어 있는 event field와 empty fixture input은 DuckDB insert 경계에 도달하기 전에 실패합니다.

## 다루지 않는 내용

이 모듈은 Parquet/CSV scan, Arrow integration, extension loading, vectorized execution
tuning, multi-process file locking, large result pagination을 다루지 않습니다. 모두
유용한 DuckDB 주제지만, 이 예제는 Exposed + embedded analytics 경계에 집중합니다.
