---
title: "11 High Performance"
tags: [exposed, cache, redis, routing-datasource, benchmark, performance]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 11 High Performance

캐시 전략, DataSource 라우팅, 벤치마크.

## 모듈

| 모듈 | 설명 | 런타임 |
|------|------|--------|
| `01-cache-strategies` | Read/Write Through, Write Behind, Read-Only | Spring MVC + Virtual Threads |
| `02-cache-strategies-coroutines` | 동일 전략의 코루틴 버전 | WebFlux + Coroutines |
| `03-routing-datasource` | 동적 DataSource 라우팅 (멀티테넌트/Read Replica) | Spring MVC |
| `04-benchmark` | kotlinx-benchmark 기반 성능 측정 | JMH |

## 캐시 전략

| 전략 | 쓰기 | 읽기 | 적합 데이터 |
|------|------|------|----------|
| Read-Through + Write-Through | 캐시+DB 동시 | 미스 시 DB 폴백 | 수정되는 엔티티 |
| Read-Only | 없음 | 미스 시 DB 폴백 | 변경 없는 데이터 |
| Write-Behind | 캐시 즉시 + DB 비동기 | 캐시 우선 | 유실 허용 이벤트 |

## 아키텍처

Near Cache (L1 로컬) → Redis (L2 분산) → Database (Exposed)

## 실행

```bash
./gradlew :01-cache-strategies:test
./gradlew :02-cache-strategies-coroutines:test
./gradlew :03-routing-datasource:test
./gradlew :04-benchmark:smokeBenchmark
```
