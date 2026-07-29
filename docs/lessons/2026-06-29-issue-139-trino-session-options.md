# Issue 139 Trino session option lesson

## 배경

Issue #139는 13장 아래 credential-free Trino workshop 예제를 추가한다.

## 결정

Workshop은 public API 위에 유지한다. Application-facing profile을 검증하고
`TrinoConnectionOptions`로 변환하며, README와 test에는 local JDBC-property preview만 노출한다.
`bluetape4k-exposed`의 internal property conversion helper는 호출하지 않는다.

## 결과

예제는 typed option과 EXPLAIN request shape를 local에서 검증한다. Trino endpoint나 credential을
요구하지 않으며 connector-specific pushdown behavior를 assert하지 않는다.

## 향후 지침

Real Trino lane에서는 명시적인 opt-in test를 사용하고 known connector의 stable EXPLAIN
fragment를 비교한다. Trino plan은 connector, version, catalog setting에 따라 달라지므로
full-plan snapshot은 피한다.
