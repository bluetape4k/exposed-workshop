package exposed.multitenant.database

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DatabasePerTenantApplication

fun main(args: Array<String>) {
    runApplication<DatabasePerTenantApplication>(*args)
}
