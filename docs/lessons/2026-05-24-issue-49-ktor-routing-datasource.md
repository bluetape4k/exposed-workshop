# Issue 49 Ktor routing DataSource

## 배경

Issue #49는 기존 Spring routing datasource module 근처에 Ktor routing datasource 예제를
요구했다.

## 결정

Ktor plugin으로 request method 또는 `X-Data-Source`에서 `READ`나 `WRITE`를 선택하고,
repository access 전에 coroutine context element를 통해 해당 role을 bind한다.

## 결과

두 H2-backed datasource role, observable route response, selection counter, English/Korean README
file, rendered architecture diagram을 갖춘 `11-high-performance/07-routing-datasource-ktor`를
추가했다.

## 검증

통과: `repo-test-summary -- ./gradlew :07-routing-datasource-ktor:test`, passing routing
selection test 5개.

## 향후 지침

Routing datasource 예제는 test에서 selected role을 노출해야 한다. Side effect에서 routing을
추론하는 것보다 검증하기 쉽기 때문이다.
