# Kotlinx Exposed Demo

Kotlinx [Exposed](https://github.com/JetBrains/Exposed) 를 이용한 Data Access 예제입니다.

## sql-webflux-coroutines

Spring Webflux + Kotlin Coroutines + Exposed 를 이용하여 H2, MySQL 데이터베이스 작업을 수행하는 예제입니다.

Exposed 의 `newSuspendTransaction` 을 활용하여, suspend 함수 내에서 트랜잭션을 수행할 수 있습니다.

## sql-webmvc-virtualthread

Spring WebMVC + Virtual Threads + Exposed 를 이용하여 H2, MySQL 데이터베이스 작업을 수행하는 예제입니다.

`Bluetape4k`의 `virtualFuture` 와 Exposed 의 `transaction` 을 활용하여, Virtual Threads 내에서 트랜잭션을 수행할 수 있습니다.
