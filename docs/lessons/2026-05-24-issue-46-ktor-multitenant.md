# Issue 46 Ktor multitenant 예제

## 배경

Issue #46은 10장 Spring multi-tenant 예제에 대응되는 Ktor equivalent를 요구했다.

## 결정

Tenant header validation에는 Ktor plugin을, request-scoped tenant binding에는 coroutine
`ThreadContextElement`를 사용한다. Schema switching은 Exposed repository transaction 안에서
명시적으로 유지한다.

## 결과

H2-backed tenant schema, focused route test, English/Korean README file, reusable architecture
diagram을 갖춘 `10-multi-tenant/07-multitenant-ktor`를 추가했다.

## 검증

통과: `repo-test-summary -- ./gradlew :07-multitenant-ktor:test`, passing test 4개.

## 향후 지침

Ktor tenant 예제에서는 tenant resolution을 repository schema switching과 분리해 test가
missing header, invalid tenant, cleanup, isolation behavior를 독립적으로 증명할 수 있게 한다.
