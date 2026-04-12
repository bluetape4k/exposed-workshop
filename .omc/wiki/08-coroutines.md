---
title: "08 Coroutines & Virtual Threads"
tags: [exposed, coroutines, virtual-threads, async, concurrency]
category: architecture
created: 2026-04-13
updated: 2026-04-13
---

# 08 Coroutines & Virtual Threads

코루틴과 Virtual Thread 기반 동시성 모델 비교.

## 비교

| 항목 | Kotlin Coroutines | Virtual Threads (Java 21+) |
|------|-------------------|---------------------------|
| API | `newSuspendedTransaction`, `suspendedTransactionAsync` | `newVirtualThreadJdbcTransaction`, `virtualThreadJdbcTransactionAsync` |
| 코드 스타일 | `suspend` 함수 | 블로킹 스타일 유지 |
| 취소 | `Job.cancel()` + 구조적 동시성 | `Thread.interrupt()` |
| 마이그레이션 | `suspend` 키워드 추가 필요 | 블로킹 코드 그대로 사용 |
| 적합 사례 | WebFlux, 신규 비동기 코드 | 기존 동기 코드 확장 |

## 모듈

- `01-coroutines-basic` — `newSuspendedTransaction`, `suspendedTransactionAsync` 병렬 실행
- `02-virtualthreads-basic` — `newVirtualThreadJdbcTransaction`, 비동기 병렬, 중첩 트랜잭션

## 실행

```bash
./gradlew :01-coroutines-basic:test
./gradlew :02-virtualthreads-basic:test
```
