package exposed.examples.kotlin.datetime

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.constraintNamePart
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.inProperCase
import exposed.shared.tests.insertAndWait
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.between
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.statements.BatchDataInconsistentException
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.h2Mode
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.datetime.CurrentDate
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.CurrentTimestampWithTimeZone
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.dateLiteral
import org.jetbrains.exposed.v1.datetime.dateTimeLiteral
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.datetime.duration
import org.jetbrains.exposed.v1.datetime.durationLiteral
import org.jetbrains.exposed.v1.datetime.time
import org.jetbrains.exposed.v1.datetime.timeLiteral
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.datetime.timestampLiteral
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZoneLiteral
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.DurationUnit.DAYS
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalTime::class)
class Ex02_Defaults: JdbcExposedTestBase() {

    companion object: KLogging()

    private fun localDateTimeNowMinusUnit(value: Int, unit: DurationUnit) =
        Clock.System.now().minus(value.toDuration(unit)).toLocalDateTime(TimeZone.currentSystemDefault())

    /**
     * ```sql
     * -- Postgre
     * CREATE TABLE IF NOT EXISTS tablewithdbdefault (
     *      id SERIAL PRIMARY KEY,
     *      field VARCHAR(100) NOT NULL,
     *      t1 TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
     *      t2 DATE DEFAULT CURRENT_DATE NOT NULL,
     *      "clientDefault" INT NOT NULL
     * )
     * ```
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS TableWithDBDefault (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      field VARCHAR(100) NOT NULL,
     *      t1 DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL,
     *      t2 DATE DEFAULT (CURRENT_DATE) NOT NULL,
     *      clientDefault INT NOT NULL
     * )
     * ```
     */
    object TableWithDBDefault: IntIdTable() {
        val cIndex = AtomicInteger(0)
        val field = varchar("field", 100)
        val t1 = datetime("t1").defaultExpression(CurrentDateTime)
        val t2 = date("t2").defaultExpression(CurrentDate)
        val clientDefault = integer("clientDefault").clientDefault { cIndex.getAndIncrement() }
    }

    class DBDefault(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<DBDefault>(TableWithDBDefault)

        var field by TableWithDBDefault.field
        var t1 by TableWithDBDefault.t1
        var t2 by TableWithDBDefault.t2
        val clientDefault by TableWithDBDefault.clientDefault

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("field", field)
                .add("t1", t1)
                .add("t2", t2)
                .add("clientDefault", clientDefault)
                .toString()
    }

    @Test
    fun testCanUseClientDefaultOnNullableColumn() {
        val defaultValue: Int? = null
        val table = object: IntIdTable() {
            val clientDefault = integer("clientDefault").nullable().clientDefault { defaultValue }
        }
        val returnedDefault = table.clientDefault.defaultValueFun?.invoke()

        table.clientDefault.columnType.nullable.shouldBeTrue()
        table.clientDefault.defaultValueFun.shouldNotBeNull()
        returnedDefault shouldBeEqualTo defaultValue
    }

    @Test
    fun testCanSetNullableColumnToUseClientDefault() {
        val defaultValue = 123
        val table = object: IntIdTable() {
            val clientDefault = integer("clientDefault").clientDefault { defaultValue }.nullable()
        }
        val returnedDefault = table.clientDefault.defaultValueFun?.invoke()

        table.clientDefault.columnType.nullable.shouldBeTrue()
        table.clientDefault.defaultValueFun.shouldNotBeNull()
        returnedDefault shouldBeEqualTo defaultValue
    }

    /**
     * 기본값 적용, 기본값이 정의된 컬럼에 직접 값 입력하는 예
     *
     * ```sql
     * INSERT INTO tablewithdbdefault (field, "clientDefault")
     * VALUES ('1', 6);
     *
     * INSERT INTO tablewithdbdefault (field, t1, "clientDefault")
     * VALUES ('2', '2025-01-30T14:45:36.7796', 7);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaultsWithExplicit01(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            val created = listOf(
                DBDefault.new { field = "1" },
                DBDefault.new {
                    field = "2"
                    t1 = localDateTimeNowMinusUnit(5, DAYS)
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
     * 기본값이 제공되는 컬럼에 값 제공하기
     *
     * ```sql
     * -- Postgres
     * INSERT INTO tablewithdbdefault (field, t1, "clientDefault")
     * VALUES ('2', '2025-01-30T14:45:36.85006', 16);
     *
     * INSERT INTO tablewithdbdefault (field, "clientDefault")
     * VALUES ('1', 17);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaultsWithExplicit02(testDB: TestDB) {
        // MySql 5 is excluded because it does not support `CURRENT_DATE()` as a default value
        Assumptions.assumeTrue { testDB != TestDB.MYSQL_V5 }
        withTables(testDB, TableWithDBDefault) {
            val created = listOf(
                DBDefault.new {
                    field = "2"
                    t1 = localDateTimeNowMinusUnit(5, DAYS)
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
     * Client Default 설정 초기화는 한번만 수행됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaultsInvokedOnlyOncePerEntity(testDB: TestDB) {
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

    private val initBatch = listOf<(BatchInsertStatement) -> Unit>(
        {
            it[TableWithDBDefault.field] = "1"
        },
        {
            it[TableWithDBDefault.field] = "2"
            it[TableWithDBDefault.t1] = now()
        }
    )

    /**
     * Raw 방식의 Batch Insert 는 실패합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testRawBatchInsertFails01(testDB: TestDB) {
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
     * Batch Insert 는 성공해야 합니다.
     *
     * ```sql
     * INSERT INTO tablewithdbdefault (field, "clientDefault")
     * VALUES ('1', 26);
     *
     * INSERT INTO tablewithdbdefault (field, t1, "clientDefault")
     * VALUES ('2', '2025-02-04T14:45:39.222217', 27);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testBatchInsertNotFails01(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            TableWithDBDefault.batchInsert(initBatch) { foo ->
                foo(this)
            }
        }
    }

    /**
     * 기본 값이 없는 NON NULL 컬럼에 값을 지정하지 않아서 실패합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testBatchInsertFails01(testDB: TestDB) {
        withTables(testDB, TableWithDBDefault) {
            expectException<BatchDataInconsistentException> {
                TableWithDBDefault.batchInsert(listOf(1)) {
                    this[TableWithDBDefault.t1] = now()
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaults01(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
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
         *      t7 BIGINT DEFAULT '1262304042000000000' NOT NULL, t8 BIGINT DEFAULT '1262304042000000000' NOT NULL,
         *      t9 TIME DEFAULT '12:00:00'::time without time zone NOT NULL, t10 TIME DEFAULT '12:00:00'::time without time zone NOT NULL
         * );
         * ```
         */
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
        val dateConstValue = LocalDate(2010, 1, 1)
        val instConstValue = dateConstValue.atStartOfDayIn(TimeZone.UTC)
        val dateTimeConstValue = instConstValue.toLocalDateTime(TimeZone.UTC)
        val dLiteral = dateLiteral(dateConstValue)
        val dtLiteral = dateTimeLiteral(dateTimeConstValue)
        val tsConstValue = instConstValue.plus(42.toDuration(DurationUnit.SECONDS))
        val tsLiteral = timestampLiteral(tsConstValue)
        val durConstValue = tsConstValue.toEpochMilliseconds().toDuration((DurationUnit.MILLISECONDS))
        val durLiteral = durationLiteral(durConstValue)
        val tmConstValue = LocalTime(12, 0)
        val tLiteral = timeLiteral(tmConstValue)

        val tester = object: IntIdTable("t") {
            val s = varchar("s", 100).default("test")
            val sn = varchar("sn", 100).default("testNullable").nullable()
            val l = long("l").default(42)
            val c = char("c").default('X')
            val t1 = datetime("t1").defaultExpression(currentDT)
            val t2 = datetime("t2").defaultExpression(nowExpression)
            val t3 = datetime("t3").defaultExpression(dtLiteral)
            val t4 = date("t4").default(dateConstValue)
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

        withTables(testDB, tester) {
            val dtType = currentDialectTest.dataTypeProvider.dateTimeType()
            val dType = currentDialectTest.dataTypeProvider.dateType()
            val timestampType = currentDialectTest.dataTypeProvider.timestampType()
            val longType = currentDialectTest.dataTypeProvider.longType()
            val timeType = currentDialectTest.dataTypeProvider.timeType()
            val varCharType = currentDialectTest.dataTypeProvider.varcharType(100)
            val q = db.identifierManager.quoteString
            val baseExpression = "CREATE TABLE " + addIfNotExistsIfSupported() +
                    "${"t".inProperCase()} (" +
                    "${"id".inProperCase()} ${currentDialectTest.dataTypeProvider.integerAutoincType()}${
                        " PRIMARY KEY"
                        // testDB.takeIf { it != TestDB.SQLITE }?.let { " PRIMARY KEY" } ?: ""
                    }, " +
                    "${"s".inProperCase()} $varCharType${tester.s.constraintNamePart()} DEFAULT 'test' NOT NULL, " +
                    "${"sn".inProperCase()} $varCharType${tester.sn.constraintNamePart()} DEFAULT 'testNullable' NULL, " +
                    "${"l".inProperCase()} ${currentDialectTest.dataTypeProvider.longType()}${tester.l.constraintNamePart()} DEFAULT 42 NOT NULL, " +
                    "$q${"c".inProperCase()}$q CHAR${tester.c.constraintNamePart()} DEFAULT 'X' NOT NULL, " +
                    "${"t1".inProperCase()} $dtType${tester.t1.constraintNamePart()} ${currentDT.itOrNull()}, " +
                    "${"t2".inProperCase()} $dtType${tester.t2.constraintNamePart()} ${nowExpression.itOrNull()}, " +
                    "${"t3".inProperCase()} $dtType${tester.t3.constraintNamePart()} ${dtLiteral.itOrNull()}, " +
                    "${"t4".inProperCase()} $dType${tester.t4.constraintNamePart()} ${dLiteral.itOrNull()}, " +
                    "${"t5".inProperCase()} $timestampType${tester.t5.constraintNamePart()} ${tsLiteral.itOrNull()}, " +
                    "${"t6".inProperCase()} $timestampType${tester.t6.constraintNamePart()} ${tsLiteral.itOrNull()}, " +
                    "${"t7".inProperCase()} $longType${tester.t7.constraintNamePart()} ${durLiteral.itOrNull()}, " +
                    "${"t8".inProperCase()} $longType${tester.t8.constraintNamePart()} ${durLiteral.itOrNull()}, " +
                    "${"t9".inProperCase()} $timeType${tester.t9.constraintNamePart()} ${tLiteral.itOrNull()}, " +
                    "${"t10".inProperCase()} $timeType${tester.t10.constraintNamePart()} ${tLiteral.itOrNull()}" +
//                    when (testDB) {
//                        TestDB.SQLITE, TestDB.ORACLE ->
//                            ", CONSTRAINT chk_t_signed_integer_id CHECK (${"id".inProperCase()} BETWEEN ${Int.MIN_VALUE} AND ${Int.MAX_VALUE})"
//                        else -> ""
//                    } +
                    ")"

            val expected =
                if (currentDialectTest is OracleDialect || currentDialectTest.h2Mode == H2Dialect.H2CompatibilityMode.Oracle) {
                    arrayListOf(
                        "CREATE SEQUENCE t_id_seq START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807",
                        baseExpression
                    )
                } else {
                    arrayListOf(baseExpression)
                }

            tester.ddl shouldBeEqualTo expected

            val id1 = tester.insertAndGetId { }

            val row1 = tester.selectAll().where { tester.id eq id1 }.single()
            row1[tester.s] shouldBeEqualTo "test"
            row1[tester.sn] shouldBeEqualTo "testNullable"
            row1[tester.l] shouldBeEqualTo 42
            row1[tester.c] shouldBeEqualTo 'X'
            row1[tester.t3] shouldBeEqualTo dateTimeConstValue
            row1[tester.t4] shouldBeEqualTo dateConstValue
            row1[tester.t5] shouldBeEqualTo tsConstValue
            row1[tester.t6] shouldBeEqualTo tsConstValue
            row1[tester.t7] shouldBeEqualTo durConstValue
            row1[tester.t8] shouldBeEqualTo durConstValue
            row1[tester.t9] shouldBeEqualTo tmConstValue
            row1[tester.t10] shouldBeEqualTo tmConstValue
        }
    }

    /**
     * 컬럼 기본값 표현식에 따라 기본값이 저장되어야 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaultExpressions01(testDB: TestDB) {
        fun abs(value: Int) = object: ExpressionWithColumnType<Int>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder { append("ABS($value)") }

            override val columnType: IColumnType<Int> = IntegerColumnType()
        }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS foo (
         *      id SERIAL PRIMARY KEY,
         *      "name" TEXT NOT NULL,
         *      "defaultDateTime" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
         *      "defaultDate" DATE DEFAULT CURRENT_DATE NOT NULL,
         *      "defaultInteger" INT DEFAULT (ABS(-100)) NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("foo") {
            val name = text("name")
            val defaultDateTime = datetime("defaultDateTime").defaultExpression(CurrentDateTime)
            val defaultDate = date("defaultDate").defaultExpression(CurrentDate)
            val defaultInt = integer("defaultInteger").defaultExpression(abs(-100))
        }

        // MySql 5 is excluded because it does not support `CURRENT_DATE()` as a default value
        Assumptions.assumeTrue { testDB !in setOf(TestDB.MYSQL_V5) }

        withTables(testDB, tester) {
            val id = tester.insertAndGetId {
                it[tester.name] = "bar"
            }
            val result = tester.selectAll().where { tester.id eq id }.single()

            result[tester.defaultDateTime].date shouldBeEqualTo today
            result[tester.defaultDate] shouldBeEqualTo today
            result[tester.defaultInt] shouldBeEqualTo 100
        }
    }

    /**
     * 컬럼 기본값이 정의된 컬럼에 새로운 값을 지정할 수 있습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaultExpressions02(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      "name" TEXT NOT NULL,
         *      "defaultDateTime" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val name = text("name")
            val defaultDateTime = datetime("defaultDateTime").defaultExpression(CurrentDateTime)
        }

        val nonDefaultDate = LocalDate(2000, 1, 1)
            .atStartOfDayIn(TimeZone.currentSystemDefault())
            .toLocalDateTime(TimeZone.currentSystemDefault())

        withTables(testDB, tester) {
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

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testBetweenFunction(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      "dateTime" TIMESTAMP NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val dt = datetime("dateTime")
        }

        withTables(testDB, tester) {
            val d2020 = LocalDate(2020, 1, 1)
            val dt2020 = d2020.atTime(0, 0, 0)
            val dt2020m1w = d2020.minus(1, DateTimeUnit.WEEK).atTime(0, 0, 0)
            val dt2020p1w = d2020.plus(1, DateTimeUnit.WEEK).atTime(0, 0, 0)

            tester.insert { it[dt] = LocalDateTime(2019, 1, 1, 1, 1) }
            tester.insert { it[dt] = dt2020 }
            tester.insert { it[dt] = LocalDateTime(2021, 1, 1, 1, 1) }

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."dateTime" BETWEEN '2019-12-25T00:00:00' AND '2020-01-08T00:00:00'
             * ```
             * ```sql
             * -- MySQL V8
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester.dateTime BETWEEN '2019-12-25 00:00:00.000000' AND '2020-01-08 00:00:00.000000'
             * ```
             */
            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester."dateTime" BETWEEN '2019-12-25T00:00:00' AND '2020-01-08T00:00:00'
             * ```
             * ```sql
             * -- MySQL V8
             * SELECT COUNT(*)
             *   FROM tester
             *  WHERE tester.dateTime BETWEEN '2019-12-25 00:00:00.000000' AND '2020-01-08 00:00:00.000000'
             * ```
             */
            val count = tester
                .selectAll()
                .where { tester.dt.between(dt2020m1w, dt2020p1w) }
                .count()
            count shouldBeEqualTo 1L
        }
    }

    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testConsistentSchemeWithFunctionAsDefaultExpression(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      "name" TEXT NOT NULL,
         *      default_date DATE DEFAULT CURRENT_DATE NOT NULL,
         *      default_date_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
         *      default_time_stamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val name = text("name")
            val defaultDate = date("default_date").defaultExpression(CurrentDate)
            val defaultDateTime = datetime("default_date_time").defaultExpression(CurrentDateTime)
            val defaultTimeStamp = timestamp("default_time_stamp").defaultExpression(CurrentTimestamp)
        }

        withTables(testDB, tester) {
            SchemaUtils.statementsRequiredToActualizeScheme(tester).shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimestampWithTimeZoneDefaults(testDB: TestDB) {
        // UTC time zone
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(ZoneOffset.UTC))
        ZoneId.systemDefault().id shouldBeEqualTo "UTC"

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS t (
         *      id SERIAL PRIMARY KEY,
         *      t1 TIMESTAMP WITH TIME ZONE DEFAULT '2024-07-18 13:19:44.1+00'::timestamp with time zone NOT NULL,
         *      t2 TIMESTAMP WITH TIME ZONE DEFAULT '2024-07-18 13:19:44.1+00'::timestamp with time zone NOT NULL,
         *      t3 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
         * )
         * ```
         * ```sql
         * -- MySQL V8
         * CREATE TABLE IF NOT EXISTS t (
         *      id INT AUTO_INCREMENT PRIMARY KEY,
         *      t1 TIMESTAMP(6) DEFAULT '2024-07-18 13:19:44.100000' NOT NULL,
         *      t2 TIMESTAMP(6) DEFAULT '2024-07-18 13:19:44.100000' NOT NULL,
         *      t3 TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL
         * )
         * ```
         */
        val nowWithTimeZone = OffsetDateTime.parse("2024-07-18T13:19:44.100+00:00")
        val timestampWithTimeZoneLiteral = timestampWithTimeZoneLiteral(nowWithTimeZone)
        val tester = object: IntIdTable("t") {
            val t1 = timestampWithTimeZone("t1").default(nowWithTimeZone)
            val t2 = timestampWithTimeZone("t2").defaultExpression(timestampWithTimeZoneLiteral)
            val t3 = timestampWithTimeZone("t3").defaultExpression(CurrentTimestampWithTimeZone)
        }

        fun Expression<*>.itOrNull() = when {
            currentDialectTest.isAllowedAsColumnDefault(this) ->
                "DEFAULT ${currentDialectTest.dataTypeProvider.processForDefaultValue(this)} NOT NULL"
            else -> "NULL"
        }

        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB + TestDB.MYSQL_V5 }
        withTables(testDB, tester) {
            val timestampWithTimeZoneType = currentDialectTest.dataTypeProvider.timestampWithTimeZoneType()

            val baseExpression = "CREATE TABLE " + addIfNotExistsIfSupported() +
                    "${"t".inProperCase()} (" +
                    "${"id".inProperCase()} ${currentDialectTest.dataTypeProvider.integerAutoincType()}${
                        " PRIMARY KEY"
                        // testDB.takeIf { it != TestDB.SQLITE }?.let { " PRIMARY KEY" } ?: ""
                    }, " +
                    "${"t1".inProperCase()} $timestampWithTimeZoneType${tester.t1.constraintNamePart()} ${timestampWithTimeZoneLiteral.itOrNull()}, " +
                    "${"t2".inProperCase()} $timestampWithTimeZoneType${tester.t2.constraintNamePart()} ${timestampWithTimeZoneLiteral.itOrNull()}, " +
                    "${"t3".inProperCase()} $timestampWithTimeZoneType${tester.t3.constraintNamePart()} ${CurrentTimestampWithTimeZone.itOrNull()}" +
//                    when (testDB) {
//                        TestDB.SQLITE, TestDB.ORACLE ->
//                            ", CONSTRAINT chk_t_signed_integer_id CHECK (${"id".inProperCase()} BETWEEN ${Int.MIN_VALUE} AND ${Int.MAX_VALUE})"
//                        else -> ""
//                    } +
                    ")"

            val expected = if (currentDialectTest is OracleDialect ||
                currentDialectTest.h2Mode == H2Dialect.H2CompatibilityMode.Oracle
            ) {
                arrayListOf(
                    "CREATE SEQUENCE t_id_seq START WITH 1 MINVALUE 1 MAXVALUE 9223372036854775807",
                    baseExpression
                )
            } else {
                arrayListOf(baseExpression)
            }
            tester.ddl shouldBeEqualTo expected

            val id1 = tester.insertAndGetId { }

            val row1 = tester.selectAll().where { tester.id eq id1 }.single()
            row1[tester.t1] shouldBeEqualTo nowWithTimeZone
            row1[tester.t2] shouldBeEqualTo nowWithTimeZone

            val dbDefault = row1[tester.t3]
            dbDefault.offset shouldBeEqualTo nowWithTimeZone.offset
            dbDefault.toLocalDateTime().toKotlinLocalDateTime() shouldBeGreaterOrEqualTo
                    nowWithTimeZone.toLocalDateTime().toKotlinLocalDateTime()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDefaultCurrentDateTime(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS testdate (
         *      id SERIAL PRIMARY KEY,
         *      "time" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("TestDate") {
            val time = datetime("time").defaultExpression(CurrentDateTime)
        }

        fun LocalDateTime.millis(): Long = this.toJavaLocalDateTime().toEpochSecond(ZoneOffset.UTC) * 1000

        withTables(testDB, tester) {
            val duration: Long = 1000

            repeat(2) {
                tester.insertAndWait(duration)
            }

            Thread.sleep(duration)

            repeat(2) {
                tester.insertAndWait(duration)
            }

            val sortedEntries: List<LocalDateTime> = tester.selectAll().map { it[tester.time] }.sorted()

            sortedEntries[1].millis() - sortedEntries[0].millis() shouldBeGreaterOrEqualTo 1000
            sortedEntries[2].millis() - sortedEntries[0].millis() shouldBeGreaterOrEqualTo 3000
            sortedEntries[3].millis() - sortedEntries[0].millis() shouldBeGreaterOrEqualTo 4000
        }
    }

    /**
     * 기본값 설정 정의 시에는 ALTER TABLE 문이 발생하지 않아야 합니다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      "datetimeWithDefault" TIMESTAMP DEFAULT '2023-05-04 05:04:07'::timestamp without time zone NOT NULL,
     *      "datetimeWithDefaultExpression" TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDatetimeDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val datetime = LocalDateTime.parse("2023-05-04T05:04:07.000")

        val tester = object: Table("tester") {
            val datetimeWithDefault = datetime("datetimeWithDefault").default(datetime)
            val datetimeWithDefaultExpression = datetime("datetimeWithDefaultExpression")
                .defaultExpression(CurrentDateTime)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testDateDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val date = LocalDate(2024, 2, 1)

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      "dateWithDefault" DATE DEFAULT '2024-02-01'::date NOT NULL
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val dateWithDefault = date("dateWithDefault").default(date)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimeDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      "timeWithDefault" TIME DEFAULT '00:30:49.341736'::time without time zone NOT NULL
         * );
         * ```
         */
        val time = Clock.System.now().toLocalDateTime(TimeZone.of("Asia/Seoul")).time
        val tester = object: Table("tester") {
            val timeWithDefault = time("timeWithDefault").default(time)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimestampDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        val instant = Instant.parse("2023-05-04T05:04:00.700Z") // In UTC

        val tester = object: Table("tester") {
            val timestampWithDefault = timestamp("timestampWithDefault").default(instant)
            val timestampWithDefaultExpression = timestamp("timestampWithDefaultExpression")
                .defaultExpression(CurrentTimestamp)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testTimestampWithTimeZoneDefaultDoesNotTriggerAlterStatement(testDB: TestDB) {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *  "timestampWithTimeZoneWithDefault" TIMESTAMP WITH TIME ZONE DEFAULT '2023-05-04 05:04:01.7+00'::timestamp with time zone NOT NULL
         * )
         * ```
         * ```sql
         * -- MySQL V8
         * CREATE TABLE IF NOT EXISTS tester (
         *  timestampWithTimeZoneWithDefault TIMESTAMP(6) DEFAULT '2023-05-03 20:04:01.700000' NOT NULL
         * )
         * ```
         */
        val offsetDateTime = OffsetDateTime.parse("2023-05-04T05:04:01.700+09:00")
        val tester = object: Table("tester") {
            val timestampWithTimeZoneWithDefault =
                timestampWithTimeZone("timestampWithTimeZoneWithDefault").default(offsetDateTime)
        }

        // SQLite does not support ALTER TABLE on a column that has a default value
        // MariaDB does not support TIMESTAMP WITH TIME ZONE column type
        val unsupportedDatabases = TestDB.ALL_MARIADB + TestDB.MYSQL_V5
        Assumptions.assumeTrue { testDB !in unsupportedDatabases }
        withTables(testDB, tester) {
            val statements = SchemaUtils.addMissingColumnsStatements(tester)
            statements.shouldBeEmpty()
        }
    }

    /**
     * Update 시에 CurrentTimestamp 이 적용되는 예 (MySQL 계열만 가능)
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      amount INT NOT NULL,
     *      created DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testColumnOnUpdateCurrentTimestamp(testDB: TestDB) {
        val tester = object: Table("tester") {
            val amount = integer("amount")
            val created =
                timestamp("created")
                    .defaultExpression(CurrentTimestamp)
                    .withDefinition("ON UPDATE", CurrentTimestamp)
        }

        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_LIKE }

        withTables(testDB, tester) {
            MigrationUtils.statementsRequiredForDatabaseMigration(tester).shouldBeEmpty()

            tester.insert {
                it[amount] = 999
            }
            val generatedTS = tester.select(tester.created).single()[tester.created]

            Thread.sleep(1000)

            // Update the row -> 이 때 `created` 컬럼의 값이 [CurrentDateTime] 값으로 변경되어야 함
            tester.update {
                it[amount] = 111
            }

            val updatedResult = tester.selectAll()
                .where { tester.created greater generatedTS }
                .single()
            updatedResult[tester.created] shouldBeGreaterThan generatedTS
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      "timestamp" TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
     * );
     * ```
     *
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `timestamp` TIMESTAMP(6) DEFAULT (now()) NOT NULL
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

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("timestamp", timestamp)
                .toString()
    }

    /**
     * 컬럼의 기본값 정의를 custom expression 함수를 사용해도, Entity 생성 시에는 기본값이 적용됩니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun testCustomDefaultTimestampFunctionWithEntity(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES + TestDB.MYSQL_V8 + TestDB.ALL_H2 }

        withTables(testDB, DefaultTimestampTable) {
            val entity = DefaultTimestampEntity.new {}
            val timestamp = DefaultTimestampTable.selectAll().first()[DefaultTimestampTable.timestamp]
            timestamp shouldBeEqualTo entity.timestamp
        }
    }

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
