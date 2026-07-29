# Issue 141 - StarRocks local-first OLAP 설계

## 배경

Issue #141은 `bluetape4k-exposed-starrocks` helper의 local-testability boundary를 가르치는
workshop example을 요구한다. 이 예제는 StarRocks를 default CI dependency로 만들지 않으면서
StarRocks-oriented DDL과 analytical projection shape를 보여 줘야 한다.

## 결정

`13-ecosystem-integrations/04-starrocks-olap-local`을 만든다.

- Connector/J boundary를 검증하고 예상 `jdbc:starrocks://host:port/catalog.database` URL을
  rendering하기 위해 `StarRocksAnalyticsProfile`을 사용한다.
- Local DDL rendering에서 `ENGINE=OLAP`와 `replication_num = 1`이 보이도록 target rollup
  table에는 `StarRocksTable`을 사용한다.
- H2는 local deterministic projection test를 위한 transaction context와 fixture store로만
  사용한다.
- Real StarRocks behavior는 default test path 밖에 두고 explicit opt-in으로 문서화한다.

## 수용 기준 매핑

- Default test는 local/deterministic 상태를 유지한다: H2-only test, Docker 없음.
- README locale pair는 local 및 real StarRocks boundary를 문서화한다.
- Test는 DDL/query shape와 실제 local aggregation scenario를 검증한다.
- Diagram asset은 SVG와 rendered PNG로 다음 위치에 제공된다.
  `docs/images/readme-diagrams/`.
- Examples workflow는 `:04-starrocks-olap-local:build`를 포함한다.

## 위험

- DDL rendering은 structure만 검증한다. StarRocks storage나 distribution behavior의 증거가
  아니다.
- Real StarRocks validation은 fast CI 안에 backend prerequisite를 숨기지 않도록 별도 opt-in
  lane으로 유지해야 한다.
