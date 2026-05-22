package exposed.examples.spring.auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot 4 authentication and session metadata example backed by Exposed JDBC.
 *
 * ## Contract
 * - Spring Security authenticates users loaded from the database.
 * - Authorization checks distinguish user and admin roles.
 * - Session/token metadata is persisted separately from the HTTP Basic credential.
 */
@SpringBootApplication
class SpringAuthSessionApplication

fun main(args: Array<String>) {
    runApplication<SpringAuthSessionApplication>(*args)
}
