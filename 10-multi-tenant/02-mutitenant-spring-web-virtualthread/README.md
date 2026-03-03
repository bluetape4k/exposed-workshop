# Exposed + Spring Web + Virtual Threads + Multi-Tenant (02)

`01` 모듈의 멀티테넌시 구조를 Java 21 Virtual Threads 환경으로 확장한 예제입니다. 블로킹 I/O 스타일을 유지하면서 동시 처리량을 높이는 구성에 초점을 맞춥니다.

## 학습 목표

- Virtual Thread 기반 요청 처리 모델을 이해한다.
- 멀티테넌트 컨텍스트와 Virtual Thread 조합 시 주의점을 학습한다.
- 동시성 증가 상황에서 격리/안정성을 검증한다.

## 선수 지식

- [`../01-multitenant-spring-web/README.md`](../01-multitenant-spring-web/README.md)
- Java 21+

## 핵심 개념

- Tomcat 요청 처리기를 Virtual Thread 실행기로 대체
- Thread-per-request 모델 유지 + 높은 동시성 확보
- 테넌트 컨텍스트는 요청 단위로 독립 관리

## 주요 구성 요소

| 파일/영역                                 | 설명                    |
|---------------------------------------|-----------------------|
| `config/TomcatVirtualThreadConfig.kt` | Virtual Thread 실행기 설정 |
| `tenant/TenantFilter.kt`              | 요청 테넌트 식별             |
| `tenant/TransactionSchemaAspect.kt`   | 트랜잭션 경계 스키마 적용        |
| `tenant/TenantAwareDataSource.kt`     | 테넌트 라우팅               |

## 실행 방법

```bash
./gradlew :02-mutitenant-spring-web-virtualthread:test
./gradlew :02-mutitenant-spring-web-virtualthread:bootRun
```

## 실습 체크리스트

- 동시 요청 수를 늘려도 테넌트 데이터가 교차되지 않는지 검증
- 스레드 풀/커넥션 풀 설정값 변경 시 지연시간 변화를 측정
- 오류/예외 경로에서도 컨텍스트 정리가 되는지 확인

## 운영 체크포인트 (성능·안정성)

- Virtual Thread 증가만으로 DB 병목이 해결되지 않으므로 커넥션 풀을 함께 튜닝
- 장시간 블로킹 작업을 요청 경로에 두지 않도록 점검
- tenant 누수 탐지를 위한 통합 테스트를 CI에 고정

## 다음 모듈

- [`../03-multitenant-spring-webflux/README.md`](../03-multitenant-spring-webflux/README.md)
