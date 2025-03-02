package exposed.example.springboot

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication(
    // 테스트를 위해 Exposed 의 SpringTransactionManager 를 사용하지 않도록 설정
    exclude = [DataSourceTransactionManagerAutoConfiguration::class]
)
class Application

fun main(vararg args: String) {
    runApplication<Application>(*args)
}
