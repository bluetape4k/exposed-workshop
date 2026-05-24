# 05 Cache Strategies Ktor

[English](./README.md) | 한국어

이 모듈은 Ktor route, Exposed JDBC persistence, in-memory cache로 cache-aside, read-through, write-through, 명시적 invalidation 흐름을 보여줍니다.

## 아키텍처 다이어그램

![Ktor cache strategy architecture](../../docs/images/readme-diagrams/11-high-performance-05-cache-strategies-ktor-architecture-01.png)

## Route

| Route | 전략 | 동작 |
|---|---|---|
| `GET /users/{id}/cache-aside` | Cache-aside | Cache를 먼저 확인하고 miss이면 database에서 읽은 뒤 cache를 채웁니다. |
| `GET /users/{id}/read-through` | Read-through | Service가 database fallback을 소유하고 이후 요청에는 cached value를 반환합니다. |
| `PUT /users/{id}/write-through` | Write-through | 하나의 application operation에서 database와 cache를 함께 갱신합니다. |
| `DELETE /users/{id}/cache` | Invalidation | Cache entry를 제거해 다음 read가 database fallback을 수행하게 합니다. |
| `GET /cache/stats` | Observability | Database read, hit, miss, cache size counter를 반환합니다. |

## 검증

```bash
./gradlew :05-cache-strategies-ktor:test
```

Spring Cache abstraction 없이 Ktor 서비스에서 명시적이고 테스트 가능한 cache behavior가 필요할 때 이 예제를 사용합니다.
