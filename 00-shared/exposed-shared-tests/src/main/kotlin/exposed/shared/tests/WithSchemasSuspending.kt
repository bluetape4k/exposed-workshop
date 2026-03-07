package exposed.shared.tests

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import kotlin.coroutines.CoroutineContext

/**
 * 지정된 스키마를 생성하고 코루틴 기반 서스펜딩 트랜잭션 블록을 실행한 후 스키마를 삭제합니다.
 *
 * 현재 데이터베이스 방언이 스키마 생성을 지원하는 경우에만 실행됩니다.
 * 테스트 완료 후 cascade 옵션으로 스키마와 하위 객체를 모두 삭제하여 테스트 격리를 보장합니다.
 *
 * 실행 흐름:
 * 1. [SchemaUtils.createSchema]로 지정된 스키마를 생성합니다.
 * 2. [statement] 서스펜딩 블록을 실행합니다.
 * 3. 커밋하여 변경사항을 영속화합니다.
 * 4. [SchemaUtils.dropSchema]로 스키마를 삭제합니다.
 *
 * @param dialect 테스트에 사용할 데이터베이스 방언 ([TestDB])
 * @param schemas 생성 및 삭제할 [Schema] 목록
 * @param configure 데이터베이스 설정을 커스터마이즈하는 빌더 람다. 기본값은 빈 설정
 * @param context 트랜잭션을 실행할 코루틴 컨텍스트. 기본값은 [Dispatchers.IO]
 * @param statement 스키마 생성 후 실행할 서스펜딩 트랜잭션 코드 블록
 * @see withDbSuspending
 * @see SchemaUtils
 */
suspend fun withSchemasSuspending(
    dialect: TestDB,
    vararg schemas: Schema,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    context: CoroutineContext? = Dispatchers.IO,
    statement: suspend JdbcTransaction.() -> Unit,
) {
    withDbSuspending(dialect, configure = configure, context = context) {
        if (currentDialectTest.supportsCreateSchema) {
            SchemaUtils.createSchema(*schemas)
            try {
                statement()
                commit()     // Need commit to persist data before drop schemas
            } finally {
                SchemaUtils.dropSchema(*schemas, cascade = true)
                commit()
            }
        }
    }
}
