package exposed.examples.ddl

import exposed.examples.ddl.Ex07_CustomEnumeration.Status.ACTIVE
import exposed.examples.ddl.Ex07_CustomEnumeration.Status.INACTIVE
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.vendors.H2Dialect
import org.jetbrains.exposed.sql.vendors.MysqlDialect
import org.jetbrains.exposed.sql.vendors.PostgreSQLDialect
import org.jetbrains.exposed.sql.vendors.currentDialect
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.util.PGobject

/**
 * 사용자 정의 Enum 수형을 사용하는 방법인데,
 * JPA의  `@Enumerated(EnumType.STRING)` 과 같은 방식으로 사용하시던 분들은
 * Exposed의 column transformation 기능을 사용하는 것을 추천합니다.
 */
class Ex07_CustomEnumeration: AbstractExposedTest() {

    private val supportsCustomEnumerationDB =
        TestDB.ALL_POSTGRES + TestDB.ALL_H2 + TestDB.ALL_MYSQL

    internal enum class Status {
        ACTIVE,
        INACTIVE;

        override fun toString(): String {
            return "Status Enum ToString: $name"
        }
    }

    internal class PGEnum<T: Enum<T>>(enumTypeName: String, enumValue: T?): PGobject() {
        init {
            value = enumValue?.name
            type = enumTypeName
        }
    }

    /**
     * 컬럼 수형으로 Custom Enum 수형을 사용하는 경우
     *
     * ```sql
     * -- PostgreSQL
     * DROP TYPE IF EXISTS StatusEnum;
     * CREATE TYPE StatusEnum AS ENUM ('ACTIVE', 'INACTIVE');
     *
     * CREATE TABLE IF NOT EXISTS enum_table (
     *      id SERIAL PRIMARY KEY,
     *      status StatusEnum NOT NULL
     * );
     * ```
     */
    internal object EnumTable: IntIdTable("enum_table") {
        internal var status: Column<Status> = enumeration<Status>("status").default(ACTIVE)

        internal fun initEnumColumn(sql: String) {
            (columns as MutableList<Column<*>>).remove(status)
            status = customEnumeration(
                name = "status",
                sql = sql,
                fromDb = { value -> Status.valueOf(value as String) },
                toDb = { value ->
                    when (currentDialect) {
                        is PostgreSQLDialect -> PGEnum(sql, value)
                        else -> value.name
                    }
                }
            )

        }
    }

    internal class EnumEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<EnumEntity>(EnumTable)

        var status: Status by EnumTable.status
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `사용자 정의 Enum 수형의 컬럼 정의 - 01`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportsCustomEnumerationDB }

        withDb(testDB) {
            val sqlType = when (currentDialect) {
                is PostgreSQLDialect -> "StatusEnum"
                is H2Dialect, is MysqlDialect -> "ENUM('ACTIVE', 'INACTIVE')"
                else -> error("Unsupported dialect: $currentDialect")
            }
            try {
                if (currentDialect is PostgreSQLDialect) {
                    exec("DROP TYPE IF EXISTS StatusEnum")
                    exec("CREATE TYPE StatusEnum AS ENUM ('ACTIVE', 'INACTIVE')")
                }
                EnumTable.initEnumColumn(sqlType)
                SchemaUtils.create(EnumTable)

                // enumColumn = ACTIVE 로 설정
                // INSERT INTO enum_table (status) VALUES ('ACTIVE')
                EnumTable.insert {
                    it[status] = ACTIVE
                }
                EnumTable.selectAll().single()[EnumTable.status] shouldBeEqualTo ACTIVE

                // enumColumn = INACTIVE 로 설정
                // UPDATE enum_table SET status='INACTIVE'
                EnumTable.update {
                    it[status] = INACTIVE
                }
                EnumTable.selectAll().single()[EnumTable.status] shouldBeEqualTo INACTIVE

                EnumTable.deleteAll()

                // Entity 를 통한 Enum 사용

                // INSERT INTO enum_table (status) VALUES ('ACTIVE')
                val entity = EnumEntity.new {
                    status = ACTIVE
                }
                flushCache()
                entity.status shouldBeEqualTo ACTIVE

                // UPDATE enum_table SET status='INACTIVE' WHERE id = 2
                entity.status = INACTIVE
                EnumEntity.reload(entity)!!.status shouldBeEqualTo INACTIVE

                // UPDATE enum_table SET status='ACTIVE' WHERE id = 2
                entity.status = ACTIVE
                EnumEntity.reload(entity)!!.status shouldBeEqualTo ACTIVE
            } finally {
                SchemaUtils.drop(EnumTable)
                if (currentDialect is PostgreSQLDialect) {
                    exec("DROP TYPE IF EXISTS StatusEnum")
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `custom enumeration with reference`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in supportsCustomEnumerationDB }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS TESTER (
         *      ENUM_COLUMN INT NOT NULL,
         *      ENUM_NAME_COLUMN VARCHAR(32) NOT NULL
         * )
         * ```
         */
        val referenceTable = object: Table("ref_table") {
            var referenceColumn: Column<Status> = enumeration<Status>("ref_column")

            fun initRefColumn() {
                (columns as MutableList<Column<*>>).remove(referenceColumn)
                referenceColumn = reference("ref_column", EnumTable.status)
            }
        }

        withDb(testDB) {
            val sqlType = when (currentDialect) {
                is PostgreSQLDialect -> "StatusEnum"
                is H2Dialect, is MysqlDialect -> "ENUM('ACTIVE', 'INACTIVE')"
                else -> error("Unsupported case")
            }

            try {
                if (currentDialect is PostgreSQLDialect) {
                    exec("DROP TYPE IF EXISTS StatusEnum;")
                    exec("CREATE TYPE StatusEnum AS ENUM ('ACTIVE', 'INACTIVE');")
                }
                EnumTable.initEnumColumn(sqlType)
                with(EnumTable) {
                    if (indices.isEmpty()) status.uniqueIndex()
                }
                SchemaUtils.create(EnumTable)

                referenceTable.initRefColumn()
                SchemaUtils.create(referenceTable)

                val status = ACTIVE
                val id1 = EnumTable.insert {
                    it[EnumTable.status] = status
                } get EnumTable.status

                referenceTable.insert {
                    it[referenceColumn] = id1
                }

                EnumTable.selectAll().single()[EnumTable.status] shouldBeEqualTo status
                referenceTable.selectAll().single()[referenceTable.referenceColumn] shouldBeEqualTo status
            } finally {
                runCatching {
                    SchemaUtils.drop(referenceTable)
                    exec(EnumTable.indices.first().dropStatement().single())
                    SchemaUtils.drop(EnumTable)

                    if (currentDialect is PostgreSQLDialect) {
                        exec("DROP TYPE IF EXISTS StatusEnum")
                    }
                }
            }
        }
    }
}
