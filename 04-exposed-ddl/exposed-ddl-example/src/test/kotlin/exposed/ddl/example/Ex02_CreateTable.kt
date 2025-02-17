package exposed.ddl.example

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.inProperCase
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFails

class Ex02_CreateTable: AbstractExposedTest() {

    companion object: KLogging()

    object TableWithDuplicatedColumn: Table("myTable") {
        val id1 = integer("id")
        val id2 = integer("id")  // 중복된 컬럼명
    }

    object IDTable: IntIdTable("IntIdTable")

    object TableDuplicatedColumnReferenceToIntIdTable: IntIdTable("myTable") {
        val reference = reference("id", IDTable)
    }

    object TableDuplicatedColumnReferToTable: Table("myTable") {
        val reference = reference("id", TableWithDuplicatedColumn.id1)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `중복된 컬럼이 있는 테이블 생성은 예외를 발생시킵니다`(testDB: TestDB) {
        val errorMessage = "Can't create a table with multiple columns having the same name"

        withDb(testDB) {
            assertFails(errorMessage) {
                SchemaUtils.create(TableWithDuplicatedColumn)
            }
            assertFails(errorMessage) {
                SchemaUtils.create(TableDuplicatedColumnReferenceToIntIdTable)
            }
            assertFails(errorMessage) {
                SchemaUtils.create(TableDuplicatedColumnReferToTable)
            }
        }
    }

    /**
     * 컬럼에 `entityId()`를 사용하여 PRIMARY KEY를 지정할 수 있습니다
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `entityId를 이용하여 특정 컬럼을 PRIMARY KEY로 설정합니다`(testDB: TestDB) {
        val tester = object: IdTable<String>("tester") {
            val column1: Column<String> = varchar("column_1", 30)

            // column1 을 `entityId()`로 id 컬럼으로 지정합니다.
            override val id: Column<EntityID<String>> = column1.entityId()

            // primaryKey 함수를 이용하여 id를 primary key로 지정합니다.
            override val primaryKey = PrimaryKey(id)
        }

        withDb(testDB) {
            val singleColumnDescription = tester.columns.single().descriptionDdl(false)
            log.debug { "single column description: $singleColumnDescription" }
            singleColumnDescription shouldContain "PRIMARY KEY"

            log.debug { "DDL: ${tester.ddl.single()}" }

            // CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
            tester.ddl.single() shouldBeEqualTo
                    "CREATE TABLE ${addIfNotExistsIfSupported()}${tester.tableName.inProperCase()} (${singleColumnDescription})"
        }
    }

    /**
     * 컬럼에 `entityId()`를 사용하여 PRIMARY KEY를 지정할 수 있습니다
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `primaryKey 함수를 이용하여 컬럼을 primary key로 지정합니다`(testDB: TestDB) {
        val tester = object: IdTable<String>("tester") {
            val column1: Column<String> = varchar("column_1", 30)

            // column1 을 `entityId()`로 id 컬럼으로 지정합니다.
            override val id: Column<EntityID<String>> = column1.entityId()

            // primaryKey 함수를 이용하여 column1을 primary key로 지정합니다.
            override val primaryKey = PrimaryKey(column1)
        }

        withDb(testDB) {
            val singleColumnDescription = tester.columns.single().descriptionDdl(false)
            log.debug { "single column description: $singleColumnDescription" }
            singleColumnDescription shouldContain "PRIMARY KEY"

            log.debug { "DDL: ${tester.ddl.single()}" }

            // CREATE TABLE IF NOT EXISTS tester (column_1 VARCHAR(30) PRIMARY KEY)
            tester.ddl.single() shouldBeEqualTo
                    "CREATE TABLE ${addIfNotExistsIfSupported()}${tester.tableName.inProperCase()} (${singleColumnDescription})"
        }
    }

    object BookTable: Table("book") {
        val id = integer("id").autoIncrement()

        override val primaryKey = PrimaryKey(id, name = "PK_Book_ID")
    }

    object PersonTable: Table("person") {
        val id1 = integer("id1")
        val id2 = integer("id2")

        override val primaryKey = PrimaryKey(id1, id2, name = "PK_Person_ID")
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS book (
     *      id SERIAL,
     *      CONSTRAINT PK_Book_ID PRIMARY KEY (id)
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `컬럼이 하나이고, PRIMARY KEY로 지정된 테이블을 생성합니다`(testDB: TestDB) {
        withDb(testDB) {
            val ddl = BookTable.ddl.single()
            log.debug { "DDL: $ddl" }

            SchemaUtils.create(BookTable)
            SchemaUtils.drop(BookTable)
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS person (
     *      id1 INT,
     *      id2 INT,
     *      CONSTRAINT PK_Person_ID PRIMARY KEY (id1, id2)
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `2개의 컬럼이 PRIMARY KEY로 지정된 테이블을 생성합니다`(testDB: TestDB) {
        withDb(testDB) {
            val ddl = PersonTable.ddl.single()
            log.debug { "DDL: $ddl" }

            SchemaUtils.create(PersonTable)
            SchemaUtils.drop(PersonTable)
        }
    }
}
