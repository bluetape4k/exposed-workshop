# Trino Session Options and Pushdown Verification

[English](README.md) | 한국어

이 예제는 Trino JDBC session 설정을 application code에서 typed configuration으로
관리하고, live Trino cluster에 연결하지 않은 채 Exposed가 생성한 분석 query를 Trino
`EXPLAIN` 검사용 request 형태로 준비하는 방법을 보여줍니다.

![Trino session options and EXPLAIN pushdown inspection sequence](../../docs/images/readme-diagrams/02-trino-session-options-sequence-01.png)

다이어그램은 local-only 경로를 보여줍니다. 검증된 workshop profile을
`TrinoConnectionOptions`로 매핑하고, Exposed가 warehouse query를 구성하며, H2는 SQL
generation용 transaction context로만 사용됩니다. 생성된 SQL은 이후 pushdown 점검에 쓸
수 있도록 `EXPLAIN` request 형태로 감쌉니다.

## 목적

이 모듈은 application configuration과 Trino JDBC driver property 사이의 경계를 다룹니다.
실제 warehouse connection을 열기 전에 catalog, schema, `source`, `clientTags`,
`sessionProperties`를 application code에서 안정적으로 소유해야 할 때 참고할 수 있습니다.

또한 projection, predicate, ordering, top-N clause가 있는 pushdown-friendly query 형태를
보여줍니다. 실제 pushdown 지원 여부는 Trino connector와 catalog 설정에 따라 달라지므로,
workshop은 request shape만 검증합니다.

## Session Options

`TrinoWorkshopConnectionProfile`은 사용자-facing 값을 검증하고 bluetape4k-exposed의
`TrinoConnectionOptions` API로 변환합니다.

- `explicitPrepare=false`
- `encoding=json+zstd`
- `validateConnection=true`
- `source=exposed-workshop`
- `clientTags=exposed,analytics,workshop`
- `sessionProperties=join_distribution_type=AUTOMATIC,query_max_execution_time=5m`

`jdbcPropertyPreview(user)` helper는 test와 documentation을 위한 local preview입니다.
실제 JDBC property 변환은 bluetape4k-exposed library의 `TrinoConnectionOptions` 안에
남겨 둡니다.

## Credential-Free Command

예제 테스트를 실행합니다.

```bash
./gradlew :02-trino-session-options:test
```

예상 결과: 이 명령은 public option object와 in-memory H2 SQL-generation transaction만
사용합니다. Trino coordinator URL, catalog credential, environment variable, endpoint
override, network access 없이 통과해야 합니다.

## Tested Behavior

테스트는 다음 동작을 검증합니다.

- 기본 profile 값이 typed `TrinoConnectionOptions`로 매핑됩니다.
- documentation과 assertion을 위한 안정적인 JDBC-property preview를 제공합니다.
- 빈 catalog, schema, source, tag, session property 값은 JDBC connection 시도 전에 실패합니다.
- 생성된 SQL이 이후 Trino `EXPLAIN` 검사에 필요한 `SELECT`, `WHERE`, `ORDER BY`,
  `LIMIT 10` signal을 유지합니다.

## Real Trino Out of Scope

이 모듈은 Trino를 시작하거나, coordinator에 연결하거나, catalog 인증을 수행하거나,
connector-specific pushdown 결과를 assertion하지 않습니다. 향후 real-service lane이
필요하면 명시적 opt-in test profile을 사용하고, Trino plan 전체를 snapshot하지 말고
안정적인 `EXPLAIN` signal만 비교해야 합니다.
