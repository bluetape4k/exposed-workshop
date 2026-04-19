# Cache Strategy Benchmark Report

- Profile: `smoke`
- Generated At: `2026-04-19`
- JMH Mode: Average Time (lower is better)

## 1. 캐시 전략 비교 (CacheStrategyComparisonBenchmark)

실제 PostgreSQL(Testcontainers) + HikariCP 환경에서 NoCache, ReadThrough, WriteThrough 전략을 비교합니다.

| Strategy | Workload | Payload (B) | Score (us/op) | Error | Unit |
|----------|----------|:-----------:|:-------------:|:-----:|------|
| NO_CACHE | READ_HEAVY | 256 | 547.733 | ±460.814 | us/op |
| NO_CACHE | READ_HEAVY | 4096 | 482.554 | ±91.551 | us/op |
| NO_CACHE | WRITE_HEAVY | 256 | 505.311 | ±267.411 | us/op |
| NO_CACHE | WRITE_HEAVY | 4096 | 529.370 | ±451.160 | us/op |
| READ_THROUGH | READ_HEAVY | 256 | 87.525 | ±27.023 | us/op |
| READ_THROUGH | READ_HEAVY | 4096 | 95.860 | ±107.651 | us/op |
| READ_THROUGH | WRITE_HEAVY | 256 | 478.593 | ±203.249 | us/op |
| READ_THROUGH | WRITE_HEAVY | 4096 | 497.923 | ±162.850 | us/op |
| WRITE_THROUGH | READ_HEAVY | 256 | 50.519 | ±33.834 | us/op |
| WRITE_THROUGH | READ_HEAVY | 4096 | 55.873 | ±100.879 | us/op |
| WRITE_THROUGH | WRITE_HEAVY | 256 | 448.564 | ±81.004 | us/op |
| WRITE_THROUGH | WRITE_HEAVY | 4096 | 491.862 | ±440.753 | us/op |

### 캐시 전략 효과 분석

#### READ_HEAVY 워크로드 (읽기 90% / 쓰기 10%)

**Payload 256B:**
- **READ_THROUGH**: 87.5 us/op → NoCache(547.7) 대비 **84.0% 개선**
- **WRITE_THROUGH**: 50.5 us/op → NoCache(547.7) 대비 **90.8% 개선**

**Payload 4096B:**
- **READ_THROUGH**: 95.9 us/op → NoCache(482.6) 대비 **80.1% 개선**
- **WRITE_THROUGH**: 55.9 us/op → NoCache(482.6) 대비 **88.4% 개선**

#### WRITE_HEAVY 워크로드 (읽기 10% / 쓰기 90%)

**Payload 256B:**
- **READ_THROUGH**: 478.6 us/op → NoCache(505.3) 대비 **5.3% 개선**
- **WRITE_THROUGH**: 448.6 us/op → NoCache(505.3) 대비 **11.2% 개선**

**Payload 4096B:**
- **READ_THROUGH**: 497.9 us/op → NoCache(529.4) 대비 **5.9% 개선**
- **WRITE_THROUGH**: 491.9 us/op → NoCache(529.4) 대비 **7.1% 개선**

## 2. Read-Through 캐시 Hit/Miss 비교 (ReadThroughCacheBenchmark)

인메모리 Caffeine 캐시의 hit/miss 오버헤드를 비교합니다. (DB I/O 없이 순수 캐시 계층 비용)

| Benchmark | Payload (B) | Score (us/op) | Error | Unit |
|-----------|:-----------:|:-------------:|:-----:|------|
| dbOnlyRead | 256 | 0.001 | ±0.000 | us/op |
| dbOnlyRead | 4096 | 0.001 | ±0.000 | us/op |
| readThroughCacheHit | 256 | 0.002 | ±0.002 | us/op |
| readThroughCacheHit | 4096 | 0.002 | ±0.000 | us/op |
| readThroughCacheMiss | 256 | 0.112 | ±0.327 | us/op |
| readThroughCacheMiss | 4096 | 0.134 | ±0.296 | us/op |

### 캐시 계층 오버헤드 분석

- **Cache Hit**: DB 직접 읽기와 거의 동일한 성능 (< 0.01 us 차이)
- **Cache Miss**: invalidate + reload 비용으로 ~0.1 us 추가 오버헤드
- **Payload 크기**: 256B → 4096B 변경 시 캐시 오버헤드 변화 미미

## 3. 결론

| 시나리오 | 추천 전략 | 이유 |
|----------|-----------|------|
| 읽기 중심 (90%+) | **WriteThrough** | 최대 ~90% 성능 개선, 쓰기 시 캐시 즉시 갱신 |
| 읽기 비중 높음 (70%+) | **ReadThrough** | ~80% 성능 개선, 구현 단순 |
| 쓰기 중심 (90%+) | **NoCache** 또는 WriteThrough | 캐시 효과 제한적 (~5-11%), 일관성 우선 |

> **핵심**: 읽기 비중이 높을수록 캐시 전략의 효과가 극대화됩니다.
> ReadThrough는 80-84%, WriteThrough는 88-91%의 응답시간 개선을 보여줍니다.
