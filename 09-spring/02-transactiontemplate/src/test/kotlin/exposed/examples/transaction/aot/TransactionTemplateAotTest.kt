package exposed.examples.transaction.aot

import exposed.examples.transaction.TransactionTemplateApplication
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.transaction.support.TransactionOperations

/**
 * Spring Boot AoT 컴파일 호환성을 [ApplicationContextRunner]로 검증합니다.
 *
 * [TransactionTemplateApplication] 컨텍스트에서 TransactionTemplate 기반 Bean들이
 * 올바르게 구성되는지 경량 컨텍스트로 확인합니다.
 */
class TransactionTemplateAotTest {

    companion object: KLogging()

    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(TransactionTemplateApplication::class.java)
        .withPropertyValues(
            "spring.datasource.url=jdbc:h2:mem:tt-aot-test;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.exposed.generate-ddl=true",
        )

    @Test
    fun `SpringTransactionManager 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<SpringTransactionManager>().shouldNotBeNull()
        }
    }

    @Test
    fun `exposedTransactionTemplate 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<TransactionOperations>("exposedTransactionTemplate").shouldNotBeNull()
        }
    }

    @Test
    fun `withoutTransactionOperations 빈이 생성되어야 한다`() {
        contextRunner.run { context ->
            context.getBean<TransactionOperations>("withoutTransactionOperations").shouldNotBeNull()
        }
    }
}
