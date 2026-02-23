# 캐시 전략 (01-cache-strategies)

Spring MVC + Virtual Threads 환경에서 Redisson + Exposed로 캐시 전략을 실습하는 모듈입니다. Read Through, Write Through, Write Behind 전략의 일관성/성능 트레이드오프를 비교합니다.

## 학습 목표

- 캐시 전략별 동작과 트레이드오프를 구분한다.
- Redis + DB 동기화 시점에 따른 일관성 모델을 이해한다.
- 운영에서 필요한 무효화/복구 시나리오를 검증한다.

## 선수 지식

- [`../09-spring/README.md`](../09-spring/README.md)

## 핵심 개념

- Read Through: 미스 시 DB 조회 후 캐시 적재
- Write Through: 캐시 쓰기와 DB 동기 반영
- Write Behind: 캐시 선반영 후 DB 비동기 반영
- Read-Only Cache: 읽기 중심 데이터 최적화

## 주요 구성 요소

| 파일/영역                                                 | 설명                 |
|-------------------------------------------------------|--------------------|
| `domain/repository/UserCacheRepository.kt`            | Read/Write Through |
| `domain/repository/UserCredentialsCacheRepository.kt` | Read-Only Cache    |
| `domain/repository/UserEventCacheRepository.kt`       | Write Behind       |
| `config/RedissonConfig.kt`                            | Redis/캐시 설정        |
| `config/TomcatVirtualThreadConfig.kt`                 | Virtual Thread 설정  |

## 실행 방법

```bash
./gradlew :exposed-11-high-performance-01-cache-strategies:test
./gradlew :exposed-11-high-performance-01-cache-strategies:bootRun
```

## 실습 체크리스트

- 캐시 히트/미스 시 응답 시간 차이를 측정
- Write Through 저장 후 DB 즉시 반영 여부 검증
- Write Behind 대량 적재 후 최종 반영 수를 검증

## 운영 체크포인트 (성능·안정성)

- 캐시 무효화 정책(TTL/수동 무효화)과 데이터 신선도 SLA를 정렬
- Write Behind는 유실 허용 데이터에 제한적으로 적용
- Redis 장애 시 DB 폴백 경로를 필수 검증

## 다음 모듈

- [`../02-cache-strategies-coroutines/README.md`](../02-cache-strategies-coroutines/README.md)
