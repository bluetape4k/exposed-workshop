package exposed.examples.transaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TransactionTemplateApplication


fun main(vararg args: String) {
    runApplication<TransactionTemplateApplication>(*args)
}
