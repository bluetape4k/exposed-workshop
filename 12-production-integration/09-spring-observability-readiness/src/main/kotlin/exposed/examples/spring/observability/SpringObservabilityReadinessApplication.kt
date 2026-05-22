package exposed.examples.spring.observability

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot 4 observability and readiness example backed by Exposed JDBC.
 *
 * ## Contract
 * - Actuator readiness includes a database-backed custom health indicator.
 * - Every HTTP response receives a correlation id in `X-Request-ID`.
 * - Structured error responses never expose raw exception details.
 */
@SpringBootApplication
class SpringObservabilityReadinessApplication

fun main(args: Array<String>) {
    runApplication<SpringObservabilityReadinessApplication>(*args)
}
