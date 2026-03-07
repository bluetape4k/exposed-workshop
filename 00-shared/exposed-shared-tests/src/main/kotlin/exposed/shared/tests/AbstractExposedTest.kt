package exposed.shared.tests

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import java.util.*

/**
 * 모든 Exposed 테스트의 기반 클래스.
 *
 * 테스트 DB 방언(dialect) 활성화, 타임존 설정, 테스트 데이터 생성을 위한 Faker 인스턴스를 제공합니다.
 */
abstract class AbstractExposedTest {

    companion object: KLogging() {
        /** 테스트 데이터 생성을 위한 Faker 인스턴스. */
        @JvmStatic
        val faker = Fakers.faker

        /**
         * 현재 환경에서 활성화된 테스트 DB 방언 목록을 반환합니다.
         *
         * @return 활성화된 [TestDB] 목록
         */
        @JvmStatic
        fun enableDialects() = TestDB.enabledDialects()

        /** JUnit 5 `@MethodSource`에서 사용하는 `enableDialects` 메서드명 상수. */
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

    /**
     * 현재 방언이 `IF NOT EXISTS` 구문을 지원하면 해당 문자열을, 그렇지 않으면 빈 문자열을 반환합니다.
     *
     * @return `"IF NOT EXISTS "` 또는 빈 문자열
     */
    fun addIfNotExistsIfSupported() = if (currentDialectTest.supportsIfNotExists) {
        "IF NOT EXISTS "
    } else {
        ""
    }

    /**
     * 테스트용 스키마 객체를 생성합니다.
     *
     * @param schemaName 생성할 스키마 이름
     * @return 테스트 설정이 적용된 [Schema] 객체
     */
    protected fun prepareSchemaForTest(schemaName: String): Schema = Schema(
        schemaName,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )
}
