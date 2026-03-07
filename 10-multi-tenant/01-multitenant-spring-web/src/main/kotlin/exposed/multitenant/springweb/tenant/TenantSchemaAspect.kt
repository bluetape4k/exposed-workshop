package exposed.multitenant.springweb.tenant

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 멀티테넌시 지원을 위한 Spring AOP 애스펙트.
 *
 * `@Transactional`이 적용된 클래스나 메서드가 실행되기 전에 현재 테넌트에 해당하는
 * 데이터베이스 스키마를 설정합니다. [TenantContext]에서 현재 테넌트 정보를 읽어
 * Exposed의 [SchemaUtils.setSchema]를 통해 스키마를 전환합니다.
 *
 * @see TenantContext
 * @see SchemaUtils
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
class TenantSchemaAspect {

    companion object: KLogging()

    /**
     * `@Transactional` 이 적용된 클래스나 메소드에 대해 Multitenncy 를 지원하기 위한 Schema 설정을 수행합니다.
     */
    @Before(
        "@within(org.springframework.transaction.annotation.Transactional) || " +
                "@annotation(org.springframework.transaction.annotation.Transactional)"
    )
    fun setSchemaForTransaction() {
        transaction {
            val schema = TenantContext.getCurrentTenantSchema()
            log.info { "Use schema=$schema" }
            SchemaUtils.setSchema(schema)
            commit()
        }
    }
}
