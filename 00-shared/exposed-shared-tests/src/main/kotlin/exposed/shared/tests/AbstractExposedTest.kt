package exposed.shared.tests

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.Key
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import java.util.*

abstract class AbstractExposedTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker

//        init {
//            EntityHook.subscribe { change ->
//                log.debug {
//                    "${change.entityClass.table.tableName} id[${change.entityId}] was ${change.changeType}"
//                }
//            }
//        }

        @JvmStatic
        fun enableDialects() = TestDB.enabledDialects()

        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    private object CurrentTestDBInterceptor: StatementInterceptor {
        override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
            return userData.filterValues { it is TestDB }
        }
    }

    fun addIfNotExistsIfSupported() = if (currentDialectTest.supportsIfNotExists) {
        "IF NOT EXISTS "
    } else {
        ""
    }

    protected fun prepareSchemaForTest(schemaName: String): Schema = Schema(
        schemaName,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )

}
