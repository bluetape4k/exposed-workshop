# 캐시 전략 - Coroutines (02-cache-strategies-coroutines)

`01-cache-strategies`의 코루틴/논블로킹 버전입니다. WebFlux + Netty + Coroutines 환경에서 suspend 기반 캐시 접근 패턴을 실습합니다.

## 학습 목표

- suspend 기반 캐시/DB 접근 패턴을 익힌다.
- 이벤트 루프 친화적인 캐시 처리 모델을 구현한다.
- 동시 연결이 많은 상황에서 안정성을 검증한다.

## 선수 지식

- [`../08-coroutines/README.md`](../08-coroutines/README.md)
- [`../01-cache-strategies/README.md`](../01-cache-strategies/README.md)

## 핵심 개념

- `AbstractSuspendedExposedCacheRepository` 기반 비동기 접근
- Controller `suspend` 엔드포인트
- Netty 이벤트 루프/커넥션 풀 튜닝

## 주요 구성 요소

| 파일/영역                                                 | 설명                           |
|-------------------------------------------------------|------------------------------|
| `domain/repository/UserCacheRepository.kt`            | Suspended Read/Write Through |
| `domain/repository/UserCredentialsCacheRepository.kt` | Suspended Read-Only          |
| `domain/repository/UserEventCacheRepository.kt`       | Suspended Write Behind       |
| `config/NettyConfig.kt`                               | Netty 튜닝                     |
| `controller/*Controller.kt`                           | suspend API 엔드포인트            |

## 실행 방법

```bash
./gradlew :exposed-11-high-performance-02-cache-strategies-coroutines:test
./gradlew :exposed-11-high-performance-02-cache-strategies-coroutines:bootRun
```

## 실습 체크리스트

- suspend 경로에서 캐시 적중/미스 동작을 검증
- 대량 이벤트 적재 시 비동기 반영 지연을 관측
- WebTestClient 기반 통합 테스트로 회귀 방지

## 운영 체크포인트 (성능·안정성)

- 이벤트 루프 블로킹 호출 금지
- 비동기 반영 지연이 허용 가능한 도메인인지 사전 합의
- 코루틴 취소/타임아웃 시 데이터 정합성 검증

## 다음 모듈

- [`../03-routing-datasource/README.md`](../03-routing-datasource/README.md)
