# Issue 48 Ktor coroutine cache

## 배경

Issue #48은 기존 Spring coroutine cache module 근처에 Ktor coroutine cache 예제를 요구했다.

## 결정

Suspend route handler, JDBC work용 `newSuspendedTransaction(Dispatchers.IO, ...)`,
coroutine-friendly per-key `Mutex`, `StatusPages`에서 명시적인 cancellation rethrowing을
사용한다.

## 결과

Read-through, write-through, invalidation, concurrent request test, English/Korean README file,
rendered architecture diagram을 갖춘 `11-high-performance/06-cache-strategies-coroutines-ktor`를
추가했다.

## 검증

통과: `repo-test-summary -- ./gradlew :06-cache-strategies-coroutines-ktor:test`, concurrent
read-through load coalescing을 포함해 passing test 4개.

## 향후 지침

Coroutine cache 예제는 sequential cache hit뿐 아니라 concurrent request behavior를 증명해야
한다. Production request path에는 `runBlocking`을 두지 않는다.
