# Kotlinx Exposed Demo

Kotlinx [Exposed](https://github.com/JetBrains/Exposed)를 이용한 Data Access 예제입니다.

## 모듈 구성

### spring-mvc-exposed

**Spring WebMVC + Virtual Threads + Exposed** 를 이용하여 H2, MySQL 데이터베이스 작업을 수행하는 예제입니다.

`Bluetape4k`의 `virtualFuture`와 Exposed의 `transaction`을 활용하여, Virtual Threads 내에서 트랜잭션을 수행할 수 있습니다.

**주요 특징:**

- 동기식 REST API 구조
- Java 21 Virtual Threads 활용
- 블로킹 I/O 작업의 효율적인 처리
- 높은 동시성 처리

### spring-webflux-exposed

**Spring Webflux + Kotlin Coroutines + Exposed** 를 이용하여 H2, MySQL 데이터베이스 작업을 수행하는 예제입니다.

Exposed의 `newSuspendTransaction`을 활용하여, suspend 함수 내에서 트랜잭션을 수행할 수 있습니다.

**주요 특징:**

- 비동기식 REST API 구조
- Kotlin Coroutines 활용
- Non-blocking I/O
- 반응형 프로그래밍 모델

## 두 방식 비교

| 항목      | spring-mvc-exposed        | spring-webflux-exposed    |
|---------|---------------------------|---------------------------|
| 웹 프레임워크 | Spring MVC (Servlet)      | Spring WebFlux (Reactive) |
| 비동기 모델  | Virtual Threads           | Kotlin Coroutines         |
| I/O 모델  | Blocking (Virtual Thread) | Non-blocking              |
| 서버      | Tomcat                    | Netty                     |
| 적합한 경우  | 기존 MVC 코드, 간단한 마이그레이션     | 고동시성, 마이크로서비스             |

## 시작하기

```bash
# MVC 예제 실행
cd spring-mvc-exposed
./gradlew bootRun

# WebFlux 예제 실행
cd spring-webflux-exposed
./gradlew bootRun
```

## 상세 문서

* [Spring Boot Web with Exposed](https://debop.notion.site/Spring-Boot-Web-with-Exposed-1ad2744526b0807f86a1eaaeb4c6baae)
* [Spring Boot Webflux with Exposed](https://debop.notion.site/Spring-Boot-Webflux-with-Exposed-1ad2744526b080db95adc241f749db58)
