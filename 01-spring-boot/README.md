# Kotlinx Exposed Demo

Kotlinx [Exposed](https://github.com/JetBrains/Exposed) 를 이용한 Data Access 예제입니다.

## spring-mvc-exposed

Spring WebMVC + Virtual Threads + Exposed 를 이용하여 H2, MySQL 데이터베이스 작업을 수행하는 예제입니다.

`Bluetape4k`의 `virtualFuture` 와 Exposed 의 `transaction` 을 활용하여, Virtual Threads 내에서 트랜잭션을 수행할 수 있습니다.

## spring-webflux-exposed

Spring Webflux + Kotlin Coroutines + Exposed 를 이용하여 H2, MySQL 데이터베이스 작업을 수행하는 예제입니다.

Exposed 의 `newSuspendTransaction` 을 활용하여, suspend 함수 내에서 트랜잭션을 수행할 수 있습니다.

## 문서

* [Spring Boot Web with Exposed](https://debop.notion.site/Spring-Boot-Web-with-Exposed-1ad2744526b0807f86a1eaaeb4c6baae)
* [Spring Boot Webflux with Exposed](https://debop.notion.site/Spring-Boot-Webflux-with-Exposed-1ad2744526b080db95adc241f749db58)
