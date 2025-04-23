# Kotlin Exposed 를 활용한 예제를 작성한다

- Java 가 아닌 Kotlin 으로 작성한다
- 최대한 Kotlin extensions 을 활용한다.
- exposed 0.61.0 기준으로 작성한다
- Kotlin 2.1.20 기준으로 작성한다
- Java 21 의 Virtual Threads 를 활용한다
- bluetape4k 를 활용한다
- exposed-core, exposed-jdbc, exposed-dao, exposed-java-time 을 활용한다.
- connection pooling 은 HikariCP 를 사용한다
- 트랜잭션 처리와 예외 처리를 포함한다
- coroutines 스코프에서 Exposed DSL을 사용하는 예제를 포함한다
- 성능 최적화를 위한 batchInsert 및 관련 기능을 활용한다
- 복잡한 조인과 쿼리 최적화 기법을 적용한다
- 예제는 간결하지만 실제 프로덕션에 사용 가능한 수준으로 작성한다
- 코드에 Kotlin Doc 형식의 주석을 포함한다
- 커밋 메시지는 한국어로 작성한다
- 커밋 메시지는 머릿말을 둔다 (예: feat, fix, docs, style, refactor, perf, test, chore)
