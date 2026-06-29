package exposed.examples.spring.modulith.publications

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringModulithPublicationApplication

fun main(args: Array<String>) {
    runApplication<SpringModulithPublicationApplication>(*args)
}
