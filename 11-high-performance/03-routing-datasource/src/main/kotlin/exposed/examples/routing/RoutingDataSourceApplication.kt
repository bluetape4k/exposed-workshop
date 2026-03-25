package exposed.examples.routing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Routing DataSource 예제를 실행하는 Spring Boot 애플리케이션입니다.
 */
@SpringBootApplication(proxyBeanMethods = false)
class RoutingDataSourceApplication

fun main(args: Array<String>) {
    runApplication<RoutingDataSourceApplication>(*args)
}
