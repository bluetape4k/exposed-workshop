# 06 Cache Strategies Coroutines Ktor

[English](./README.md) | 한국어

이 모듈은 suspending route handler, `Dispatchers.IO`로 격리한 Exposed JDBC transaction, key별 load coalescing, write-through update, 명시적 invalidation으로 coroutine-safe Ktor cache access를 보여줍니다. `CoroutineCachedProductService`는 SKU별 `Mutex`를 사용해 동시에 들어온 read-through miss가 database load 하나를 공유하도록 합니다.

## 아키텍처 다이어그램

![Ktor coroutine cache architecture](../../docs/images/readme-diagrams/11-high-performance-06-cache-strategies-coroutines-ktor-architecture-01.png)

## Coroutine 동작

- Route handler는 suspend-first 방식이며 production path에서 `runBlocking`을 사용하지 않습니다.
- Database access는 `newSuspendedTransaction(Dispatchers.IO, ...)`를 사용합니다.
- 같은 SKU에 대한 concurrent read-through 요청은 key별 `Mutex` 하나로 database load를 합칩니다.
- `StatusPages`는 `CancellationException`을 일반 error response로 변환하지 않고 다시 던집니다.

## 검증

```bash
./gradlew :06-cache-strategies-coroutines-ktor:test
```

테스트는 두 번째 조회의 cache hit, concurrent load coalescing, write-through cache refresh, invalidation, cancellation-friendly error handling을 검증합니다. Concurrent suspending request handler에서도 안전한 cache behavior가 필요한 Ktor 서비스 예제로 사용합니다.
