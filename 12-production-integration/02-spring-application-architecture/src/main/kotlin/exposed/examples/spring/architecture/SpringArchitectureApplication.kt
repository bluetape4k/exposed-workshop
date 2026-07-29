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
 * Exposed JDBC를 사용하는 Spring Boot 4 애플리케이션 아키텍처 예제이다.
 *
 * ## 계약
 * - 컨트롤러는 얇게 유지하고 서비스 유스케이스에 처리를 위임한다.
 * - 저장소는 Exposed 트랜잭션과 스키마 부트스트랩을 책임진다.
 * - 오류 응답은 controller advice에서 정제한다.
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

