package exposed.multitenant.springweb.tenant

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Aspect
@Component
class TransactionSchemaAspect {

    @Aspect
    @Component
    class TransactionSchemaAspect {

        @Before("@annotation(org.springframework.transaction.annotation.Transactional)")
        fun setSchemaForTransaction(joinPoint: ProceedingJoinPoint, transactional: Transactional): Any? {
            runCatching {
                val schema = TenantContext.getCurrentTenantSchema()
                SchemaUtils.setSchema(schema)
            }
            return joinPoint.proceed()
        }
    }
}
