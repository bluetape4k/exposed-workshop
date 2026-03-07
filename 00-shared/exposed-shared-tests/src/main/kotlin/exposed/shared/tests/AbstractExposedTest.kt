package exposed.shared.tests

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import java.util.*

/**
 * 모든 Exposed 테스트의 기반 추상 클래스.
 *
 * 이 클래스는 Exposed 프레임워크를 사용하는 모든 테스트 모듈에서 공통으로 사용되는
 * 기본 설정과 유틸리티 메서드를 제공합니다.
 *
 * - 기본 타임존을 UTC로 설정합니다.
 * - Faker 인스턴스를 제공하여 테스트 데이터 생성을 지원합니다.
 * - 활성화된 데이터베이스 방언(Dialect) 목록을 제공합니다.
 *
 * @see TestDB
 * @see withTables
 */
abstract class AbstractExposedTest {

    companion object: KLogging() {
        /** Faker 라이브러리 인스턴스. 테스트 데이터 생성에 사용됩니다. */
        @JvmStatic
        val faker = Fakers.faker

        /**
         * 현재 활성화된 데이터베이스 방언 목록을 반환합니다.
         *
         * JUnit 5의 `@MethodSource` 어노테이션과 함께 파라미터화 테스트에 사용됩니다.
         *
         * @return 활성화된 [TestDB] 목록
         */
        @JvmStatic
        fun enableDialects() = TestDB.enabledDialects()

        /**
         * `@MethodSource` 어노테이션에 사용할 메서드 이름 상수.
         *
         * 파라미터화 테스트에서 `@MethodSource(ENABLE_DIALECTS_METHOD)`로 참조합니다.
         */
        const val ENABLE_DIALECTS_METHOD = "enableDialects"
    }

    init {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    /**
     * 트랜잭션 커밋 시 [TestDB] 관련 사용자 데이터를 유지하는 인터셉터.
     *
     * 커밋 후에도 현재 테스트 DB 정보가 트랜잭션 스토어에 남아있도록 합니다.
     */
    private object CurrentTestDBInterceptor: StatementInterceptor {
        override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
            return userData.filterValues { it is TestDB }
        }
    }

    /**
     * 현재 데이터베이스 방언이 `IF NOT EXISTS` 구문을 지원하는 경우 해당 문자열을 반환합니다.
     *
     * DDL 구문 생성 시 데이터베이스 호환성을 위해 사용됩니다.
     *
     * @return `IF NOT EXISTS ` 문자열 또는 빈 문자열
     */
    fun addIfNotExistsIfSupported() = if (currentDialectTest.supportsIfNotExists) {
        "IF NOT EXISTS "
    } else {
        ""
    }

    /**
     * 테스트용 스키마 객체를 생성합니다.
     *
     * Oracle 스타일의 테이블스페이스 설정이 포함된 스키마를 생성합니다.
     *
     * @param schemaName 생성할 스키마의 이름
     * @return 설정이 적용된 [Schema] 객체
     */
    protected fun prepareSchemaForTest(schemaName: String): Schema = Schema(
        schemaName,
        defaultTablespace = "USERS",
        temporaryTablespace = "TEMP ",
        quota = "20M",
        on = "USERS"
    )
}
