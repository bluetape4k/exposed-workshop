package exposed.examples.ddl

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.assertFailAndRollback
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class Ex03_CreateMissingTableAndColumns: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * 현재 DB에서 누락된 테이블과 컬럼을 생성 - 01
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      id INT PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      "time" BIGINT NOT NULL
     * );
     *
     * ALTER TABLE tester ADD CONSTRAINT tester_time_unique UNIQUE ("time");
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `누락된 테이블과 컬럼을 생성 - 01`(testDB: TestDB) {
        val testerV1 = object: Table("tester") {
            val id = integer("id")
            val name = varchar("name", 50)
            val time = long("time")

            override val primaryKey = PrimaryKey(id)
        }
        val testerV2 = object: Table("tester") {
            val id = integer("id")
            val name = varchar("name", 50)
            val time = long("time").uniqueIndex()

            override val primaryKey = PrimaryKey(id)
        }

        withDb(testDB) {
            // V1 테이블 생성
            SchemaUtils.create(testerV1)

            // V2 테이블의 uniqueIndex 를 추가합니다.
            SchemaUtils.createMissingTablesAndColumns(testerV2)

            testerV2.exists().shouldBeTrue()
            SchemaUtils.drop(testerV2)
        }
    }

    /**
     * 누락된 테이블과 컬럼을 생성 - 02
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS users2 (
     *      id VARCHAR(22) PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      email VARCHAR(255) NOT NULL,
     *      "camelCased" VARCHAR(255) NOT NULL
     * );
     *
     * ALTER TABLE users2 ADD CONSTRAINT users2_email_unique UNIQUE (email);
     *
     * CREATE INDEX users2_camelcased ON users2 ("camelCased");
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `누락된 테이블과 컬럼을 생성 - 02`(testDB: TestDB) {
        val tester = object: IdTable<String>("Users2") {
            override val id: Column<EntityID<String>> = varchar("id", 22)
                .clientDefault { UUID.randomUUID().toString() }
                .entityId()

            val name = varchar("name", 255)
            val email = varchar("email", 255).uniqueIndex()
            val camelCased = varchar("camelCased", 255).index()

            override val primaryKey = PrimaryKey(id)
        }

        withDb(testDB) {
            tester.exists().shouldBeFalse()

            SchemaUtils.createMissingTablesAndColumns(tester)
            tester.exists().shouldBeTrue()

            try {
                // 아무런 작업도 하지 않습니다.
                SchemaUtils.createMissingTablesAndColumns(tester)
            } finally {
                SchemaUtils.drop(tester)
            }
        }
    }

    /**
     * 같은 테이블 (`tester`) 을 바라보는 두 개의 Exposed Table 객체에 대해 스키마를 변경하면, 적용됩니다.
     *
     * t1 으로 테이블 생성
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      idcol SERIAL PRIMARY KEY,
     *      "text" VARCHAR(50) NOT NULL
     * )
     * ```
     *
     * t2 로 테이블 수정
     * ```sql
     * -- Postgres
     * ALTER TABLE tester
     *      ALTER COLUMN idcol TYPE INT,
     *      ALTER COLUMN idcol DROP DEFAULT;
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `같은 테이블에 대한 매핑 중 autoIncrement를 없앨 수 있다`(testDB: TestDB) {
        val t1 = object: Table("tester") {
            val id = integer("idcol").autoIncrement()
            val text = varchar("text", 50)

            override val primaryKey = PrimaryKey(id)
        }

        val t2 = object: Table("tester") {
            val id = integer("idcol")     // t1 과 같은 테이블, 같은 컬럼을 가르킨다. 단 autoIncrement 가 없다.
            val text = varchar("text", 50)

            override val primaryKey = PrimaryKey(id)
        }

        withDb(testDB) {
            try {
                t1.exists().shouldBeFalse()

                SchemaUtils.createMissingTablesAndColumns(t1)
                t1.exists().shouldBeTrue()
                t1.insert { it[text] = "ABC" }

                // t2 로 테이블 (`tester`) 를 변경하면, `id` 컬럼이 autoIncrement 가 아니게 됩니다.
                SchemaUtils.createMissingTablesAndColumns(t2)

                assertFailAndRollback("Can't insert without primaryKey value") {
                    t2.insert { it[text] = "ABC" }
                }

                t2.insert {
                    it[id] = 3
                    it[text] = "ABC"
                }
            } finally {
                // t1, t2 모두 동일한 `tester` 테이블을 가르킨다.
                SchemaUtils.drop(t1)
            }
        }
    }
}
