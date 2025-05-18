package exposed.example.springboot

import exposed.example.springboot.tables.TestTable
import exposed.example.springboot.tables.ignored.IgnoredTable
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.support.uninitialized
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.spring.DatabaseInitializer
import org.jetbrains.exposed.spring.autoconfigure.ExposedAutoConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import kotlin.test.assertFailsWith

/**
 * Exposed의 Spring Boot 용 AutoConfiguration인 [ExposedAutoConfiguration] 을 제외하고, 직접 설정 작업을 수행하는 방법
 */
@SpringBootTest(
    classes = [Application::class],
    // 매뉴얼로 DatabaseInitializer 를 사용하기 위해 ExposedAutoConfiguration 를 제외합니다.
    properties = [
        "spring.autoconfigure.exclude=org.jetbrains.exposed.spring.boot.autoconfigure.ExposedAutoConfiguration"
    ]
)
class DatabaseInitializerTest {

    companion object: KLoggingChannel()

    @Autowired
    private val applicationContext: ApplicationContext = uninitialized()

    @Test
    fun `TestTable에 대해 스키마를 생성하고 IgnoreTable에 대해서는 생성하지 않아야 합니다`() {

        // H2 DB에 연결
        val database = Database.connect("jdbc:h2:mem:test-spring", driver = "org.h2.Driver", user = "sa")

        transaction(database) {
            // `IgnoredTable` 은 `DatabaseInitializer` 에서 제외합니다.
            val excludedPackages: List<String> = listOf(IgnoredTable::class.java.`package`.name)
            log.info { "Excluded packages: $excludedPackages" }

            // `DatabaseInitializer` 를 사용하여 스키마를 생성합니다.
            DatabaseInitializer(applicationContext, excludedPackages).run(null)

            // `TestTable` 은 생성되어야 합니다.
            TestTable.exists().shouldBeTrue()
            TestTable.selectAll().count() shouldBeEqualTo 0L

            // `IgnoredTable` 은 `DatabaseInitializer` 에서 제외되어 Table 생성이 되지 않습니다.
            IgnoredTable.exists().shouldBeFalse()
            assertFailsWith<ExposedSQLException> {
                IgnoredTable.selectAll().count()
            }
        }
    }
}
