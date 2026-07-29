# Issue 188 DuckDB embedded analytics lesson

## 배경

Ecosystem integration blog backlog는 article writing 전에 source-backed example을 더 필요로
했다. DuckDB는 remote service, credential, Docker, cloud setup이 필요 없는 local analytics
lane을 추가한다.

## 결정

File-backed DuckDB database를 사용하고 workshop session 동안 root `DuckDBConnection` 하나를
열어 둔다. Exposed는 duplicated transaction connection을 받아 별도 transaction이 같은 local
database를 관찰하게 한다. README는 `queryFlow`가 transaction 밖 raw JDBC streaming이 아니라
transaction materialization 이후 coroutine consumption boundary임을 명시한다.

## 결과

모듈은 schema creation, batch insert, aggregate projection, rendered SQL shape, validation
failure, Flow consumption을 local에서 검증한다. README pair는 SVG source와 PNG render가 있는
architecture, example-flow, sequence diagram을 embed한다.

## 향후 지침

DuckDB follow-up 예제에서는 Parquet/CSV scan, Arrow integration, extension loading을 별도
모듈로 추가한다. 이 모듈은 Exposed + embedded analytics boundary에 집중시키고 real-service
assumption을 도입하지 않는다.
