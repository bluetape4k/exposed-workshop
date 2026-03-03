# Exposed + Spring WebFlux + Coroutines + Multi-Tenant (03)

WebFlux + Coroutines 기반의 논블로킹 멀티테넌트 예제입니다. Reactor Context를 사용해 테넌트 정보를 전파하고, 코루틴 트랜잭션과 연계해 스키마를 분리합니다.

## 학습 목표

- Reactor Context와 Coroutines 컨텍스트 브릿지를 이해한다.
- WebFlux 환경에서 안전한 테넌트 전파 패턴을 익힌다.
- 논블로킹 경로에서 격리/성능을 함께 검증한다.

## 선수 지식

- [`../08-coroutines/README.md`](../08-coroutines/README.md)
- [`../01-multitenant-spring-web/README.md`](../01-multitenant-spring-web/README.md)

## 핵심 개념

- `TenantFilter`에서 Reactor Context에 tenant 저장
- 코루틴 트랜잭션에서 tenant를 조회해 스키마 적용
- 이벤트 루프 블로킹 방지

## 주요 구성 요소

| 파일/영역                             | 설명                           |
|-----------------------------------|------------------------------|
| `tenant/TenantFilter.kt`          | Reactor Context 기반 tenant 주입 |
| `tenant/TenantId.kt`              | tenant 식별자 모델                |
| `tenant/TenantAwareDataSource.kt` | tenant 라우팅                   |
| `config/NettyConfig.kt`           | Netty 튜닝                     |
| `controller/ActorController.kt`   | 멀티테넌트 API 예제                 |

## 실행 방법

```bash
./gradlew :03-multitenant-spring-webflux:test
./gradlew :03-multitenant-spring-webflux:bootRun
```

## 실습 체크리스트

- 동일 요청을 tenant별로 반복해 데이터 격리 확인
- 컨텍스트 전파 누락 시 재현 테스트를 만들어 회귀 방지
- Netty/DB 풀 튜닝에 따른 처리량 변화를 측정

## 운영 체크포인트 (성능·안정성)

- 이벤트 루프에서 블로킹 코드 호출 금지
- 컨텍스트 전달 누락 시 교차 오염이 발생하므로 필터/어댑터 테스트 강화
- 운영 관측 지표(tenant별 QPS, 오류율, latency)를 분리 수집

## 다음 챕터

- [`../11-high-performance/README.md`](../11-high-performance/README.md)

## 참고

- [Multi-tenant App with Spring Webflux and Coroutines](https://debop.notion.site/Multi-tenant-App-with-Spring-Webflux-and-Coroutines-1dc2744526b0802e926de76e268bd2a8)
