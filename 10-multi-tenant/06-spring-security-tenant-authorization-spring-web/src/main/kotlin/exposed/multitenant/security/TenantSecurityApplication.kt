package exposed.multitenant.security

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TenantSecurityApplication

fun main(args: Array<String>) {
    runApplication<TenantSecurityApplication>(*args)
}
