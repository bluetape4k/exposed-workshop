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
./gradlew :01-cache-strategies:test
./gradlew :01-cache-strategies:bootRun
```

## 실습 체크리스트

- 캐시 히트/미스 시 응답 시간 차이를 측정
- Write Through 저장 후 DB 즉시 반영 여부 검증
- Write Behind 대량 적재 후 최종 반영 수를 검증

## 운영 체크포인트 (성능·안정성)

- 캐시 무효화 정책(TTL/수동 무효화)과 데이터 신선도 SLA를 정렬
- Write Behind는 유실 허용 데이터에 제한적으로 적용
- Redis 장애 시 DB 폴백 경로를 필수 검증

## 복잡한 시나리오

### Read-Through + Write-Through 흐름 (User)

`UserCacheRepository`는 캐시 미스 시 DB를 조회해 캐시에 적재(Read-Through)하고, 엔티티 갱신 시 DB와 캐시에 동시 반영(Write-Through)합니다. 두 번째 조회부터는 Redis에서 직접 반환돼 DB 부하가 줄어드는 것을 시간 측정으로 확인할 수 있습니다.

- 관련 파일: [`domain/repository/UserCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/domain/repository/UserCacheRepository.kt)
- 검증 테스트: [`domain/repository/UserCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/domain/repository/UserCacheRepositoryTest.kt), [`controller/UserControllerTest.kt`](src/test/kotlin/exposed/examples/cache/controller/UserControllerTest.kt)

### Write-Behind 대량 이벤트 비동기 반영 (UserEvent)

`UserEventCacheRepository`는 이벤트를 Redis에 선반영한 뒤 비동기로 DB에 일괄 저장(Write-Behind)합니다. 대량 적재(`bulk insert`) 후 Awaitility로 최종 DB 반영 수를 검증하는 시나리오에서 지연 허용 범위를 확인할 수 있습니다.

- 관련 파일: [`domain/repository/UserEventCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/domain/repository/UserEventCacheRepository.kt)
- 검증 테스트: [`domain/repository/UserEventCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/domain/repository/UserEventCacheRepositoryTest.kt), [`controller/UserEventControllerTest.kt`](src/test/kotlin/exposed/examples/cache/controller/UserEventControllerTest.kt)

### 캐시 무효화 (UserCredentials)

`UserCredentialsCacheRepository`는 Read-Only 캐시 전략을 적용하며, 특정 ID 목록의 캐시를 명시적으로 무효화하는 API를 제공합니다. 무효화 후 재조회 시 DB에서 최신값을 가져오는 흐름을 검증합니다.

- 관련 파일: [`domain/repository/UserCredentialsCacheRepository.kt`](src/main/kotlin/exposed/examples/cache/domain/repository/UserCredentialsCacheRepository.kt)
- 검증 테스트: [`domain/repository/UserCredentialsCacheRepositoryTest.kt`](src/test/kotlin/exposed/examples/cache/domain/repository/UserCredentialsCacheRepositoryTest.kt), [`controller/UserCredentialsControllerTest.kt`](src/test/kotlin/exposed/examples/cache/controller/UserCredentialsControllerTest.kt)

## 다음 모듈

- [`../02-cache-strategies-coroutines/README.md`](../02-cache-strategies-coroutines/README.md)
