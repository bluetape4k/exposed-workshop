package exposed.examples.spring.realtime

import exposed.examples.spring.realtime.model.HealthResponse
import exposed.examples.spring.realtime.model.IndexResponse
import exposed.examples.spring.realtime.persistence.NotificationPersistence
import exposed.examples.spring.realtime.repository.ExposedNotificationOutboxRepository
import exposed.examples.spring.realtime.repository.NotificationOutboxRepository
import exposed.examples.spring.realtime.service.NotificationService
import exposed.examples.spring.realtime.service.RealtimeDelivery
import exposed.examples.spring.realtime.web.RealtimeHub
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
internal class SpringOutboxRealtimeApplication

fun main(args: Array<String>) {
    runApplication<SpringOutboxRealtimeApplication>(*args)
}

@Configuration(proxyBeanMethods = false)
internal class SpringOutboxRealtimeConfiguration {

    @Bean(destroyMethod = "close")
    fun notificationPersistence(): NotificationPersistence =
        NotificationPersistence.inMemory()

    @Bean
    fun notificationOutboxRepository(persistence: NotificationPersistence): NotificationOutboxRepository =
        ExposedNotificationOutboxRepository(persistence.database)

    @Bean
    fun realtimeHub(): RealtimeHub =
        RealtimeHub()

    @Bean
    fun notificationService(
        repository: NotificationOutboxRepository,
        realtimeDelivery: RealtimeDelivery,
    ): NotificationService =
        NotificationService(repository, realtimeDelivery)
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
