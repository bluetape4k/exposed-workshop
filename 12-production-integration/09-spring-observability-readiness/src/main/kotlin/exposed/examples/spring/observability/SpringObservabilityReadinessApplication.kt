package exposed.examples.spring.observability

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Exposed JDBC를 사용하는 Spring Boot 4 관측성 및 readiness 예제이다.
 *
 * ## 계약
 * - Actuator readiness는 데이터베이스 기반 custom health indicator를 포함한다.
 * - 모든 HTTP 응답에는 `X-Request-ID` correlation id를 포함한다.
 * - 구조화된 오류 응답은 원본 예외 세부 정보를 노출하지 않는다.
 */
@SpringBootApplication
class SpringObservabilityReadinessApplication

fun main(args: Array<String>) {
    runApplication<SpringObservabilityReadinessApplication>(*args)
}
