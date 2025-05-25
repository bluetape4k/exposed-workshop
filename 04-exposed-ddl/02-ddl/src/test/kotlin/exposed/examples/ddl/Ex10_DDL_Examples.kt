package exposed.examples.ddl

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.assertFailAndRollback
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.inProperCase
import exposed.shared.tests.withDb
import exposed.shared.tests.withSchemas
import exposed.shared.tests.withTables
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainIgnoringCase
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.CheckConstraint
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.ExperimentalKeywordApi
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.UpperCase
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.exists
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.migration.MigrationUtils
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.test.assertTrue

/**
 * 다양한 DDL 예제
 */
class Ex10_DDL_Examples: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `table exists`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS "testTable" (
         *      id INT PRIMARY KEY,
         *      "name" VARCHAR(42) NOT NULL
         * )
         * ```
         */
        val testTable = object: Table("testTable") {
            val id = integer("id")
            val name = varchar("name", length = 42)

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB) {
            testTable.exists().shouldBeFalse()
        }

        withTables(testDB, testTable) {
            testTable.exists().shouldBeTrue()
        }
    }

    @Test
    fun `keyword identifiers with opt out`() {
        val keywords = listOf("Integer", "name")
        val tester = object: Table(keywords[0]) {
            val name = varchar(keywords[1], length = 32)
        }

        transaction(keywordFlagDB) {
            log.debug { "DB Config preserveKeywordCasing=false" }
            db.config.preserveKeywordCasing.shouldBeFalse()

            tester.exists().shouldBeFalse()

            SchemaUtils.create(tester)
            tester.exists().shouldBeTrue()

            val (tableName, columnName) = keywords.map { "\"${it.uppercase()}\"" }

            val expectedCreate =
                "CREATE TABLE ${addIfNotExistsIfSupported()}$tableName (" + "$columnName ${tester.name.columnType.sqlType()} NOT NULL)"
            tester.ddl.single() shouldBeEqualTo expectedCreate

            // check that insert and select statement identifiers also match in DB without throwing SQLException
            tester.insert { it[name] = "A" }

            val expectedSelect = "SELECT $tableName.$columnName FROM $tableName"
            tester.selectAll().also {
                it.prepareSQL(this, prepared = false) shouldBeEqualTo expectedSelect
            }

            // check that identifiers match with returned jdbc metadata
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(tester, withLogs = false)
            statements.isEmpty().shouldBeTrue()

            SchemaUtils.drop(tester)
        }

        TransactionManager.closeAndUnregister(keywordFlagDB)
    }

    private val keywordFlagDB by lazy {
        Database.connect(
            url = "jdbc:h2:mem:keywordFlagDB;DB_CLOSE_DELAY=-1;",
            driver = "org.h2.Driver",
            user = "root",
            password = "",
            databaseConfig = DatabaseConfig {
                @OptIn(ExperimentalKeywordApi::class)
                preserveKeywordCasing = false
            })
    }

    /**
     * 키워드를 테이블이나 컬럼 이름으로 사용할 수 있도록 설정 ( db.config.preserveKeywordCasing = true )
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `keyword identifiers without opt out`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_H2 }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS "data" (
         *      "public" BOOLEAN NOT NULL,
         *      "key" INT NOT NULL,
         *      "constraint" VARCHAR(32) NOT NULL
         * )
         * ```
         */
        val keywords = listOf("data", "public", "key", "constraint")
        val keywordTable = object: Table(keywords[0]) {
            val public = bool(keywords[1])
            val data = integer(keywords[2])
            val constraint = varchar(keywords[3], length = 32)
        }

        withDb(testDB) {
            db.config.preserveKeywordCasing.shouldBeTrue()

            SchemaUtils.create(keywordTable)
            keywordTable.exists().shouldBeTrue()

            val (tableName, publicName, dataName, constraintName) = keywords.map {
                when (currentDialectTest) {
                    is MysqlDialect -> "`$it`"
                    else -> "\"$it\""
                }
            }

            val expectedCreate =
                "CREATE TABLE ${addIfNotExistsIfSupported()}$tableName (" + "$publicName ${keywordTable.public.columnType.sqlType()} NOT NULL, " + "$dataName ${keywordTable.data.columnType.sqlType()} NOT NULL, " + "$constraintName ${keywordTable.constraint.columnType.sqlType()} NOT NULL)"

            keywordTable.ddl.single() shouldBeEqualTo expectedCreate

            // check that insert and select statement identifiers also match in DB without throwing SQLException
            keywordTable.insert {
                it[public] = true
                it[data] = 999
                it[constraint] = "unique"
            }

            val expectedSelect =
                "SELECT $tableName.$publicName, $tableName.$dataName, $tableName.$constraintName FROM $tableName"
            keywordTable.selectAll().also {
                it.prepareSQL(this, prepared = false) shouldBeEqualTo expectedSelect
            }

            // check that identifiers match with returned jdbc metadata
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(keywordTable, withLogs = false)
            statements.shouldBeEmpty()

            SchemaUtils.drop(keywordTable)
        }
    }

    /**
     * 테스트 함수 밖으로 이동하여, 생성된 테이블 명이 짧아지도록 함
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS "unnamedtable$1" (
     *      id INT PRIMARY KEY,
     *      "name" VARCHAR(42) NOT NULL
     * );
     * ```
     */
    val unnamedTable = object: Table() {
        val id = integer("id")
        val name = varchar("name", 42)

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * 이름을 지정하지 않은 테이블에 대해 테이블 명을 정의하는 방법
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `unnamed table with quoted SQL`(testDB: TestDB) {
        withTables(testDB, unnamedTable) {
            val q = db.identifierManager.quoteString

            // MySQL 테이블 명에는 back-quote(`) 를 사용하지 않네요.
            val tableName = if (currentDialectTest.needsQuotesWhenSymbolsInNames && testDB !in TestDB.ALL_MYSQL) {
                "$q${"unnamedTable$1".inProperCase()}$q"
            } else {
                "unnamedTable$1".inProperCase()
            }
            log.debug { "Table Name: $tableName" }

            val integerType = currentDialectTest.dataTypeProvider.integerType()
            val varCharType = currentDialectTest.dataTypeProvider.varcharType(42)

            val expectedDDL =
                "CREATE TABLE " + addIfNotExistsIfSupported() + "$tableName " +
                        "(${"id".inProperCase()} $integerType PRIMARY KEY," + " $q${"name".inProperCase()}$q $varCharType NOT NULL)"

            val unnamedTableDDL = unnamedTable.ddl.single()
            log.debug { "Expected DDL: $expectedDDL" }
            log.debug { "Actual DDL:   $unnamedTableDDL" }

            unnamedTableDDL shouldBeEqualTo expectedDDL
        }
    }


    @Test
    fun `namedEmptyTable without quotes SQL`() {
        val testTable = object: Table("test_named_table") {}
        withDb(TestDB.H2) {
            testTable.ddl.single() shouldBeEqualTo "CREATE TABLE IF NOT EXISTS ${"test_named_table".inProperCase()}"
        }
    }

    fun `table with different column types SQL 01`() {
        /**
         * ```sql
         * -- H2
         * CREATE TABLE IF NOT EXISTS DIFFERENT_COLUMN_TYPES (
         *      ID INT AUTO_INCREMENT NOT NULL,
         *      "name" VARCHAR(42) PRIMARY KEY,
         *      AGE INT NULL
         * )
         * ```
         */
        val testTable = object: Table("different_column_types") {
            val id = integer("id").autoIncrement()
            val name = varchar("name", 42)
            val age = integer("age").nullable()

            override val primaryKey = PrimaryKey(name)
        }

        withTables(TestDB.H2, testTable) {
            val varCharType = currentDialectTest.dataTypeProvider.varcharType(42)

            log.debug { "DDL: ${testTable.ddl.single()}" }

            testTable.ddl.single() shouldBeEqualTo
                    "CREATE TABLE " + addIfNotExistsIfSupported() +
                    "${"different_column_types".inProperCase()} " +
                    "(${"id".inProperCase()} ${currentDialectTest.dataTypeProvider.integerAutoincType()} NOT NULL, " +
                    "" + "\"${"name".inProperCase()}\" $varCharType PRIMARY KEY, " +
                    "${"age".inProperCase()} ${currentDialectTest.dataTypeProvider.integerType()} NULL)"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `table with different column types SQL 02`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS with_different_column_types (
         *      id INT,
         *      "name" VARCHAR(42),
         *      age INT NULL,
         *
         *      CONSTRAINT pk_with_different_column_types PRIMARY KEY (id, "name")
         * )
         * ```
         */
        val testTable = object: Table("with_different_column_types") {
            val id = integer("id")
            val name = varchar("name", 42)
            val age = integer("age").nullable()

            override val primaryKey = PrimaryKey(id, name)
        }

        withTables(testDB, testTable) {
            val q = db.identifierManager.quoteString
            val varCharType = currentDialectTest.dataTypeProvider.varcharType(42)
            val tableDescription =
                "CREATE TABLE " + addIfNotExistsIfSupported() + "with_different_column_types".inProperCase()
            val idDescription = "${"id".inProperCase()} ${currentDialectTest.dataTypeProvider.integerType()}"
            val nameDescription = "$q${"name".inProperCase()}$q $varCharType"
            val ageDescription = "${"age".inProperCase()} ${db.dialect.dataTypeProvider.integerType()} NULL"
            val primaryKeyConstraint =
                "CONSTRAINT pk_with_different_column_types PRIMARY KEY (${"id".inProperCase()}, $q${"name".inProperCase()}$q)"

            log.debug { "DDL: ${testTable.ddl.single()}" }

            testTable.ddl.single() shouldBeEqualTo
                    "$tableDescription " +
                    "($idDescription, $nameDescription, $ageDescription, $primaryKeyConstraint)"
        }
    }

    /**
     * unsigned 수형에 autoIncrement 인 Primary Key 인 컬럼에 대한 DDL 생성
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `auto increment on unsigned columns`(testDB: TestDB) {
        /**
         * separate tables are necessary as some db only allow a single column to be auto-incrementing
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS u_int_tester (
         *      id BIGSERIAL PRIMARY KEY,
         *
         *      CONSTRAINT chk_u_int_tester_unsigned_id
         *          CHECK (id BETWEEN 0 AND 4294967295)
         * )
         * ```
         */
        val uIntTester = object: Table("u_int_tester") {
            val id = uinteger("id").autoIncrement()
            override val primaryKey = PrimaryKey(id)
        }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS u_long_tester (
         *      id BIGSERIAL PRIMARY KEY
         * )
         * ```
         */
        val uLongTester = object: Table("u_long_tester") {
            val id = ulong("id").autoIncrement()
            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, uIntTester, uLongTester) {
            uIntTester.insert { }
            uIntTester.selectAll().single()[uIntTester.id] shouldBeEqualTo 1u

            uLongTester.insert { }
            uLongTester.selectAll().single()[uLongTester.id] shouldBeEqualTo 1uL
        }
    }

    /**
     * Primary Key 가 복수 개이고, 그 중 autoIncrement 인 컬럼이 있는 경우
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `table with multi PK and auto increment`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS footable (
         *      bar INT,
         *      id BIGSERIAL,
         *      CONSTRAINT pk_FooTable PRIMARY KEY (id, bar)
         * );
         * ```
         */
        val foo = object: IdTable<Long>("FooTable") {
            val bar = integer("bar")
            override val id: Column<EntityID<Long>> = long("id").entityId().autoIncrement()

            override val primaryKey = PrimaryKey(bar, id)
        }

        withTables(testDB, foo) {
            foo.insert {
                it[bar] = 1
            }
            foo.insert {
                it[bar] = 2
            }

            val result = foo.selectAll().map { it[foo.id] to it[foo.bar] }
            result shouldHaveSize 2
            result[0].second shouldBeEqualTo 1
            result[1].second shouldBeEqualTo 2
        }
    }

    /**
     * `TEXT` 수형의 컬럼이 Primary Key 인 경우
     */
    @Test
    fun `primary key on text column in H2`() {
        /**
         * ```sql
         * -- H2
         * CREATE TABLE IF NOT EXISTS TEXT_PK_TABLE (
         *      COLUMN_1 TEXT PRIMARY KEY
         * );
         * ```
         */
        val testTable = object: Table("text_pk_table") {
            val column1 = text("column_1")

            override val primaryKey = PrimaryKey(column1)
        }

        withDb(TestDB.H2) {
            val h2Dialect = currentDialectTest as H2Dialect
            val isOracleMode = h2Dialect.h2Mode == H2Dialect.H2CompatibilityMode.Oracle
            val singleColumnDescription = testTable.columns.single().descriptionDdl(false)

            singleColumnDescription shouldContainIgnoringCase "PRIMARY KEY"

            if (h2Dialect.isSecondVersion && !isOracleMode) {
                SchemaUtils.create(testTable)
                SchemaUtils.drop(testTable)
            } else {
                expectException<ExposedSQLException> {
                    SchemaUtils.create(testTable)
                }
            }
        }
    }

    /**
     * 인덱스 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `indices 01`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t1 (
         *      id INT PRIMARY KEY,
         *      "name" VARCHAR(255) NOT NULL
         * );
         * CREATE INDEX t1_name ON t1 ("name");
         */
        val t = object: Table("t1") {
            val id = integer("id")
            val name = varchar("name", 255).index()

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, t) {
            val alter = SchemaUtils.createIndex(t.indices.first()).single()
            val q = db.identifierManager.quoteString

            log.debug { "Alter: $alter" }

            alter shouldBeEqualTo "CREATE INDEX ${"t1_name".inProperCase()} " +
                    "ON ${"t1".inProperCase()} ($q${"name".inProperCase()}$q)"
        }
    }

    /**
     * 복수의 컬럼으로 구성된 인덱스 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `indices 02`(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS t2 (
         *      id INT PRIMARY KEY,
         *      lvalue INT NOT NULL,
         *      rvalue INT NOT NULL,
         *      "name" VARCHAR(255) NOT NULL
         * );
         *
         * CREATE INDEX t2_name ON t2 ("name");
         * CREATE INDEX t2_lvalue_rvalue ON t2 (lvalue, rvalue);
         */
        val t = object: Table("t2") {
            val id = integer("id")
            val lvalue = integer("lvalue")
            val rvalue = integer("rvalue")
            val name = varchar("name", 255).index()

            override val primaryKey = PrimaryKey(id)

            init {
                index(false, lvalue, rvalue)
            }
        }

        withTables(testDB, t) {
            val q = db.identifierManager.quoteString

            val a1 = SchemaUtils.createIndex(t.indices[0]).single()
            log.debug { "Alter 1: $a1" }
            a1 shouldBeEqualTo "CREATE INDEX ${"t2_name".inProperCase()} " +
                    "ON ${"t2".inProperCase()} ($q${"name".inProperCase()}$q)"

            val a2 = SchemaUtils.createIndex(t.indices[1]).single()
            log.debug { "Alter 2: $a2" }
            a2 shouldBeEqualTo "CREATE INDEX ${"t2_lvalue_rvalue".inProperCase()} " +
                    "ON ${"t2".inProperCase()} " + "(${"lvalue".inProperCase()}, ${"rvalue".inProperCase()})"
        }
    }

    /**
     * H2 DB에서 `TEXT` 수형의 컬럼에 인덱스 생성하기
     */
    @Test
    fun `index on text column in H2`() {
        /**
         * ```sql
         * -- H2
         * CREATE TABLE IF NOT EXISTS TEST_INDEX_TABLE (COLUMN_1 TEXT NOT NULL);
         * CREATE INDEX TEST_INDEX_TABLE_COLUMN_1 ON TEST_INDEX_TABLE (COLUMN_1);
         * ```
         */
        val testTable = object: Table("test_index_table") {
            val column1 = text("column_1")

            init {
                index(false, column1)
            }
        }

        withDb(TestDB.H2) {
            val h2Dialect = currentDialectTest as H2Dialect
            val isOracleMode = h2Dialect.h2Mode == H2Dialect.H2CompatibilityMode.Oracle
            val tableProperName = testTable.tableName.inProperCase()
            val columnProperName = testTable.columns.first().name.inProperCase()
            val indexProperName = "${tableProperName}_${columnProperName}"

            val indexStatement = SchemaUtils.createIndex(testTable.indices.single()).single()

            log.debug { "DDL: ${testTable.ddl.single()}" }

            testTable.ddl.single() shouldBeEqualTo "CREATE TABLE " + addIfNotExistsIfSupported() +
                    tableProperName + " (" + testTable.columns.single().descriptionDdl(false) + ")"

            if (h2Dialect.isSecondVersion && !isOracleMode) {
                log.debug { "Index: $indexStatement" }
                indexStatement shouldBeEqualTo "CREATE INDEX $indexProperName ON $tableProperName ($columnProperName)"
            } else {
                indexStatement.shouldBeEmpty()
            }
        }
    }

    /**
     * UNIQUE 인덱스 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `unique indices 01`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t1 (
         *      id INT PRIMARY KEY,
         *      "name" VARCHAR(255) NOT NULL
         * );
         *
         * ALTER TABLE t1 ADD CONSTRAINT t1_name_unique UNIQUE ("name");
         * ```
         */
        val t = object: Table("t1") {
            val id = integer("id")
            val name = varchar("name", 255).uniqueIndex()

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, t) {
            val q = db.identifierManager.quoteString
            val alter = SchemaUtils.createIndex(t.indices[0]).single()

            log.debug { "Alter: $alter" }

            alter shouldBeEqualTo "ALTER TABLE ${"t1".inProperCase()} " +
                    "ADD CONSTRAINT ${"t1_name_unique".inProperCase()} " +
                    "UNIQUE ($q${"name".inProperCase()}$q)"
        }
    }

    /**
     * Custom name을 가진 UNIQUE 인덱스 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `unique indices custom name`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t1 (
         *      id INT PRIMARY KEY,
         *      "name" VARCHAR(255) NOT NULL
         * );
         *
         * ALTER TABLE t1 ADD CONSTRAINT U_T1_NAME UNIQUE ("name");
         * ```
         */
        val t = object: Table("t1") {
            val id = integer("id")
            val name = varchar("name", 255).uniqueIndex("U_T1_NAME")

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, t) {
            val q = db.identifierManager.quoteString
            val alter = SchemaUtils.createIndex(t.indices[0]).single()

            log.debug { "Alter: $alter" }

            alter shouldBeEqualTo "ALTER TABLE ${"t1".inProperCase()} " +
                    "ADD CONSTRAINT ${"U_T1_NAME"} " +
                    "UNIQUE ($q${"name".inProperCase()}$q)"
        }
    }

    /**
     * 복수의 컬럼으로 구성된 일반 인덱스와 UNIQUE 인덱스 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi column index`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t1 (
         *      "type" VARCHAR(255) NOT NULL,
         *      "name" VARCHAR(255) NOT NULL
         * );
         *
         * CREATE INDEX t1_name_type ON t1 ("name", "type");
         *
         * ALTER TABLE t1
         *      ADD CONSTRAINT t1_type_name_unique UNIQUE ("type", "name");
         */
        val t = object: Table("t1") {
            val type = varchar("type", 255)
            val name = varchar("name", 255)

            init {
                index(false, name, type)
                uniqueIndex(type, name)
            }
        }

        withTables(testDB, t) {
            val q = db.identifierManager.quoteString
            val indexAlter = SchemaUtils.createIndex(t.indices[0]).single()
            val uniqueAlter = SchemaUtils.createIndex(t.indices[1]).single()

            log.debug { "Index Alter: $indexAlter" }
            log.debug { "Unique Alter: $uniqueAlter" }

            indexAlter shouldBeEqualTo "CREATE INDEX ${"t1_name_type".inProperCase()} ON ${"t1".inProperCase()} " +
                    "($q${"name".inProperCase()}$q, $q${"type".inProperCase()}$q)"

            uniqueAlter shouldBeEqualTo "ALTER TABLE ${"t1".inProperCase()} " +
                    "ADD CONSTRAINT ${"t1_type_name_unique".inProperCase()} " +
                    "UNIQUE ($q${"type".inProperCase()}$q, $q${"name".inProperCase()}$q)"
        }
    }

    /**
     * Custom 명의 복수의 컬럼으로 구성된 일반 인덱스와 UNIQUE 인덱스 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi column index custom name`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t1 (
         *      "type" VARCHAR(255) NOT NULL,
         *      "name" VARCHAR(255) NOT NULL
         * );
         *
         * CREATE INDEX I_T1_NAME_TYPE ON t1 ("name", "type");
         *
         *  ALTER TABLE t1
         *      ADD CONSTRAINT U_T1_TYPE_NAME UNIQUE ("type", "name")
         * ```
         */
        val t = object: Table("t1") {
            val type = varchar("type", 255)
            val name = varchar("name", 255)

            init {
                index("I_T1_NAME_TYPE", false, name, type)
                uniqueIndex("U_T1_TYPE_NAME", type, name)
            }
        }

        withTables(testDB, t) {
            val q = db.identifierManager.quoteString
            val indexAlter = SchemaUtils.createIndex(t.indices[0]).single()
            val uniqueAlter = SchemaUtils.createIndex(t.indices[1]).single()

            log.debug { "Index Alter: $indexAlter" }
            log.debug { "Unique Alter: $uniqueAlter" }

            indexAlter shouldBeEqualTo "CREATE INDEX ${"I_T1_NAME_TYPE"} ON ${"t1".inProperCase()} " +
                    "($q${"name".inProperCase()}$q, $q${"type".inProperCase()}$q)"

            uniqueAlter shouldBeEqualTo "ALTER TABLE ${"t1".inProperCase()} " +
                    "ADD CONSTRAINT ${"U_T1_TYPE_NAME"} " +
                    "UNIQUE ($q${"type".inProperCase()}$q, $q${"name".inProperCase()}$q)"
        }
    }

    /**
     * length 지정 없이, `binary` 컬럼 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `binary without length`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in (TestDB.ALL_H2 - TestDB.H2_MYSQL + TestDB.ALL_POSTGRES) }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tablewithbinary (
         *      "binaryColumn" bytea NOT NULL
         * );
         * ```
         */
        val tableWithBinary = object: Table("TableWithBinary") {
            val binaryColumn = binary("binaryColumn")
        }

        fun SizedIterable<ResultRow>.readAsString() = map { String(it[tableWithBinary.binaryColumn]) }

        val exposedBytes = "Exposed".toByteArray()
        val kotlinBytes = "Kotlin".toByteArray()

        withTables(testDB, tableWithBinary) {
            tableWithBinary.insert {
                it[tableWithBinary.binaryColumn] = exposedBytes
            }
            val insertedExposed = tableWithBinary.selectAll().readAsString().single()
            insertedExposed shouldBeEqualTo "Exposed"

            tableWithBinary.insert {
                it[tableWithBinary.binaryColumn] = kotlinBytes
            }

            tableWithBinary.selectAll().readAsString() shouldBeEqualTo listOf("Exposed", "Kotlin")

            val insertedKotlin =
                tableWithBinary.selectAll().where { tableWithBinary.binaryColumn eq kotlinBytes }.readAsString()

            insertedKotlin shouldBeEqualTo listOf("Kotlin")
        }
    }

    /**
     * `binary` 컬럼 생성하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `binary columns`(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t (
         *      "binary" bytea NULL,
         *      "byteCol" bytea NOT NULL
         * );
         * ```
         */
        val t = object: Table("t") {
            val binary = binary("binary", 10).nullable()
            val byteCol = binary("byteCol", 1).clientDefault { byteArrayOf(0) }
        }

        fun SizedIterable<ResultRow>.readAsString() = map { row -> row[t.binary]?.toUtf8String() }

        withTables(testDB, t) {
            t.insert { it[t.binary] = "Hello!".toByteArray() }

            val hello = t.selectAll().readAsString().single()
            hello shouldBeEqualTo "Hello!"

            // INSERT INTO t ("binary", "byteCol") VALUES (World!, )
            val worldBytes = "World!".toByteArray()
            t.insert {
                it[t.binary] = worldBytes
                it[t.byteCol] = byteArrayOf(1)
            }

            t.selectAll().readAsString() shouldBeEqualTo listOf("Hello!", "World!")

            // INSERT INTO t ("binary", "byteCol") VALUES (NULL, )
            t.insert {
                it[t.binary] = null
                it[t.byteCol] = byteArrayOf(2)
            }

            t.selectAll().readAsString() shouldBeEqualTo listOf("Hello!", "World!", null)

            val world = t.selectAll().where { t.binary eq worldBytes }.readAsString().single()
            world shouldBeEqualTo "World!"

            val worldByBitCol = t.selectAll().where { t.byteCol eq byteArrayOf(1) }.readAsString().single()
            worldByBitCol shouldBeEqualTo "World!"
        }
    }

    /**
     * H2 DB에 [VarCharColumnType] 과 [TextColumnType] 의 SQL 생성하기
     */
    @Test
    fun `escape string column type`() {
        withDb(TestDB.H2) {
            VarCharColumnType(collate = "utf8_general_ci").sqlType() shouldBeEqualTo "VARCHAR(255) COLLATE utf8_general_ci"
            VarCharColumnType(collate = "injected'code").sqlType() shouldBeEqualTo "VARCHAR(255) COLLATE injected''code"
            VarCharColumnType().nonNullValueToString("value") shouldBeEqualTo "'value'"
            VarCharColumnType().nonNullValueToString("injected'value") shouldBeEqualTo "'injected''value'"

            TextColumnType(collate = "utf8_general_ci").sqlType() shouldBeEqualTo "TEXT COLLATE utf8_general_ci"
            TextColumnType(collate = "injected'code").sqlType() shouldBeEqualTo "TEXT COLLATE injected''code"
            TextColumnType().nonNullValueToString("value") shouldBeEqualTo "'value'"
            TextColumnType().nonNullValueToString("injected'value") shouldBeEqualTo "'injected''value'"
        }
    }

    private abstract class EntityTable(name: String = ""): IdTable<String>(name) {
        final override val id: Column<EntityID<String>> = varchar("id", 64)
            .clientDefault { Base58.randomString(32) }
            .entityId()

        override val primaryKey = PrimaryKey(id)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `complex test 01`(testDB: TestDB) {
        val user = object: EntityTable() {
            val name = varchar("name", 255)
            val email = varchar("email", 255)
        }
        val repository = object: EntityTable() {
            val name = varchar("name", 255)
        }

        val userToRepo = object: EntityTable() {
            val user = reference("user_id", user)
            val repo = reference("repo_id", repository)
        }

        withTables(testDB, user, repository, userToRepo) {
            val userID = user.insert {
                it[user.name] = "foo"
                it[user.email] = "bar"
            } get user.id

            val repoID = repository.insert {
                it[repository.name] = "foo"
            } get repository.id

            userToRepo.insert {
                it[userToRepo.user] = userID
                it[userToRepo.repo] = repoID
            }

            userToRepo.selectAll().count() shouldBeEqualTo 1L

            userToRepo.insert {
                it[userToRepo.user] = userID
                it[userToRepo.repo] = repoID
            }
            userToRepo.selectAll().count() shouldBeEqualTo 2L
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS table1 (
     *      id SERIAL PRIMARY KEY,
     *      "teamId" INT NOT NULL
     * );
     *
     * ALTER TABLE table1
     *      ADD CONSTRAINT fk_table1_teamid__id FOREIGN KEY ("teamId")
     *      REFERENCES table2(id) ON UPDATE RESTRICT;
     *
     * ```
     */
    object Table1: IntIdTable() {
        val table2 = reference("teamId", Table2, onDelete = ReferenceOption.NO_ACTION)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS table2 (
     *      id SERIAL PRIMARY KEY,
     *      "teamId" INT NULL
     * );
     *
     * ALTER TABLE table2
     *      ADD CONSTRAINT fk_table2_teamid__id FOREIGN KEY ("teamId")
     *      REFERENCES table1(id) ON UPDATE RESTRICT;
     * ```
     */
    object Table2: IntIdTable() {
        val table1 = optReference("teamId", Table1, onDelete = ReferenceOption.NO_ACTION)
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `cross reference`(testDB: TestDB) {
        withTables(testDB, Table1, Table2) {
            val table2id = Table2.insertAndGetId {}
            val table1id = Table1.insertAndGetId {
                it[Table1.table2] = table2id
            }
            Table2.insertAndGetId {
                it[Table2.table1] = table1id
            }

            Table1.selectAll().count() shouldBeEqualTo 1L
            Table2.selectAll().count() shouldBeEqualTo 2L

            Table2.update {
                it[Table2.table1] = null
            }

            Table1.deleteAll()
            Table2.deleteAll()

            /**
             * 이 작업을 해줘야 서로 의존관계에 있는 테이블들을 DROP 할 수 있습니다.
             *
             * ```sql
             * ALTER TABLE table2
             *      DROP CONSTRAINT fk_table2_teamid__id
             * ```
             */
            exec(Table2.table1.foreignKey!!.dropStatement().single())
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS node (
     *      id SERIAL PRIMARY KEY,
     *      uuid uuid NOT NULL
     * )
     *
     * INSERT INTO node (uuid) VALUES ('c1bfb96a-7f89-43a4-af87-d6bbb1d22c43');
     *
     * SELECT node.id, node.uuid
     *   FROM node
     *  WHERE node.id = 1;
     *
     * SELECT node.id, node.uuid
     *   FROM node
     *  WHERE node.uuid = 'c1bfb96a-7f89-43a4-af87-d6bbb1d22c43');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `uuid column type`(testDB: TestDB) {
        val node = object: IntIdTable("node") {
            val uuid = uuid("uuid")
        }

        withTables(testDB, node) {
            val key = UUID.randomUUID()
            val id = node.insertAndGetId { it[uuid] = key }
            id.shouldNotBeNull()

            val uidById = node.selectAll().where { node.id eq id }.single()[node.uuid]
            uidById shouldBeEqualTo key

            val uidByKey = node.selectAll().where { node.uuid eq key }.single()[node.uuid]
            uidByKey shouldBeEqualTo key
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `boolean column type`(testDB: TestDB) {
        val boolTable = object: Table("boolTable") {
            val bool = bool("bool")
        }

        withTables(testDB, boolTable) {
            boolTable.insert {
                it[boolTable.bool] = true
            }

            val result = boolTable.selectAll().toList()
            result shouldHaveSize 1
            result.single()[boolTable.bool].shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `table with different text type`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES_LIKE + TestDB.MYSQL_V5 }

        /**
         * Postgres:
         * ```sql
         * CREATE TABLE IF NOT EXISTS differnt_text_column_types (
         *      id SERIAL PRIMARY KEY,
         *      txt TEXT NOT NULL,
         *      "txtMed" TEXT NOT NULL,
         *      "txtLong" TEXT NOT NULL
         * );
         * ```
         */
        val testTable = object: Table("different_text_column_types") {
            val id = integer("id").autoIncrement()
            val txt = text("txt")
            val txtMed = mediumText("txt_med")
            val txtLong = largeText("txt_large")

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, testTable) {
            testTable.ddl.single() shouldBeEqualTo
                    "CREATE TABLE " + addIfNotExistsIfSupported() + "${"different_text_column_types".inProperCase()} " +
                    "(${testTable.id.nameInDatabaseCase()} ${currentDialectTest.dataTypeProvider.integerAutoincType()} PRIMARY KEY, " +
                    "${testTable.txt.nameInDatabaseCase()} ${currentDialectTest.dataTypeProvider.textType()} NOT NULL, " +
                    "${testTable.txtMed.nameInDatabaseCase()} ${currentDialectTest.dataTypeProvider.mediumTextType()} NOT NULL, " +
                    "${testTable.txtLong.nameInDatabaseCase()} ${currentDialectTest.dataTypeProvider.largeTextType()} NOT NULL)"

            assertTrue {
                testDB != TestDB.MYSQL_V5 || (
                        currentDialectTest.dataTypeProvider.textType() != currentDialectTest.dataTypeProvider.mediumTextType() &&
                                currentDialectTest.dataTypeProvider.mediumTextType() != currentDialectTest.dataTypeProvider.largeTextType() &&
                                currentDialectTest.dataTypeProvider.largeTextType() != currentDialectTest.dataTypeProvider.textType()
                        )
            }

            testTable.insert {
                it[txt] = "1Txt"
                it[txtMed] = "1TxtMed"
                it[txtLong] = "1TxtLong"
            }

            val concat = SqlExpressionBuilder.concat(
                separator = " ",
                listOf(LowerCase(testTable.txt), UpperCase(testTable.txtMed), LowerCase(testTable.txtLong))
            )

            // just to be sure new type didn't break the functions
            testTable.select(concat).forEach {
                it[concat] shouldBeEqualTo "1txt 1TXTMED 1txtlong"
            }

        }
    }

    /**
     * drop table 시에는 `IF EXISTS` 를 사용합니다.
     *
     * ```sql
     * DROP TABLE IF EXISTS missingtable
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete missing table`(testDB: TestDB) {
        val missingTable = Table("missingTable")
        withDb(testDB) {
            SchemaUtils.drop(missingTable)
        }
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS checktable (
     *      positive INT NOT NULL,
     *      negative INT NOT NULL,
     *
     *      CONSTRAINT check_checkTable_0 CHECK (positive >= 0),
     *      CONSTRAINT check_checkTable_1 CHECK (negative < 0)
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `check contraints 01`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        val checkTable = object: Table("checkTable") {
            val positive = integer("positive").check { it greaterEq 0 }
            val negative = integer("negative").check { it less 0 }
        }

        withTables(testDB, checkTable) {
            checkTable.insert {
                it[positive] = 1
                it[negative] = -1
            }

            checkTable.selectAll().count() shouldBeEqualTo 1L

            assertFailAndRollback("Check constraint 1") {
                checkTable.insert {
                    it[positive] = -1
                    it[negative] = -1
                }
            }
            assertFailAndRollback("Check constraint 2") {
                checkTable.insert {
                    it[positive] = 1
                    it[negative] = 1
                }
            }
        }
    }

    /**
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS multichecktable (
     *      positive INT NOT NULL,
     *      negative INT NOT NULL,
     *
     *      CONSTRAINT multi CHECK ((positive >= 0) AND (negative < 0))
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `check constraint 02`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }

        val checkTable = object: Table("multiCheckTable") {
            val positive = integer("positive")
            val negative = integer("negative")

            init {
                check("multi") {
                    (positive greaterEq 0) and (negative less 0)
                }
            }
        }

        withTables(testDB, checkTable) {
            checkTable.insert {
                it[positive] = 1
                it[negative] = -1
            }

            checkTable.selectAll().count() shouldBeEqualTo 1L

            assertFailAndRollback("Check constraint 1") {
                checkTable.insert {
                    it[positive] = -1
                    it[negative] = -1
                }
            }
            assertFailAndRollback("Check constraint 2") {
                checkTable.insert {
                    it[positive] = 1
                    it[negative] = 1
                }
            }
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (amount INT NOT NULL);
     *
     * ALTER TABLE tester ADD CONSTRAINT check_amount_positive CHECK (amount > 0);
     *
     * ALTER TABLE tester DROP CONSTRAINT check_amount_positive;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create and drop check constraint`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val amount = integer("amount")
        }

        withTables(testDB, tester) {
            val constraintName = "check_amount_positive"
            val constraintOp = "${"amount".inProperCase()} > 0"

            val checkConstraint = CheckConstraint("tester", constraintName, constraintOp)
            val createConstraint = checkConstraint.createStatement()
            val dropConstraint = checkConstraint.dropStatement()

            if (testDB in listOf(TestDB.MYSQL_V5)) {
                createConstraint.shouldBeEmpty()
                dropConstraint.shouldBeEmpty()
            } else {
                val negative = -9
                tester.insert { it[amount] = negative }

                // fails to create check constraint because negative values already stored
                assertFailAndRollback("Check constraint violation") {
                    exec(createConstraint.single())
                }

                tester.deleteAll()
                exec(createConstraint.single())

                assertFailAndRollback("Check constraint violation") {
                    tester.insert { it[amount] = negative }
                }

                exec(dropConstraint.single())

                tester.insert { it[amount] = negative }
                tester.selectAll().single()[tester.amount] shouldBeEqualTo negative
            }
        }
    }

    @Test
    fun `eq operator without DB connection`() {
        object: Table("test") {
            val testColumn = integer("test_column").nullable()

            init {
                check("test_constraint") {
                    testColumn.isNotNull() eq Op.TRUE
                }
            }
        }
    }

    @Test
    fun `neq operator without DB connection`() {
        object: Table("test") {
            val testColumn = integer("test_column").nullable()

            init {
                check("test_constraint") {
                    testColumn.isNotNull() neq Op.TRUE
                }
            }
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS keywords (
     *      id SERIAL PRIMARY KEY,
     *      bool BOOLEAN NOT NULL
     * )
     * ```
     */
    object KeyWordTable: IntIdTable("keywords") {
        val bool = bool("bool")
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS keywords (
     *      id SERIAL PRIMARY KEY,
     *      bool BOOLEAN NOT NULL
     * );
     *
     * INSERT INTO keywords (bool) VALUES (TRUE);
     *
     * DROP TABLE IF EXISTS keywords;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `drop table flushes cache`(testDB: TestDB) {
        class Keyword(id: EntityID<Int>): IntEntity(id) {
            var bool by KeyWordTable.bool
        }

        val keywordEntityClass = object: IntEntityClass<Keyword>(KeyWordTable, Keyword::class.java) {}

        withTables(testDB, KeyWordTable) {
            keywordEntityClass.new { bool = true }
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS users (
     *      id SERIAL PRIMARY KEY
     * );
     *
     * CREATE TABLE IF NOT EXISTS subscriptions (
     *      id SERIAL PRIMARY KEY,
     *      "userId" INT NOT NULL,
     *      "adminId" INT NULL,
     *
     *      CONSTRAINT fk_subscriptions_userid__id FOREIGN KEY ("userId")
     *      REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *      CONSTRAINT fk_subscriptions_adminid__id FOREIGN KEY ("adminId")
     *      REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * SELECT COUNT(*)
     *   FROM subscriptions INNER JOIN users ON (subscriptions."userId" = users.id);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inner join with multiple foreignkey`(testDB: TestDB) {
        val users = object: IntIdTable("users") {}
        val subscriptions = object: IntIdTable("subscriptions") {
            val userId = reference("userId", users)
            val adminId = reference("adminId", users).nullable()
        }

        withTables(testDB, subscriptions) {
            val query = subscriptions
                .join(users, JoinType.INNER, additionalConstraint = { subscriptions.userId eq users.id })
                .selectAll()

            query.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create table with foreign key to another schema`(testDB: TestDB) {
        // 보안때문에 `exposed` 계정으로는 다른 스키마에 접근할 수 없습니다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        val one = prepareSchemaForTest("one")
        val two = prepareSchemaForTest("two")

        withSchemas(testDB, two, one) {
            SchemaUtils.create(TableFromSchemeOne)
            SchemaUtils.create(TableFromSchemeTwo)

            val idFromOne = TableFromSchemeOne.insertAndGetId {}

            TableFromSchemeTwo.insert {
                it[reference] = idFromOne
            }

            TableFromSchemeOne.selectAll().count() shouldBeEqualTo 1L
            TableFromSchemeTwo.selectAll().count() shouldBeEqualTo 1L
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS one.test (id SERIAL PRIMARY KEY)
     * ```
     */
    object TableFromSchemeOne: IntIdTable("one.test")

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS two.test (
     *      id SERIAL PRIMARY KEY,
     *      "testOne" INT NOT NULL,
     *
     *      CONSTRAINT fk_test_testone__id FOREIGN KEY ("testOne")
     *      REFERENCES one.test(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object TableFromSchemeTwo: IntIdTable("two.test") {
        val reference = reference("testOne", TableFromSchemeOne)
    }

    /**
     * ```sql
     * -- TableA
     * CREATE TABLE IF NOT EXISTS tablea (
     *      id_a INT NOT NULL,
     *      id_b INT NOT NULL
     * );
     * ALTER TABLE tablea
     *      ADD CONSTRAINT tablea_id_a_id_b_unique UNIQUE (id_a, id_b);
     * ```
     *
     * ```sql
     * -- TableB
     * CREATE TABLE IF NOT EXISTS tableb (
     *      id_a INT,
     *      id_b INT,
     *      id_c INT,
     *
     *      CONSTRAINT pk_TableB PRIMARY KEY (id_a, id_b, id_c),
     *
     *      CONSTRAINT fk_tableb_id_a_id_b__id_a_id_b FOREIGN KEY (id_a, id_b)
     *      REFERENCES tablea(id_a, id_b) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `composite FK referencing unique index`(testDB: TestDB) {
        val tableA = object: Table("TableA") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            init {
                uniqueIndex(idA, idB)
            }
        }

        val tableB = object: Table("TableB") {
            val idA = integer("id_a")
            val idB = integer("id_b")
            val idC = integer("id_c")

            override val primaryKey = PrimaryKey(idA, idB, idC)

            init {
                foreignKey(idA to tableA.idA, idB to tableA.idB)
            }
        }

        withTables(testDB, tableA, tableB) {
            tableA.insert {
                it[tableA.idA] = 1
                it[tableA.idB] = 2
            }
            tableB.insert {
                it[tableB.idA] = 1
                it[tableB.idB] = 2
                it[tableB.idC] = 3
            }

            assertFailAndRollback(
                "check violation composite foreign key constraint (insert key into child table not present in parent table)"
            ) {
                tableB.insert {
                    it[tableB.idA] = 1
                    it[tableB.idB] = 1
                    it[tableB.idC] = 3
                }
            }
        }
    }

    /**
     * ```sql
     * -- TableA
     * CREATE TABLE IF NOT EXISTS tablea (
     *      id_a INT,
     *      id_b INT,
     *
     *      CONSTRAINT pk_TableA PRIMARY KEY (id_a, id_b)
     * );
     *
     * -- TableB
     * CREATE TABLE IF NOT EXISTS tableb (
     *      id_a INT,
     *      id_b INT,
     *      id_c INT,
     *
     *      CONSTRAINT pk_TableB PRIMARY KEY (id_a, id_b, id_c),
     *
     *      CONSTRAINT fk_tableb_id_a_id_b__id_a_id_b FOREIGN KEY (id_a, id_b)
     *      REFERENCES tablea(id_a, id_b) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `composite FK referencing primary key`(testDB: TestDB) {
        val tableA = object: Table("TableA") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            override val primaryKey = PrimaryKey(idA, idB)
        }

        val tableB = object: Table("TableB") {
            val idA = integer("id_a")
            val idB = integer("id_b")
            val idC = integer("id_c")

            override val primaryKey = PrimaryKey(idA, idB, idC)

            init {
                foreignKey(idA, idB, target = tableA.primaryKey)
            }
        }

        withTables(testDB, tableA, tableB) {
            tableA.insert {
                it[tableA.idA] = 1
                it[tableA.idB] = 2
            }
            tableB.insert {
                it[tableB.idA] = 1
                it[tableB.idB] = 2
                it[tableB.idC] = 3
            }

            assertFailAndRollback(
                "check violation composite foreign key constraint (insert key into child table not present in parent table)"
            ) {
                tableB.insert {
                    it[tableB.idA] = 1
                    it[tableB.idB] = 1
                    it[tableB.idC] = 3
                }
            }
        }
    }

    /**
     * ```sql
     * -- TableA
     * CREATE TABLE IF NOT EXISTS tablea (
     *      id_a INT,
     *      id_b INT,
     *
     *      CONSTRAINT pk_TableA PRIMARY KEY (id_a, id_b)
     * );
     * ```
     *
     * ```sql
     * -- TableC
     * CREATE TABLE IF NOT EXISTS tablec (
     *      id_c INT NOT NULL
     * );
     *
     * ALTER TABLE tablec ADD CONSTRAINT tablec_id_c_unique UNIQUE (id_c);
     * ```
     *
     * ```sql
     * -- TableB
     * CREATE TABLE IF NOT EXISTS tableb (
     *      id_a INT,
     *      id_b INT,
     *      id_c INT,
     *
     *      CONSTRAINT pk_TableB PRIMARY KEY (id_a, id_b, id_c),
     *
     *      CONSTRAINT fk_tableb_id_c__id_c FOREIGN KEY (id_c)
     *      REFERENCES tablec(id_c) ON DELETE RESTRICT ON UPDATE RESTRICT,
     *
     *      CONSTRAINT fk_tableb_id_a_id_b__id_a_id_b FOREIGN KEY (id_a, id_b)
     *      REFERENCES tablea(id_a, id_b) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multiple FK`(testDB: TestDB) {
        val tableA = object: Table("TableA") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            override val primaryKey = PrimaryKey(idA, idB)
        }

        val tableC = object: Table("TableC") {
            val idC = integer("id_c").uniqueIndex()
        }

        val tableB = object: Table("TableB") {
            val idA = integer("id_a")
            val idB = integer("id_b")
            val idC = integer("id_c") references tableC.idC

            override val primaryKey = PrimaryKey(idA, idB, idC)

            init {
                foreignKey(idA, idB, target = tableA.primaryKey)
            }
        }

        withTables(testDB, tableA, tableB, tableC) {
            tableA.insert {
                it[tableA.idA] = 1
                it[tableA.idB] = 2
            }

            tableC.insert {
                it[tableC.idC] = 3
            }

            tableB.insert {
                it[tableB.idA] = 1
                it[tableB.idB] = 2
                it[tableB.idC] = 3
            }

            assertFailAndRollback(
                "check violation composite foreign key constraint (insert key into child table not present in parent table)"
            ) {
                tableB.insert {
                    it[tableB.idA] = 1
                    it[tableB.idB] = 1
                    it[tableB.idC] = 3
                }
            }

            assertFailAndRollback(
                "check violation foreign key constraint (insert key into child table not present in parent table)"
            ) {
                tableB.insert {
                    it[tableB.idA] = 1
                    it[tableB.idB] = 2
                    it[tableB.idC] = 1
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create table with composite primary key and schema`(testDB: TestDB) {
        // 권한 설정 때문에 MySQL의 Schema 생성에 실패한다.
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        val one = prepareSchemaForTest("test")

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS test.table_a (
         *      id_a INT,
         *      id_b INT,
         *
         *      CONSTRAINT pk_table_a PRIMARY KEY (id_a, id_b)
         * );
         * ```
         */
        val tableA = object: Table("test.table_a") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            override val primaryKey = PrimaryKey(idA, idB)
        }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS test.table_b (
         *      id_a INT,
         *      id_b INT,
         *
         *      CONSTRAINT pk_table_b PRIMARY KEY (id_a, id_b)
         * );
         * ```
         */
        val tableB = object: Table("test.table_b") {
            val idA = integer("id_a")
            val idB = integer("id_b")

            override val primaryKey = PrimaryKey(arrayOf(idA, idB))
        }

        withSchemas(testDB, one) {
            SchemaUtils.create(tableA, tableB)

            tableA.insert {
                it[tableA.idA] = 1
                it[tableA.idB] = 2
            }

            tableB.insert {
                it[tableB.idA] = 3
                it[tableB.idB] = 4
            }

            tableA.selectAll().count() shouldBeEqualTo 1L
            tableB.selectAll().count() shouldBeEqualTo 1L
        }
    }

}
