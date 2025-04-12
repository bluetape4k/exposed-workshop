package exposed.examples.java.time

import MigrationUtils
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.constraintNamePart
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.inProperCase
import exposed.shared.tests.insertAndWait
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.ExpressionWithColumnType
import org.jetbrains.exposed.sql.IColumnType
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.javatime.CurrentDate
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.sql.javatime.JavaOffsetDateTimeColumnType
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.dateLiteral
import org.jetbrains.exposed.sql.javatime.dateTimeLiteral
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.javatime.durationLiteral
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timeLiteral
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.javatime.timestampLiteral
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZoneLiteral
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.BatchDataInconsistentException
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.vendors.H2Dialect.H2CompatibilityMode.Oracle
import org.jetbrains.exposed.sql.vendors.MysqlDialect
import org.jetbrains.exposed.sql.vendors.OracleDialect
import org.jetbrains.exposed.sql.vendors.SQLServerDialect
import org.jetbrains.exposed.sql.vendors.SQLiteDialect
import org.jetbrains.exposed.sql.vendors.h2Mode
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/**
 * DB의 `now()` 함수를 사용하는 Custom Function
 */
private val dbTimestampNow: CustomFunction<OffsetDateTime>
    get() = object: CustomFunction<OffsetDateTime>("now", JavaOffsetDateTimeColumnType()) {}

/**
 * Java Time 형식의 컬럼에 기본값 설정과 관련된 테스트
 */
class Ex02_Defaults: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS t_db_default (
     *      id SERIAL PRIMARY KEY,
     *      field VARCHAR(100) NOT NULL,
     *      t1 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     *      "clientDefault" INT NOT NULL
     * )
     * ```
     */
    object TableWithDBDefault: IntIdTable("t_db_default") {
        val cIndex = AtomicInteger(0)

        val field = varchar("field", 100)
        val t1: Column<LocalDateTime> = datetime("t1").defaultExpression(CurrentDateTime)
        val t2: Column<LocalDate> = date("t2").defaultExpression(CurrentDate)
        val t3: Column<Instant> = timestamp("t3").defaultExpression(CurrentTimestamp)
        val t4: Column<OffsetDateTime> = timestampWithTimeZone("t5").defaultExpression(CurrentTimestampWithTimeZone)
        val clientDefault = integer("clientDefault").clientDefault { cIndex.getAndIncrement() }

        init {
            cIndex.set(0)
        }
    }

    class DBDefault(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<DBDefault>(TableWithDBDefault)

        var field by TableWithDBDefault.field
        var t1 by TableWithDBDefault.t1
        var clientDefault by TableWithDBDefault.clientDefault

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.value.hashCode()
        override fun toString(): String = toStringBuilder()
            .add("field", field)
            .add("t1", t1)
            .add("clientDefault", clientDefault)
            .toString()
    }

    /**
     * nullable 컬럼에 Client 기본값 (`clientDefault`) 을 설정할 수 없다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `can use client default on nullable column`(testDB: TestDB) {
        val defaultValue: Int? = null
        val table = object: IntIdTable("tester") {
            val clientDefault = integer("clientDefault").nullable().clientDefault { defaultValue }
        }

        val returnedDefault = table.clientDefault.defaultValueFun?.invoke()

        table.clientDefault.columnType.nullable.shouldBeTrue()
        table.clientDefault.defaultValueFun.shouldNotBeNull()
        returnedDefault shouldBeEqualTo defaultValue
    }

    /**
     * nullable 컬럼이지만 Exposed의 `clientDefault`를 사용하여 기본값을 설정할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `can set nullable column to use client default`(testDB: TestDB) {
        val defaultValue = 123
        val table = object: IntIdTable("tester") {
            val clientDefault = integer("clientDefault").nullable().clientDefault { defaultValue }
        }

        val returnedDefault = table.clientDefault.defaultValueFun?.invoke()

        table.clientDefault.columnType.nullable.shouldBeTrue()
        table.clientDefault.defaultValueFun.shouldNotBeNull()
        returnedDefault shouldBeEqualTo defaultValue
    }

    /**
     * [TableWithDBDefault] 테이블에 기본값을 설정하고, 생성된 엔티티를 조회한다.
     *
     * ```sql
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('1', 8);
     *
     * INSERT INTO t_db_default (field, t1, "clientDefault")
     * VALUES ('2', '2025-01-30T08:42:03.129272', 9);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `defaults with explicit 01`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            val created = listOf(
                DBDefault.new { field = "1" },
                DBDefault.new {
                    field = "2"
                    t1 = LocalDateTime.now().minusDays(5)
                }
            )
            commit()
            created.forEach {
                DBDefault.removeFromCache(it)
            }

            val entities = DBDefault.all().toList()
            entities shouldBeEqualTo created
        }
    }

    /**
     * 기본 값으로 저장되는지 확인한다.
     *
     * ```sql
     * INSERT INTO t_db_default (field, t1, "clientDefault") VALUES ('2', '2025-01-30T08:42:03.182995', 18)
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('1', 19)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `defaults with explicit 02`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            val created = listOf(
                DBDefault.new {
                    field = "2"
                    t1 = LocalDateTime.now().minusDays(5)
                },
                DBDefault.new { field = "1" }
            )
            flushCache()
            created.forEach {
                DBDefault.removeFromCache(it)
            }

            val entities = DBDefault.all().toList()
            entities shouldBeEqualTo created
        }
    }

    /**
     * 기본값 설정을 위해 `clientDefault` 를 사용할 때, 기본값 초기화는 한번만 이루어져야 한다.
     *
     * ```sql
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('1', 0);
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('2', 1);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `defaults invoked only once per entity`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            TableWithDBDefault.cIndex.set(0)
            val db1 = DBDefault.new { field = "1" }
            val db2 = DBDefault.new { field = "2" }
            flushCache()

            db1.clientDefault shouldBeEqualTo 0
            db2.clientDefault shouldBeEqualTo 1
            TableWithDBDefault.cIndex.get() shouldBeEqualTo 2
        }
    }

    /**
     * 엔티티의 Client Default 값은 재정의할 수 있습니다.
     *
     * ```sql
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('1', 12345);
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('2', 1);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `defaults can be overriden`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            TableWithDBDefault.cIndex.set(0)

            val db1 = DBDefault.new { field = "1" }
            val db2 = DBDefault.new { field = "2" }
            db1.clientDefault = 12345

            flushCache()

            db1.clientDefault shouldBeEqualTo 12345
            db2.clientDefault shouldBeEqualTo 1
            TableWithDBDefault.cIndex.get() shouldBeEqualTo 2

            flushCache()

            db1.clientDefault shouldBeEqualTo 12345
        }
    }

    private val initBatch = listOf<(BatchInsertStatement) -> Unit>(
        {
            it[TableWithDBDefault.field] = "1"
        },
        {
            it[TableWithDBDefault.field] = "2"
            it[TableWithDBDefault.t1] = LocalDateTime.now()
        }
    )

    /**
     * Database Default 를 사용하는 경우에는 `addBatch` 를 호출하면 예외가 발생합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `raw batch insert fails 01`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            expectException<BatchDataInconsistentException> {
                BatchInsertStatement(TableWithDBDefault).run {
                    initBatch.forEach {
                        addBatch()
                        it(this)
                    }
                }
            }
        }
    }

    /**
     *  `batchInsert` 호출 시에 기본값 때문에 `addBatch`를 사용하지 않아야 제대로 수행된다.
     *
     * ```sql
     * INSERT INTO t_db_default (field, "clientDefault") VALUES ('1', 28);
     * INSERT INTO t_db_default (field, t1, "clientDefault") VALUES ('2', '2025-02-04T08:42:28.637826', 29);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert not fails 01`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            TableWithDBDefault.batchInsert(initBatch) {
                it(this)
            }
        }
    }

    /**
     * Batch Insert 시에 Database Default 를 사용하는 경우에는 값을 넣지 않아야 한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert fails 01`(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            expectException<BatchDataInconsistentException> {
                TableWithDBDefault.batchInsert(initBatch) {
                    // t1 은 Database Default 값이 이미 설정되어 있으므로 추가로 값을 설정하지 않습니다.
                    this[TableWithDBDefault.t1] = LocalDateTime.now()
                }
            }
        }
    }

    /**
     * 컬럼의 다양한 기본값이 정상적으로 설정되는지 확인한다. (`default`, `defaultExpression`)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `defaults 01`(testDB: TestDB) {
        val currentDT = CurrentDateTime
        val nowExpression = object: Expression<LocalDateTime>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
                +when (val dialect = currentDialectTest) {
                    is OracleDialect -> "SYSDATE"
                    is SQLServerDialect -> "GETDATE()"
                    is MysqlDialect -> if (dialect.isFractionDateTimeSupported()) "NOW(6)" else "NOW()"
                    is SQLiteDialect -> "CURRENT_TIMESTAMP"
                    else -> "NOW()"
                }
            }
        }
        val dtConstValue = LocalDate.of(2010, 1, 1)
        val dLiteral = dateLiteral(dtConstValue)
        val dtLiteral = dateTimeLiteral(dtConstValue.atStartOfDay())
        val tsConstValue = dtConstValue.atStartOfDay(ZoneOffset.UTC).plusSeconds(42).toInstant()
        val tsLiteral = timestampLiteral(tsConstValue)
        val durConstValue = Duration.between(Instant.EPOCH, tsConstValue)
        val durLiteral = durationLiteral(durConstValue)
        val tmConstValue = LocalTime.of(12, 0)
        val tLiteral = timeLiteral(tmConstValue)

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS t (
         *      id SERIAL PRIMARY KEY,
         *      s VARCHAR(100) DEFAULT 'test' NOT NULL,
         *      sn VARCHAR(100) DEFAULT 'testNullable' NULL,
         *      l BIGINT DEFAULT 42 NOT NULL,
         *      "c" CHAR DEFAULT 'X' NOT NULL,
         *      t1 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
         *      t2 TIMESTAMP DEFAULT (NOW()) NOT NULL,
         *      t3 TIMESTAMP DEFAULT '2010-01-01 00:00:00'::timestamp without time zone NOT NULL,
         *      t4 DATE DEFAULT '2010-01-01'::date NOT NULL,
         *      t5 TIMESTAMP DEFAULT '2010-01-01 00:00:42'::timestamp without time zone NOT NULL,
         *      t6 TIMESTAMP DEFAULT '2010-01-01 00:00:42'::timestamp without time zone NOT NULL,
         *      t7 BIGINT DEFAULT '1262304042000000000' NOT NULL,
         *      t8 BIGINT DEFAULT '1262304042000000000' NOT NULL,
         *      t9 TIME DEFAULT '12:00:00'::time without time zone NOT NULL,
         *      t10 TIME DEFAULT '12:00:00'::time without time zone NOT NULL
         * )
         * ```
         */
        val testTable = object: IntIdTable("t") {
            val s = varchar("s", 100).default("test")
            val sn = varchar("sn", 100).default("testNullable").nullable()
            val l = long("l").default(42)
            val c = char("c").default('X')
            val t1 = datetime("t1").defaultExpression(currentDT)
            val t2 = datetime("t2").defaultExpression(nowExpression)
            val t3 = datetime("t3").defaultExpression(dtLiteral)
            val t4 = date("t4").default(dtConstValue)
            val t5 = timestamp("t5").default(tsConstValue)
            val t6 = timestamp("t6").defaultExpression(tsLiteral)
            val t7 = duration("t7").default(durConstValue)
            val t8 = duration("t8").defaultExpression(durLiteral)
            val t9 = time("t9").default(tmConstValue)
            val t10 = time("t10").defaultExpression(tLiteral)
        }

        fun Expression<*>.itOrNull() = when {
            currentDialectTest.isAllowedAsColumnDefault(this) ->
                "DEFAULT ${currentDialectTest.dataTypeProvider.processForDefaultValue(this)} NOT NULL"
            else -> "NULL"
        }

        withTables(testDB, testTable) {
            val dtType = currentDialectTest.dataTypeProvider.dateTimeType()
            val dType = currentDialectTest.dataTypeProvider.dateType()
            val timestampType = currentDialectTest.dataTypeProvider.timestampType()
            val longType = currentDialectTest.dataTypeProvider.longType()
            val timeType = currentDialectTest.dataTypeProvider.timeType()
            val varcharType = currentDialectTest.dataTypeProvider.varcharType(100)
            val q = db.identifierManager.quoteString
            val baseExpression = "CREATE TABLE " + addIfNotExistsIfSupported() +
                    "${"t".inProperCase()} (" +
                    "${"id".inProperCase()} ${currentDialectTest.dataTypeProvider.integerAutoincType()}${
                        " PRIMARY KEY"
                    }, " +
                    "${"s".inProperCase()} $varcharType${testTable.s.constraintNamePart()} DEFAULT 'test' NOT NULL, " +
                    "${"sn".inProperCase()} $varcharType${testTable.sn.constraintNamePart()} DEFAULT 'testNullable' NULL, " +
                    "${"l".inProperCase()} ${currentDialectTest.dataTypeProvider.longType()}${testTable.l.constraintNamePart()} DEFAULT 42 NOT NULL, " +
                    "$q${"c".inProperCase()}$q CHAR${testTable.c.constraintNamePart()} DEFAULT 'X' NOT NULL, " +
                    "${"t1".inProperCase()} $dtType${testTable.t1.constraintNamePart()} ${currentDT.itOrNull()}, " +
                    "${"t2".inProperCase()} $dtType${testTable.t2.constraintNamePart()} ${nowExpression.itOrNull()}, " +
                    "${"t3".inProperCase()} $dtType${testTable.t3.constraintNamePart()} ${dtLiteral.itOrNull()}, " +
                    "${"t4".inProperCase()} $dType${testTable.t4.constraintNamePart()} ${dLiteral.itOrNull()}, " +
                    "${"t5".inProperCase()} $timestampType${testTable.t5.constraintNamePart()} ${tsLiteral.itOrNull()}, " +
                    "${"t6".inProperCase()} $timestampType${testTable.t6.constraintNamePart()} ${tsLiteral.itOrNull()}, " +
                    "${"t7".inProperCase()} $longType${testTable.t7.constraintNamePart()} ${durLiteral.itOrNull()}, " +
                    "${"t8".inProperCase()} $longType${testTable.t8.constraintNamePart()} ${durLiteral.itOrNull()}, " +
                    "${"t9".inProperCase()} $timeType${testTable.t9.constraintNamePart()} ${tLiteral.itOrNull()}, " +
                    "${"t10".inProperCase()} $timeType${testTable.t10.constraintNamePart()} ${tLiteral.itOrNull()}" +
//                    when (testDB) {
//                        TestDB.SQLITE, TestDB.ORACLE ->
//                            ", CONSTRAINT chk_t_signed_integer_id CHECK (${"id".inProperCase()} BETWEEN ${Int.MIN_VALUE} AND ${Int.MAX_VALUE})"
//                        else -> ""
//                    } +
                    ")"

            val expected =
                if (currentDialectTest is OracleDialect || currentDialectTest.h2Mode == Oracle) {
                    arrayListOf(
                        "CREATE SEQUENCE t_id_seq START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807",
                        baseExpression
                    )
                } else {
                    arrayListOf(baseExpression)
                }

            log.debug { "Expected: $expected" }
            log.debug { "Actual: ${testTable.ddl}" }
            testTable.ddl shouldBeEqualTo expected.toList()

            val id1 = testTable.insertAndGetId { }

            val row1 = testTable.selectAll().where { testTable.id eq id1 }.single()
            row1[testTable.s] shouldBeEqualTo "test"
            row1[testTable.sn] shouldBeEqualTo "testNullable"
            row1[testTable.l] shouldBeEqualTo 42
            row1[testTable.c] shouldBeEqualTo 'X'
            row1[testTable.t3] shouldTemporalEqualTo dtConstValue.atStartOfDay()
            row1[testTable.t4] shouldTemporalEqualTo dtConstValue
            row1[testTable.t5] shouldTemporalEqualTo tsConstValue
            row1[testTable.t6] shouldTemporalEqualTo tsConstValue
            row1[testTable.t7] shouldBeEqualTo durConstValue
            row1[testTable.t8] shouldBeEqualTo durConstValue
            row1[testTable.t9] shouldBeEqualTo tmConstValue
            row1[testTable.t10] shouldBeEqualTo tmConstValue
        }
    }

    /**
     * 컬럼의 기본값을 Custom Expression 으로 설정하기
     *
     * ```sql
     * -- Postgres:
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "name" TEXT NOT NULL,
     *      "defaultDateTime" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     *      "defaultDate" DATE DEFAULT CURRENT_DATE NOT NULL,
     *      "defaultInt" INT DEFAULT (ABS(-100)) NOT NULL
     * );
     *
     * -- MySQL V8:
     * CREATE TABLE IF NOT EXISTS tester (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` text NOT NULL,
     *      defaultDateTime DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
     *      defaultDate DATE DEFAULT (CURRENT_DATE()) NOT NULL,
     *      defaultInt INT DEFAULT (ABS(-100)) NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `default expressions 01`(testDB: TestDB) {
        /**
         * `ABS` 함수를 사용하는 Custom Expression
         */
        fun abs(value: Int) = object: ExpressionWithColumnType<Int>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { append("ABS($value)") }
            override val columnType: IColumnType<Int> = IntegerColumnType()
        }

        val tester = object: IntIdTable("tester") {
            val name = text("name")
            val defaultDateTime = datetime("defaultDateTime").defaultExpression(CurrentDateTime)
            val defaultDate = date("defaultDate").defaultExpression(CurrentDate)
            val defaultInt = integer("defaultInt").defaultExpression(abs(-100))
        }

        // MySql 5 is excluded because it does not support `CURRENT_DATE()` as a default value
        Assumptions.assumeTrue { testDB !in setOf(TestDB.MYSQL_V5) }
        withTables(testDB, tester) {
            val id = tester.insertAndGetId {
                it[tester.name] = "bar"
            }
            val result = tester.selectAll().where { tester.id eq id }.single()

            result[tester.defaultDateTime].toLocalDate() shouldBeEqualTo today
            result[tester.defaultDate] shouldBeEqualTo today
            result[tester.defaultInt] shouldBeEqualTo 100
        }
    }

    /**
     * 컬럼의 기본값을 DB에서 제공하는 `CurrentDateTime`, `CurrentDate` 로 설정하기
     *
    ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "name" TEXT NOT NULL,
     *      "defaultDateTime" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * )
     * ```
     *
     * @see [CurrentDateTime]
     * @see [CurrentDate]
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `default expressions 02`(testDB: TestDB) {
        val tester = object: IntIdTable("tester") {
            val name = text("name")
            val defaultDateTime = datetime("defaultDateTime").defaultExpression(CurrentDateTime)
        }

        val nonDefaultDate = LocalDate.of(2000, 1, 1).atStartOfDay()

        withTables(testDB, tester) {
            // INSERT INTO tester ("name", "defaultDateTime") VALUES ('bar', '2000-01-01T00:00:00')
            val id = tester.insertAndGetId {
                it[tester.name] = "bar"
                it[tester.defaultDateTime] = nonDefaultDate
            }

            val result = tester.selectAll().where { tester.id eq id }.single()
            result[tester.name] shouldBeEqualTo "bar"
            result[tester.defaultDateTime] shouldBeEqualTo nonDefaultDate

            tester.update({ tester.id eq id }) {
                it[tester.name] = "baz"
            }

            val result2 = tester.selectAll().where { tester.id eq id }.single()
            result2[tester.name] shouldBeEqualTo "baz"
            result2[tester.defaultDateTime] shouldBeEqualTo nonDefaultDate
        }
    }

    /**
     * `datetime` 컬럼을 조건절에 `between` 사용하기
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      datetime TIMESTAMP NOT NULL
     * );
     *
     * INSERT INTO tester (datetime) VALUES ('2019-01-01T01:01:00');
     * INSERT INTO tester (datetime) VALUES ('2020-01-01T01:01:00');
     * INSERT INTO tester (datetime) VALUES ('2021-01-01T01:01:00');
     *
     * SELECT COUNT(*)
     *   FROM tester
     *  WHERE tester.datetime BETWEEN '2019-12-25T01:01:00' AND '2020-01-08T01:01:00';
     *
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `between function`(testDB: TestDB) {
        val tester = object: IntIdTable("tester") {
            val dt = datetime("datetime")
        }

        withTables(testDB, tester) {
            val dt2020 = LocalDateTime.of(2020, 1, 1, 1, 1)
            tester.insert { it[dt] = LocalDateTime.of(2019, 1, 1, 1, 1) }
            tester.insert { it[dt] = dt2020 }
            tester.insert { it[dt] = LocalDateTime.of(2021, 1, 1, 1, 1) }

            val count = tester.selectAll()
                .where {
                    tester.dt.between(dt2020.minusWeeks(1), dt2020.plusWeeks(1))
                }
                .count()
            count shouldBeEqualTo 1L
        }
    }

    /**
     * 기본 표현식으로 `CurrentDate`, `CurrentDateTime`, `CurrentTimestamp` 사용하기
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "name" TEXT NOT NULL,
     *      "defaultDate" DATE DEFAULT CURRENT_DATE NOT NULL,
     *      "defaultDateTime" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     *      "defaultTimestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Consistent Scheme With Function As Default Expression`(testDB: TestDB) {
        val tester = object: IntIdTable("tester") {
            val name = text("name")
            val defaultDate = date("defaultDate").defaultExpression(CurrentDate)
            val defaultDateTime = datetime("defaultDateTime").defaultExpression(CurrentDateTime)
            val defaultTimestamp = timestamp("defaultTimestamp").defaultExpression(CurrentTimestamp)
        }
        withTables(testDB, tester) {
            val actual = MigrationUtils.statementsRequiredForDatabaseMigration(tester)
            actual.shouldBeEmpty()
        }
    }

    /**
     * Timestamp 에 Time Zone 을 포함한 컬럼을 사용할 때 (`TIMESTAMP WITH TIME ZONE DEFAULT`)
     *
     * ```sql
     * -- Postgres:
     * CREATE TABLE IF NOT EXISTS t (
     *      id SERIAL PRIMARY KEY,
     *      t1 TIMESTAMP WITH TIME ZONE DEFAULT '2024-07-18 13:19:44+00'::timestamp with time zone NOT NULL,
     *      t2 TIMESTAMP WITH TIME ZONE DEFAULT '2024-07-18 13:19:44+00'::timestamp with time zone NOT NULL,
     *      t3 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
     * );
     * -- MySQL V8:
     * CREATE TABLE IF NOT EXISTS t (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      t1 TIMESTAMP(6) DEFAULT '2024-07-18 13:19:44.000000' NOT NULL,
     *      t2 TIMESTAMP(6) DEFAULT '2024-07-18 13:19:44.000000' NOT NULL,
     *      t3 TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Timestamp with TimeZone Default`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB + TestDB.MYSQL_V5 }
        // UTC time zone
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(ZoneOffset.UTC))
        ZoneId.systemDefault().id shouldBeEqualTo "UTC"

        val nowWithTimeZone = OffsetDateTime.parse("2024-07-18T13:19:44.000+00:00")
        val timestampWithTimeZoneLiteral = timestampWithTimeZoneLiteral(nowWithTimeZone)

        val testTable = object: IntIdTable("t") {
            val t1 = timestampWithTimeZone("t1").default(nowWithTimeZone)
            val t2 = timestampWithTimeZone("t2").defaultExpression(timestampWithTimeZoneLiteral)
            val t3 = timestampWithTimeZone("t3").defaultExpression(CurrentTimestampWithTimeZone)
        }

        fun Expression<*>.itOrNull() = when {
            currentDialectTest.isAllowedAsColumnDefault(this) ->
                "DEFAULT ${currentDialectTest.dataTypeProvider.processForDefaultValue(this)} NOT NULL"
            else -> "NULL"
        }

        withTables(testDB, testTable) {
            val timestampWithTimeZoneType = currentDialectTest.dataTypeProvider.timestampWithTimeZoneType()

            val baseExpression = "CREATE TABLE " + addIfNotExistsIfSupported() +
                    "${"t".inProperCase()} (" +
                    "${"id".inProperCase()} ${currentDialectTest.dataTypeProvider.integerAutoincType()}${
                        " PRIMARY KEY"
                        // testDB.takeIf { it != TestDB.SQLITE }?.let { " PRIMARY KEY" } ?: ""
                    }, " +
                    "${"t1".inProperCase()} $timestampWithTimeZoneType${testTable.t1.constraintNamePart()} ${timestampWithTimeZoneLiteral.itOrNull()}, " +
                    "${"t2".inProperCase()} $timestampWithTimeZoneType${testTable.t2.constraintNamePart()} ${timestampWithTimeZoneLiteral.itOrNull()}, " +
                    "${"t3".inProperCase()} $timestampWithTimeZoneType${testTable.t3.constraintNamePart()} ${CurrentTimestampWithTimeZone.itOrNull()}" +
//                    when (testDB) {
//                        TestDB.SQLITE, TestDB.ORACLE ->
//                            ", CONSTRAINT chk_t_signed_integer_id CHECK (${"id".inProperCase()} BETWEEN ${Int.MIN_VALUE} AND ${Int.MAX_VALUE})"
//                        else -> ""
//                    } +
                    ")"

            val expected = if (currentDialectTest is OracleDialect ||
                currentDialectTest.h2Mode == Oracle
            ) {
                arrayListOf(
                    "CREATE SEQUENCE t_id_seq START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807",
                    baseExpression
                )
            } else {
                arrayListOf(baseExpression)
            }

            testTable.ddl shouldBeEqualTo expected

            val id1 = testTable.insertAndGetId { }

            val row1 = testTable.selectAll().where { testTable.id eq id1 }.single()
            row1[testTable.t1] shouldBeEqualTo nowWithTimeZone
            row1[testTable.t2] shouldBeEqualTo nowWithTimeZone
            val dbDefault = row1[testTable.t3]
            dbDefault.offset shouldBeEqualTo nowWithTimeZone.offset
            dbDefault.toLocalDateTime() shouldBeGreaterOrEqualTo nowWithTimeZone.toLocalDateTime()
        }
    }

    /**
     * [CurrentDateTime]을 기본값으로 사용
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS testdate (
     *      id SERIAL PRIMARY KEY,
     *      "time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Default CurrentDateTime`(testDB: TestDB) {

        val testDate = object: IntIdTable("TestDate") {
            val time = datetime("time").defaultExpression(CurrentDateTime)
        }

        fun LocalDateTime.millis(): Long = this.toEpochSecond(ZoneOffset.UTC) * 1000

        withTables(testDB, testDate) {
            val duration = 1000L

            repeat(2) {
                testDate.insertAndWait(duration)
            }

            Thread.sleep(duration)

            repeat(2) {
                testDate.insertAndWait(duration)
            }

            val sortedEntries: List<LocalDateTime> = testDate.selectAll().map { it[testDate.time] }.sorted()

            (sortedEntries[1].millis() - sortedEntries[0].millis()) shouldBeGreaterOrEqualTo 1000
            (sortedEntries[2].millis() - sortedEntries[0].millis()) shouldBeGreaterOrEqualTo 2000
            (sortedEntries[3].millis() - sortedEntries[0].millis()) shouldBeGreaterOrEqualTo 4000
        }
    }

    /**
     * `Date` 에 대한 기본 값 설정은 `ALTER TABLE` 문을 발생시키지 않아야 한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      "dateWithDefault" DATE DEFAULT '2024-02-01'::date NOT NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDateDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val date = LocalDate.of(2024, 2, 1)

        val tester = object: Table("tester") {
            val dateWithDefault = date("dateWithDefault").default(date)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    /**
     * Timestamp 컬럼에 Default 값을 설정할 때, ALTER TABLE 문이 발생하지 않아야 한다.
     *
     * ```sql
     * -- Postgres:
     * CREATE TABLE IF NOT EXISTS tester (
     *      "timestampWithDefault" TIMESTAMP DEFAULT '2023-05-04 05:04:00.7'::timestamp without time zone NOT NULL,
     *      "timestampWithDefaultExpression" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * );
     *
     * -- MYSQL V8:
     * CREATE TABLE IF NOT EXISTS tester (
     *      timestampWithDefault DATETIME(6) DEFAULT '2023-05-04 05:04:00.700000' NOT NULL,
     *      timestampWithDefaultExpression DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimestampDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val instant = Instant.parse("2023-05-04T05:04:00.700Z") // In UTC

        val tester = object: Table("tester") {
            val timestampWithDefault = timestamp("timestampWithDefault").default(instant)
            val timestampWithDefaultExpression =
                timestamp("timestampWithDefaultExpression").defaultExpression(CurrentTimestamp)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDatetimeDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      "datetimeWithDefault" TIMESTAMP DEFAULT '2023-05-04 05:04:07'::timestamp without time zone NOT NULL,
         *      "datetimeWithDefaultExpression" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
         * );
         * ```
         */
        val datetime = LocalDateTime.parse("2023-05-04T05:04:07.000")
        val tester = object: Table("tester") {
            val datetimeWithDefault = datetime("datetimeWithDefault").default(datetime)
            val datetimeWithDefaultExpression =
                datetime("datetimeWithDefaultExpression").defaultExpression(CurrentDateTime)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    /**
     * `Time` 컬럼에 대한 기본값 설정은 `ALTER TABLE` 문을 발생시키지 않아야 한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      "timeWithDefault" TIME DEFAULT '17:42:28.669743'::time without time zone NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimeDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val time = LocalDateTime.now(ZoneId.of("Asia/Seoul")).toLocalTime()

        val tester = object: Table("tester") {
            val timeWithDefault = time("timeWithDefault").default(time)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    /**
     * `Timestamp with Time Zone` 컬럼에 대한 기본값 설정은 `ALTER TABLE` 문을 발생시키지 않아야 한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      "timestampWithTimeZoneWithDefault" TIMESTAMP WITH TIME ZONE DEFAULT '2024-02-08 20:48:04.7+00'::timestamp with time zone NOT NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimestampWithTimeZoneDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val offsetDateTime = OffsetDateTime.parse("2024-02-08T20:48:04.700+09:00")

        val tester = object: Table("tester") {
            val timestampWithTimeZoneWithDefault =
                timestampWithTimeZone("timestampWithTimeZoneWithDefault").default(offsetDateTime)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        // MariaDB does not support TIMESTAMP WITH TIME ZONE column type
        val unsupportedDatabases = TestDB.ALL_MARIADB + listOf(TestDB.MYSQL_V5)
        Assumptions.assumeTrue { testDB !in unsupportedDatabases }
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      "timestamp" TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
     * );
     * ```
     */
    object DefaultTimestampTable: IntIdTable("test_table") {
        val timestamp: Column<OffsetDateTime> =
            timestampWithTimeZone("timestamp").defaultExpression(dbTimestampNow)
    }

    class DefaultTimestampEntity(id: EntityID<Int>): Entity<Int>(id) {
        companion object: EntityClass<Int, DefaultTimestampEntity>(DefaultTimestampTable)

        var timestamp: OffsetDateTime by DefaultTimestampTable.timestamp
    }

    /**
     * 엔티티 생성 시 사용한 사용자 정의 Timestamp 함수를 기본값으로 사용하는 경우에도 작동한다.
     *
     * ```sql
     * INSERT INTO test_table  DEFAULT VALUES;
     * ```
     *
     * @see [DefaultTimestampTable]
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testCustomDefaultTimestampFunctionWithEntity(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in (TestDB.ALL_POSTGRES + TestDB.MYSQL_V8 + TestDB.ALL_H2) }

        withTables(testDB, DefaultTimestampTable) {
            val entity = DefaultTimestampEntity.new {}
            val timestamp = DefaultTimestampTable.selectAll().first()[DefaultTimestampTable.timestamp]
            timestamp shouldBeEqualTo entity.timestamp
        }
    }

    /**
     * 사용자 정의 Timestamp 함수를 기본값으로 사용하는 경우에도 INSERT 문에서는 제외된다.
     *
     * TABLE 생성 시 ([DefaultTimestampTable]) 에 기본값이 설정되어 있기 때문에 INSERT 문에서는 값을 넣지 않아도 된다.
     *
     * ```sql
     * INSERT INTO test_table  DEFAULT VALUES;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testCustomDefaultTimestampFunctionWithInsertStatement(testDB: TestDB) {
        // Only Postgres allows to get timestamp values directly from the insert statement due to implicit 'returning *'
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }

        withTables(testDB, DefaultTimestampTable) {
            val entity = DefaultTimestampTable.insert { }
            val timestamp = DefaultTimestampTable.selectAll().first()[DefaultTimestampTable.timestamp]
            timestamp shouldBeEqualTo entity[DefaultTimestampTable.timestamp]
        }
    }
}
