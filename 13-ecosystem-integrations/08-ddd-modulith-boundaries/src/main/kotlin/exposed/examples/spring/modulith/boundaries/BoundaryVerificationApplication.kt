package exposed.examples.spring.modulith.boundaries

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * DDD 및 Spring Modulith 경계 워크숍의 Spring Boot 진입점이다.
 */
@SpringBootApplication
class BoundaryVerificationApplication

fun main(args: Array<String>) {
    runApplication<BoundaryVerificationApplication>(*args)
}
