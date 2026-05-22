package exposed.multitenant.schema

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SchemaPerTenantApplication

fun main(args: Array<String>) {
    runApplication<SchemaPerTenantApplication>(*args)
}
