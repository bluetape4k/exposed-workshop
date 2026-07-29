# Issue 141 - StarRocks local-first OLAP lesson

## 배경

StarRocks 예제는 Testcontainers-backed integration test로 다루고 싶어지기 쉽지만, issue #141은
local-first OLAP boundary를 요구했다.

## 결정

Default workshop test lane은 local 및 deterministic 상태로 유지한다. SQL rendering과 fixture
aggregation에는 H2를 사용하고, StarRocks-specific DDL shape는 `StarRocksTable`로 보여 준다.

## 결과

예제는 Docker나 network access 없이 typed connection setting, OLAP DDL rendering, query shape,
aggregation을 증명한다. README file은 별도의 real StarRocks validation boundary를 문서화한다.

## 향후 보호 장치

나중 issue가 heavyweight opt-in 또는 nightly lane을 명시적으로 요구하지 않는 한 default
workshop CI path에 StarRocks container를 추가하지 않는다.
