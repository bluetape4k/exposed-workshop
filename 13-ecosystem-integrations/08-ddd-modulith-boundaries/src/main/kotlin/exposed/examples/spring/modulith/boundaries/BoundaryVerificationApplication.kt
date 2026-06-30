package exposed.examples.spring.modulith.boundaries

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot entrypoint for the DDD and Spring Modulith boundary workshop.
 */
@SpringBootApplication
class BoundaryVerificationApplication

fun main(args: Array<String>) {
    runApplication<BoundaryVerificationApplication>(*args)
}
