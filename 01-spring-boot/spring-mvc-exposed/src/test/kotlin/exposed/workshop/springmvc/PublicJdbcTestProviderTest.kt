package exposed.workshop.springmvc

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withDb
import io.bluetape4k.exposed.tests.withTables
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test

/**
 * published JDBC test provider가 Spring MVC consumer의 test classpath에서
 * schema lifecycle을 수행하는지 확인한다.
 */
class PublicJdbcTestProviderTest {
    object ProviderTable : Table("public_jdbc_provider_mvc") {
        val value = varchar("value", 32)
    }

    @Test
    fun `published JDBC provider creates and cleans up consumer tables`() {
        withTables(TestDB.H2, ProviderTable) {
            ProviderTable.insert { it[value] = "provider" }

            ProviderTable.selectAll().count() shouldBeEqualTo(1L)
        }

        withDb(TestDB.H2, configure = {}) {
            ProviderTable.exists().shouldBeFalse()
        }
    }
}
