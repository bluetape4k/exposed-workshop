# CTE Query Builder 예제

## 배경

`bluetape4k-exposed`가 `CteTable`과 JDBC `withCte` API를 `1.8.1-SNAPSHOT`으로
publish했으므로, workshop은 raw SQL 없이 CTE를 시연할 수 있다.

## 교훈

Workshop의 snapshot adoption은 좁게 유지한다. `exposed-workshop`은 일반 utility에
대해 여전히 main `bluetape4k` line에 의존하고, Exposed artifact만 dedicated
`bluetape4k-exposed` version key로 이동할 수 있다.

## 근거

- 기존 raw SQL `Ex50_RecursiveCTE` 옆에 `Ex51_CteQueryBuilder`를 추가했다.
- `:01-dml:test` fast path를 통해 H2에서 `CteTable`과 `withCte`를 검증했다.

## 향후 보호 장치

새로 published snapshot API의 예제를 추가할 때 snapshot이 필요한 artifact family가
하나뿐이라면 library-line version을 분리한다.
