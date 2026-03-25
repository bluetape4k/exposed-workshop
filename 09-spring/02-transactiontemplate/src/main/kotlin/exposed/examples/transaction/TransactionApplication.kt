package exposed.examples.transaction

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(proxyBeanMethods = false)
/**
 * Spring `TransactionTemplate` 과 Exposed 트랜잭션 연동 예제를 실행하는 애플리케이션입니다.
 */
class TransactionTemplateApplication

/**
 * 애플리케이션 진입점입니다.
 */
fun main(vararg args: String) {
    runApplication<TransactionTemplateApplication>(*args)
}
