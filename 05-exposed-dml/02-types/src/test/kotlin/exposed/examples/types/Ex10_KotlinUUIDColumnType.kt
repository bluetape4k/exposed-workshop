// Kotlin 2.3 부터 사용 가능

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Exposed DSL에서 Kotlin UUID 컬럼 타입 사용 예제.
 *
 * Kotlin 표준 라이브러리의 [kotlin.uuid.Uuid]를 Exposed 컬럼에 매핑하는 방법과
 * DB별 UUID 저장/조회 동작을 학습합니다.
 */
@OptIn(ExperimentalUuidApi::class)
class Ex10_KotlinUUIDColumnType: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert read kotlin UUID`(testDB: TestDB) {
        val tester = object: Table("test_java_uuid") {
            val id = uuid("id")
        }
        withTables(testDB, tester) {
            val uuid = Uuid.generateV7()
            tester.insert {
                it[id] = uuid
            }
            val dbUuid = tester.selectAll().first()[tester.id]
            dbUuid shouldBeEqualTo uuid
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `test kotlin UUIDColumnType`(testDB: TestDB) {
        val tester = object: IntIdTable("test_java_uuid_column_type") {
            val uuid = uuid("java_uuid")
        }

        withTables(testDB, tester) {
            val uuid = Uuid.generateV7()
            val id = tester.insertAndGetId { it[this.uuid] = uuid }
            id.shouldNotBeNull()

            val uidById = tester.selectAll()
                .where { tester.id eq id }
                .singleOrNull()
                ?.get(tester.uuid)
            uidById shouldBeEqualTo uuid

            val uidByKey = tester.selectAll()
                .where { tester.uuid eq uuid }
                .singleOrNull()
                ?.get(tester.uuid)
            uidByKey shouldBeEqualTo uuid
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `mariadb 전용 uuid 수형`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB == TestDB.MARIADB }

        val tester = object: Table("test_java_uuid_mariadb") {
            val id = uuid("id")
        }

        withDb(testDB) {
            try {
                // From the version 10.7 MariaDB has own 'UUID' column type, that does not work with the current column type.
                // Even if we generate on DDL type 'BINARY(16)' we could support native UUID for IO operations.
                exec("CREATE TABLE test_java_uuid_mariadb (id UUID NOT NULL)")

                val uuid = Uuid.generateV7()
                tester.insert {
                    it[id] = uuid
                }

                val dbUuid = tester.selectAll().first()[tester.id]
                dbUuid shouldBeEqualTo uuid
            } finally {
                exec("DROP TABLE IF EXISTS test_java_uuid_mariadb")
            }
        }
    }
}
