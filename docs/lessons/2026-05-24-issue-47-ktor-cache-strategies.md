# Issue 47 Ktor cache strategy

## 배경

Issue #47은 11장 Spring cache strategy 예제에 대응되는 Ktor counterpart를 요구했다.

## 결정

예제는 framework-neutral하게 유지한다. Ktor route, 명시적인 cache strategy method를 가진
application service, Exposed JDBC persistence, test에서 hit/miss counter를 노출하는 simple
in-memory cache를 사용한다.

## 결과

Cache-aside, read-through, write-through, invalidation, English/Korean README file, rendered
architecture diagram을 갖춘 `11-high-performance/05-cache-strategies-ktor`를 추가했다.

## 검증

통과: `repo-test-summary -- ./gradlew :05-cache-strategies-ktor:test`, passing test 3개.

## 향후 지침

Workshop cache 예제에서는 사용자가 scenario의 cache hit, cache miss, database fallback 여부를
볼 수 있도록 observable route response와 counter를 선호한다.
