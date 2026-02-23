# 01 Spring Boot Demo

Spring Boot + Exposed로 MVC/Reactive 애플리케이션을 구현하며, 두 가지 웹 모델(동기/비동기)에서 Exposed 트랜잭션을 비교하는 챕터입니다.

## 챕터 목표

- Spring MVC와 WebFlux에서 Exposed 기반 API를 비교하며 일관된 데이터 흐름을 확인한다.
- Virtual Threads와 코루틴 각각의 트랜잭션/커넥션 처리 방식의 차이를 파악한다.
- Swagger/자동화된 테스트를 통해 API 문서화와 검증을 병행한다.

## 선수 지식

- Spring Boot 기본 개념
- `00-shared/exposed-shared-tests` 내용

## 포함 모듈

| 모듈                       | 설명                                         |
|--------------------------|--------------------------------------------|
| `spring-mvc-exposed`     | Tomcat + Virtual Threads 기반 Exposed MVC 예제 |
| `spring-webflux-exposed` | Netty + Kotlin Coroutines 기반 Reactive 예제   |

## 권장 학습 순서

1. `spring-mvc-exposed`
2. `spring-webflux-exposed`

## 실행 방법

```bash
./gradlew :exposed-01-spring-boot-spring-mvc-exposed:bootRun
./gradlew :exposed-01-spring-boot-spring-webflux-exposed:bootRun
```

## 테스트 포인트

- 동기/비동기 API에서 동일한 도메인 결과가 일관된지 검증한다.
- Swagger 화면이 실행 시 자동으로 노출되는지 확인한다.

## 성능·안정성 체크포인트

- Virtual Threads를 늘렸을 때 커넥션 풀/DB 부하가 급등하지 않는지 점검한다.
- Netty/WebFlux 환경에서 비동기 트랜잭션이 Reactor 컨텍스트와 충돌 없이 작동하는지 확인한다.

## 다음 챕터

- [02-alternatives-to-jpa](../02-alternatives-to-jpa/README.md): JPA 대안 스택을 학습하기 위한 챕터로 이어갑니다.
