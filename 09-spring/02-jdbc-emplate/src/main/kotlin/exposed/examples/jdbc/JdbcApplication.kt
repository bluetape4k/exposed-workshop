package exposed.examples.jdbc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class JdbcApplication


fun main(vararg args: String) {
    runApplication<JdbcApplication>(*args)
}
