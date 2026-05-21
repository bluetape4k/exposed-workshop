package exposed.examples.spring.architecture

import exposed.examples.spring.architecture.model.HealthResponse
import exposed.examples.spring.architecture.model.IndexResponse
import exposed.examples.spring.architecture.persistence.CustomerPersistence
import exposed.examples.spring.architecture.repository.CustomerRepository
import exposed.examples.spring.architecture.repository.ExposedCustomerRepository
import exposed.examples.spring.architecture.service.CustomerService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Spring Boot 4 application architecture example backed by Exposed JDBC.
 *
 * ## Contract
 * - Controllers remain thin and delegate to service use cases.
 * - Repositories own Exposed transactions and schema bootstrap.
 * - Error responses are sanitized by controller advice.
 */
@SpringBootApplication
internal class SpringArchitectureApplication

fun main(args: Array<String>) {
    runApplication<SpringArchitectureApplication>(*args)
}

@Configuration(proxyBeanMethods = false)
internal class SpringArchitectureConfiguration {

    @Bean(destroyMethod = "close")
    fun customerPersistence(): CustomerPersistence =
        CustomerPersistence.inMemory()

    @Bean
    fun customerRepository(persistence: CustomerPersistence): CustomerRepository =
        ExposedCustomerRepository(persistence.database)

    @Bean
    fun customerService(repository: CustomerRepository): CustomerService =
        CustomerService(repository)
}

@RestController
internal class IndexController {

    @GetMapping("/")
    fun index(): IndexResponse =
        IndexResponse()

    @GetMapping("/health")
    fun health(): HealthResponse =
        HealthResponse(status = "UP")
}

