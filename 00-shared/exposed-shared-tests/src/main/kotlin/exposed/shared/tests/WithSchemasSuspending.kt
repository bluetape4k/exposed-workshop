package exposed.shared.tests

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴 환경에서 지정된 스키마를 생성한 후 서스펜딩 트랜잭션 블록을 실행하고, 완료 후 스키마를 삭제합니다.
 *
 * 현재 방언(dialect)이 스키마 생성을 지원하는 경우에만 실행됩니다.
 *
 * @param dialect 사용할 테스트 데이터베이스 방언
 * @param schemas 생성할 스키마 목록
 * @param configure 데이터베이스 설정 빌더 람다 (선택사항)
 * @param context 트랜잭션에 사용할 코루틴 컨텍스트 (기본값: [Dispatchers.IO])
 * @param statement 스키마가 생성된 상태에서 실행할 서스펜딩 트랜잭션 블록
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
