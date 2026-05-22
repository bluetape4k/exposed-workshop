package exposed.examples.spring.httpoutbox

import exposed.examples.spring.httpoutbox.client.PaymentGateway
import exposed.examples.spring.httpoutbox.client.RestClientPaymentGateway
import exposed.examples.spring.httpoutbox.model.HealthResponse
import exposed.examples.spring.httpoutbox.model.IndexResponse
import exposed.examples.spring.httpoutbox.persistence.PaymentPersistence
import exposed.examples.spring.httpoutbox.repository.ExposedPaymentOutboxRepository
import exposed.examples.spring.httpoutbox.repository.PaymentOutboxRepository
import exposed.examples.spring.httpoutbox.service.PaymentService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient

@SpringBootApplication
internal class SpringHttpOutboxApplication

fun main(args: Array<String>) {
    runApplication<SpringHttpOutboxApplication>(*args)
}

@Configuration(proxyBeanMethods = false)
internal class SpringHttpOutboxConfiguration {

    @Bean(destroyMethod = "close")
    fun paymentPersistence(): PaymentPersistence =
        PaymentPersistence.inMemory()

    @Bean
    fun paymentOutboxRepository(persistence: PaymentPersistence): PaymentOutboxRepository =
        ExposedPaymentOutboxRepository(persistence.database)

    @Bean
    fun restClientBuilder(): RestClient.Builder =
        RestClient.builder()

    @Bean
    fun paymentGateway(
        restClientBuilder: RestClient.Builder,
        @Value("\${example.external-payments.base-url:https://payments.example.invalid}") baseUrl: String,
    ): PaymentGateway =
        RestClientPaymentGateway(restClientBuilder.baseUrl(baseUrl).build())

    @Bean
    fun paymentService(
        repository: PaymentOutboxRepository,
        paymentGateway: PaymentGateway,
    ): PaymentService =
        PaymentService(repository, paymentGateway)
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
