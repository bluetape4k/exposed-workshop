package exposed.examples.spring.transaction

import org.jetbrains.exposed.v1.spring.boot.autoconfigure.ExposedAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(
    // 테스트를 위해 Exposed 의 SpringTransactionManager 를 사용하지 않도록 설정
    exclude = [ExposedAutoConfiguration::class, DataSourceTransactionManagerAutoConfiguration::class]
)
class SpringTransactionApp

fun main(vararg args: String) {
    runApplication<SpringTransactionApp>(*args)
}
