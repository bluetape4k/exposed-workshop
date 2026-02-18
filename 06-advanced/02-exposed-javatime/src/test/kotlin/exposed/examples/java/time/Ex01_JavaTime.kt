package exposed.examples.java.time

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.currentDialectTest
import exposed.shared.tests.expectException
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.debug
import io.mockk.impl.InternalPlatform.time
import kotlinx.serialization.json.Json
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Cast
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.castTo
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.get
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.min
import org.jetbrains.exposed.v1.core.slice
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.OracleDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.SQLServerDialect
import org.jetbrains.exposed.v1.core.vendors.SQLiteDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.javatime.*
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.extract
import org.jetbrains.exposed.v1.json.jsonb
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.MILLIS
import java.time.temporal.Temporal
import java.util.*

/**
 * Exposed에서 제공하는 Java Time 수형을 사용하는 테스트
 */
class Ex01_JavaTime: JdbcExposedTestBase() {

    companion object: KLogging()

    private val timestampWithTimeZoneUnsupportedDB = TestDB.ALL_MARIADB + TestDB.MYSQL_V5

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `javaTime functions`(testDB: TestDB) {
        // FIXME: MySQL_V8 에서는 LocalDate 에 대해 Year 함수가 제대로 동작하지 않는다 --> Driver 문제인 듯 
        // 에러 메시지: Unexpected value of type Int: 2025-01-01 of java.sql.Date
        // Assumptions.assumeTrue { testDB != TestDB.MYSQL_V8 }

        withTables(testDB, CitiesTime) {
            val now = LocalDateTime.now()

            /**
             * Insert a city with local time
             * ```sql
             * -- Postgres
             * INSERT INTO citiestime ("name", local_time)
             * VALUES ('Seoul', '2025-02-04T10:40:53.969116')
             * ```
             */
            val cityID = CitiesTime.insertAndGetId {
                it[name] = "Seoul"
                it[local_time] = now
            }

            /**
             * Select the city with local time
             *
             * ```sql
             * -- Postgres
             * SELECT Extract(YEAR FROM citiestime.local_time),
             *        Extract(MONTH FROM citiestime.local_time),
             *        Extract(DAY FROM citiestime.local_time),
             *        Extract(HOUR FROM citiestime.local_time),
             *        Extract(MINUTE FROM citiestime.local_time),
             *        Extract(SECOND FROM citiestime.local_time)
             *   FROM citiestime
             *  WHERE citiestime.id = 1
             * ```
             * ```sql
             * -- MySQL V5
             * SELECT YEAR(CitiesTime.local_time),
             *        MONTH(CitiesTime.local_time),
             *        DAY(CitiesTime.local_time),
             *        HOUR(CitiesTime.local_time),
             *        MINUTE(CitiesTime.local_time),
             *        SECOND(CitiesTime.local_time)
             *   FROM CitiesTime
             *  WHERE CitiesTime.id = 1
             * ```
             */
            val row = CitiesTime
                .select(
                    CitiesTime.local_time.year(),
                    CitiesTime.local_time.month(),
                    CitiesTime.local_time.day(),
                    CitiesTime.local_time.hour(),
                    CitiesTime.local_time.minute(),
                    CitiesTime.local_time.second(),
                )
                .where { CitiesTime.id eq cityID }
                .single()

            // FIXME: MySQL_V8 에서는 LocalDate 에 대해 Year 함수가 제대로 동작하지 않는다 --> Driver 문제인 듯
            // 에러 메시지: Unexpected value of type Int: 2025-01-01 of java.sql.Date
            // Assumptions.assumeTrue { testDB != TestDB.MYSQL_V8 }

            if (testDB != TestDB.MYSQL_V8) {
                row[CitiesTime.local_time.year()] shouldBeEqualTo now.year
            }
            row[CitiesTime.local_time.month()] shouldBeEqualTo now.month.value
            row[CitiesTime.local_time.day()] shouldBeEqualTo now.dayOfMonth
            row[CitiesTime.local_time.hour()] shouldBeEqualTo now.hour
            row[CitiesTime.local_time.minute()] shouldBeEqualTo now.minute
            row[CitiesTime.local_time.second()] shouldBeEqualTo now.second
        }
    }

    /**
     * Instant 를 DB에 저장화고 조회한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS ts_table (
     *      ts TIMESTAMP NOT NULL,
     *      tsn TIMESTAMP NULL
     * );
     * ```
     * ```sql
     * INSERT INTO ts_table (ts, tsn)
     * VALUES ('2025-02-04T10:29:49.01299', '2025-02-04T10:29:49.01299');
     * ```
     *
     * ```sql
     *  SELECT MAX(ts_table.ts) FROM ts_table;
     *  SELECT MIN(ts_table.ts) FROM ts_table;
     *  SELECT MAX(ts_table.tsn) FROM ts_table;
     *  SELECT MIN(ts_table.tsn) FROM ts_table;
     *  ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `selecting instant using expression`(testDB: TestDB) {
        val testTable = object: Table("ts_table") {
            val ts = timestamp("ts")
            val tsn = timestamp("tsn").nullable()
        }

        val now = Instant.now()

        withTables(testDB, testTable) {
            testTable.insert {
                it[ts] = now
                it[tsn] = now
            }

            val maxTsExpr = testTable.ts.max()
            val maxTimestamp = testTable.select(maxTsExpr).single()[maxTsExpr]
            maxTimestamp shouldBeEqualTo now

            val minTsExpr = testTable.ts.min()
            val minTimestamp = testTable.select(minTsExpr).single()[minTsExpr]
            minTimestamp shouldBeEqualTo now

            val maxTsnExpr = testTable.tsn.max()
            val maxTimestampNullable = testTable.select(maxTsnExpr).single()[maxTsnExpr]
            maxTimestampNullable shouldBeEqualTo now

            val minTsnExpr = testTable.tsn.min()
            val minTimestampNullable = testTable.select(minTsnExpr).single()[minTsnExpr]
            minTimestampNullable shouldBeEqualTo now
        }
    }

    /**
     * `LocalDateTim`을 `date` 컬럼 함수로 nanos 값까지 DB에 저장하기
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS testlocaldatetime (
     *      id SERIAL PRIMARY KEY,
     *      "time" TIMESTAMP NOT NULL
     * )
     * ```
     *
     * Insert two `LocalDateTime` with nanos
     * ```sql
     * -- Postgres
     * INSERT INTO testlocaldatetime ("time") VALUES ('2025-02-04T09:13:33.000111112')
     * INSERT INTO testlocaldatetime ("time") VALUES ('2025-02-04T09:13:33.000111118')
     * ```
     *
     * Load the `LocalDateTime` with nanos from the DB
     * ```
     * -- Postgres, MySQL, H2_PSQL
     * dateTimesFromDB=[2025-02-04T09:24:16.000111, 2025-02-04T09:24:16.000111]
     * -- H2, H2_MYSQL, H2_MARIADB
     * dateTimesFromDB=[2025-02-04T09:24:16.000111112, 2025-02-04T09:24:16.000111118]
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Storing LocalDateTime with nanos`(testDB: TestDB) {
        val testDate = object: IntIdTable("TestLocalDateTime") {
            val time: Column<LocalDateTime> = datetime("time")
        }

        withTables(testDB, testDate) {
            val dateTime = LocalDateTime.now()
            val nanos = 111111

            val dateTimeWithNanos1 = dateTime.withNano(nanos + 1)
            val dateTimeWithNanos2 = dateTime.withNano(nanos + 7)

            testDate.insert {
                it[time] = dateTimeWithNanos1
            }

            testDate.insert {
                it[time] = dateTimeWithNanos2
            }

            flushCache()

            val dateTimesFromDB = testDate.selectAll().map { it[testDate.time] }.apply {
                log.debug { "dateTimesFromDB=$this" }
            }
            dateTimesFromDB[0] shouldTemporalEqualTo dateTimeWithNanos1
            dateTimesFromDB[1] shouldTemporalEqualTo dateTimeWithNanos2
        }
    }

    /**
     * [LocalDate] 저장, 로드, 비교를 수행합니다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS test_table (
     *      created DATE NOT NULL,
     *      deleted DATE NOT NULL
     * )
     * ```
     * Insert [LocalDate] values
     * ```sql
     * INSERT INTO test_table (created, deleted) VALUES ('2024-05-04', '2024-05-04');
     * INSERT INTO test_table (created, deleted) VALUES ('2024-05-04', '2024-05-05');
     * ```
     * Select [LocalDate] values
     * ```sql
     * -- Same date
     * SELECT test_table.created, test_table.deleted
     *   FROM test_table
     *  WHERE test_table.created = test_table.deleted;
     *
     * -- Same month
     * SELECT test_table.created, test_table.deleted
     *   FROM test_table
     *  WHERE Extract(MONTH FROM test_table.created) = Extract(MONTH FROM test_table.deleted);
     *
     *  -- Same year
     * SELECT test_table.created, test_table.deleted
     *   FROM test_table
     *  WHERE Extract(YEAR FROM test_table.created) = Extract(YEAR FROM CAST('2024-05-04' AS DATE))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `LocalDate comparison`(testDB: TestDB) {
        val testTable = object: Table("test_table") {
            val created = date("created")
            val deleted = date("deleted")
        }

        withTables(testDB, testTable) {
            val mayTheFourth = LocalDate.of(2024, 5, 4)
            testTable.insert {
                it[created] = mayTheFourth
                it[deleted] = mayTheFourth
            }
            testTable.insert {
                it[created] = mayTheFourth
                it[deleted] = mayTheFourth.plusDays(1L)
            }

            val sameDateResult = testTable
                .selectAll()
                .where { testTable.created eq testTable.deleted }
                .toList()
            sameDateResult shouldHaveSize 1
            sameDateResult.single()[testTable.deleted] shouldBeEqualTo mayTheFourth

            // Same Month
            val sameMonthResult = testTable
                .selectAll()
                .where { testTable.created.month() eq testTable.deleted.month() }
                .toList()
            sameMonthResult shouldHaveSize 2

            // Same Year
            val year2024 = if (currentDialect is PostgreSQLDialect) {
                // PostgreSQL requires explicit type cast to resolve function date_part
                dateParam(mayTheFourth).castTo(JavaLocalDateColumnType()).year()
            } else {
                dateParam(mayTheFourth).year()
            }

            val createdIn2025 = testTable
                .selectAll()
                .where { testTable.created.year() eq year2024 }
                .toList()
            createdIn2025 shouldHaveSize 2
        }
    }

    /**
     * [LocalDateTime]을 저장하고, 비교한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS test_table_dt (
     *      id SERIAL PRIMARY KEY,
     *      created TIMESTAMP NOT NULL,
     *      modified TIMESTAMP NOT NULL
     * );
     * ```
     * ```sql
     * INSERT INTO test_table_dt (created, modified)
     * VALUES ('2024-05-04T13:00:21.871130789', '2024-05-04T13:00:21.871130789');
     *
     * INSERT INTO test_table_dt (created, modified)
     * VALUES ('2024-05-04T13:00:21.871130789', '2025-02-04T10:40:53.92072');
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `LocalDateTime comparison`(testDB: TestDB) {
        val tester = object: IntIdTable("test_table_dt") {
            val created = datetime("created")
            val modified = datetime("modified")
        }

        withTables(testDB, tester) {
            val mayTheFourth = LocalDateTime.of(
                2024, 5, 4,
                13, 0, 21,
                871130789
            )
            val now = LocalDateTime.now()

            val id1 = tester.insertAndGetId {
                it[created] = mayTheFourth
                it[modified] = mayTheFourth
            }
            val id2 = tester.insertAndGetId {
                it[created] = mayTheFourth
                it[modified] = now
            }

            // these DB take the nanosecond value 871_130_789 and round up to default precision (e.g. in Oracle: 871_131)
            val requiresExplicitDTCast = setOf(TestDB.H2_PSQL)
            val dateTime = when (testDB) {
                in requiresExplicitDTCast -> Cast(dateTimeParam(mayTheFourth), JavaLocalDateTimeColumnType())
                else -> dateTimeParam(mayTheFourth)
            }

            /**
             * ```sql
             * SELECT COUNT(*) FROM test_table_dt WHERE test_table_dt.created = '2024-05-04T13:00:21.871130789'
             * ```
             */
            tester
                .selectAll()
                .where { tester.created eq dateTime }
                .count() shouldBeEqualTo 2L

            tester
                .selectAll()
                .where { tester.modified eq tester.created }
                .single()[tester.id] shouldBeEqualTo id1

            tester.selectAll()
                .where { tester.modified greater tester.created }
                .single()[tester.id] shouldBeEqualTo id2
        }
    }

    /**
     * LocalDateTime을 가진 [ModifierData] 클래스를 [jsonb] 형식으로 저장, 조회하기
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      created TIMESTAMP NOT NULL,
     *      modified JSONB NOT NULL
     * )
     * ```
     * ```sql
     * INSERT INTO tester (created, modified)
     * VALUES ('2024-02-04T09:44:53.239415', {"userId":1,"timestamp":"2025-02-04T09:44:53.239415"});
     * INSERT INTO tester (created, modified)
     * VALUES ('2026-02-04T09:44:53.239415', {"userId":2,"timestamp":"2025-02-04T09:44:53.239415"});
     * ```
     *
     * ```sql
     * SELECT JSONB_EXTRACT_PATH_TEXT(tester.modified, 'timestamp') FROM tester;
     *
     * SELECT tester.id, tester.created, tester.modified
     *   FROM tester
     *  WHERE CAST(JSONB_EXTRACT_PATH_TEXT(tester.modified, 'timestamp') AS TIMESTAMP) < tester.created;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DateTime as JsonB`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 }

        val tester = object: IntIdTable("tester") {
            val created = datetime("created")
            val modified = jsonb<ModifierData>("modified", Json.Default)
        }

        withTables(testDB, tester) {
            val dateTimeNow = LocalDateTime.now()

            val id1 = tester.insert {
                it[created] = dateTimeNow.minusYears(1)
                it[modified] = ModifierData(1, dateTimeNow)
            }
            id1.shouldNotBeNull()

            val id2 = tester.insert {
                it[created] = dateTimeNow.plusYears(1)
                it[modified] = ModifierData(2, dateTimeNow)
            }
            id2.shouldNotBeNull()


            val prefix = if (currentDialectTest is PostgreSQLDialect) "" else "."

            // value extracted in same manner it is stored, a json string
            val modifiedAsString = tester.modified.extract<String>("${prefix}timestamp")
            val allModifiedAsString = tester.select(modifiedAsString)
            allModifiedAsString.all { it[modifiedAsString] == dateTimeNow.toString() }.shouldBeTrue()

            // PostgreSQL requires explicit type cast to timestamp for in-DB comparison
            val dateModified = when (currentDialectTest) {
                is PostgreSQLDialect -> tester.modified.extract<String>("${prefix}timestamp")
                    .castTo(JavaLocalDateTimeColumnType())

                else -> tester.modified.extract<String>("${prefix}timestamp")
            }
            val modifiedBeforeCreation = tester.selectAll()
                .where { dateModified less tester.created }
                .single()
            modifiedBeforeCreation[tester.modified].userId shouldBeEqualTo 2
        }
    }

    /**
     * [LocalDateTime]을 DB에 `TIMESTAMP WITH TIME ZONE` 컬럼으로 저장하고, 다른 Time Zone에서 조회한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      timestampz_column TIMESTAMP WITH TIME ZONE NOT NULL
     * );
     * ```
     *
     * ```sql
     * -- Africa/Cairo time zone
     * INSERT INTO tester (timestampz_column)
     * VALUES ('2025-02-04T11:44:53.135595+02:00');
     * -- cairoNowInsertedInCairoTimeZone=2025-02-04T09:44:53.135595Z;
     *
     * -- UTC time zone
     * INSERT INTO tester (timestampz_column)
     * VALUES ('2025-02-04T11:27:49.38008+02:00');
     * -- cairoNowRetrievedInUTCTimeZone=2025-02-04T09:44:53.135595Z;
     * -- cairoNowInsertedInUTCTimeZone=2025-02-04T09:44:53.135595Z
     *
     * -- Asis/Seoul time zone
     * INSERT INTO tester (timestampz_column)
     * VALUES ('2025-02-04T11:27:49.38008+02:00');
     * -- cairoNowInsertedInSeoulTimeZone=2025-02-04T09:44:53.135595Z
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Timestamp with TimeZone`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in timestampWithTimeZoneUnsupportedDB }

        val tester = object: IntIdTable("tester") {
            val timestampWithTimeZone = timestampWithTimeZone("timestampz_column")
        }
        val systemTimeZone = TimeZone.getDefault()

        withTables(testDB, tester) {
            // Africa/Cairo time zone
            TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"))
            ZoneId.systemDefault().id shouldBeEqualTo "Africa/Cairo"

            val cairoNow = OffsetDateTime.now(ZoneId.systemDefault())

            // cairoId = 1
            val cairoId = tester.insertAndGetId {
                it[timestampWithTimeZone] = cairoNow
            }

            val cairoNowInsertedInCairoTimeZone = tester
                .selectAll()
                .where { tester.id eq cairoId }
                .single()[tester.timestampWithTimeZone]
            // UTC 로 반환
            log.debug { "cairoNowInsertedInCairoTimeZone=$cairoNowInsertedInCairoTimeZone" }

            // UTC time zone
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
            ZoneId.systemDefault().id shouldBeEqualTo "UTC"

            val cairoNowRetrievedInUTCTimeZone = tester
                .selectAll()
                .where { tester.id eq cairoId }
                .single()[tester.timestampWithTimeZone]
            // UTC 로 반환
            log.debug { "cairoNowRetrievedInUTCTimeZone=$cairoNowRetrievedInUTCTimeZone" }

            // utcID = 2
            val utcID = tester.insertAndGetId {
                it[timestampWithTimeZone] = cairoNow
            }

            val cairoNowInsertedInUTCTimeZone = tester
                .selectAll()
                .where { tester.id eq utcID }
                .single()[tester.timestampWithTimeZone]
            log.debug { "cairoNowInsertedInUTCTimeZone=$cairoNowInsertedInUTCTimeZone" }

            // Seoul time zone
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
            ZoneId.systemDefault().id shouldBeEqualTo "Asia/Seoul"

            val cairoNowRetrievedInSeoulTimeZone = tester
                .selectAll()
                .where { tester.id eq cairoId }
                .single()[tester.timestampWithTimeZone]

            val seoulID = tester.insertAndGetId {
                it[timestampWithTimeZone] = cairoNow
            }

            val cairoNowInsertedInSeoulTimeZone = tester.selectAll()
                .where { tester.id eq seoulID }
                .single()[tester.timestampWithTimeZone]
            log.debug { "cairoNowInsertedInSeoulTimeZone=$cairoNowInsertedInSeoulTimeZone" }

            // PostgreSQL and MySQL always store the timestamp in UTC, thereby losing the original time zone.
            // To preserve the original time zone, store the time zone information in a separate column.
            val isOriginalTimeZonePreserved = testDB !in (TestDB.ALL_MYSQL + TestDB.ALL_POSTGRES)
            if (isOriginalTimeZonePreserved) {
                // Assert that time zone is preserved when the same value is inserted in different time zones
                cairoNowInsertedInCairoTimeZone shouldTemporalEqualTo cairoNow
                cairoNowInsertedInUTCTimeZone shouldTemporalEqualTo cairoNow
                cairoNowInsertedInSeoulTimeZone shouldTemporalEqualTo cairoNow

                // Assert that time zone is preserved when the same record is retrieved in different time zones
                cairoNowRetrievedInUTCTimeZone shouldTemporalEqualTo cairoNow
                cairoNowRetrievedInSeoulTimeZone shouldTemporalEqualTo cairoNow
            } else {
                // Assert equivalence in UTC when the same value is inserted in different time zones
                cairoNowInsertedInUTCTimeZone shouldTemporalEqualTo cairoNowInsertedInCairoTimeZone
                cairoNowInsertedInSeoulTimeZone shouldTemporalEqualTo cairoNowInsertedInUTCTimeZone

                // Assert equivalence in UTC when the same record is retrieved in different time zones
                cairoNowRetrievedInSeoulTimeZone shouldTemporalEqualTo cairoNowRetrievedInUTCTimeZone
            }

            // Reset to original time zone as set up in DatabaseTestsBase init block
            TimeZone.setDefault(systemTimeZone)
            ZoneId.systemDefault().id shouldBeEqualTo systemTimeZone.id
        }
    }

    /**
     * MySQL V5 만 지원합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Timestamp With TimeZone Throws Exception For Unsupported Dialects`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in timestampWithTimeZoneUnsupportedDB }

        val tester = object: IntIdTable("tester") {
            val timestampWithTimeZone = timestampWithTimeZone("timestampz_column")
        }
        withDb(testDB) {
            expectException<UnsupportedByDialectException> {
                SchemaUtils.create(tester)
            }
        }
    }

    /**
     * `TIMESTAMP WITH TIME ZONE` 컬럼을 사용할 때 확장함수를 사용한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      timestampz_column TIMESTAMP WITH TIME ZONE NOT NULL
     * )
     * ```
     * ```sql
     * INSERT INTO tester (timestampz_column) VALUES ('2023-05-04T05:04:01.123123123Z')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Timestamp with TimeZone extension functions`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in (timestampWithTimeZoneUnsupportedDB + TestDB.ALL_H2_V1) }

        val tester = object: IntIdTable("tester") {
            val timestampWithTimeZone = timestampWithTimeZone("timestampz_column")
        }

        withTables(testDB, tester) {
            // UTC time zone
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
            ZoneId.systemDefault().id shouldBeEqualTo "UTC"

            val nowText = "2023-05-04T05:04:01.123123123+00:00"

            val now: OffsetDateTime = OffsetDateTime.parse(nowText)
            val nowId = tester.insertAndGetId {
                it[timestampWithTimeZone] = now
            }

            // SELECT CAST(tester.timestampz_column AS DATE) FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.date())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.date()] shouldBeEqualTo now.toLocalDate()

            val expectedTime: LocalTime = when (testDB) {
                // FIXME: MySQL_V8 에서는 UTC 가 아닌 시스템 기본 시간대로 저장된다.
                // MySQL 8.4 서버의 시간대가 UTC 이면 UTC 로 저장된다.
                // https://www.perplexity.ai/search/mysql-jdbc-connection-sie-loca-atDZSJ7QQsyTx6nN4OyM.g
                TestDB.MYSQL_V8, in TestDB.ALL_POSTGRES_LIKE ->
                    OffsetDateTime.parse("2023-05-04T05:04:01.123123+00:00")  // NOTE: Microseconds 까지만 지원
                else -> now
            }.toLocalTime()

            // SELECT TO_CHAR(tester.timestampz_column, 'HH24:MI:SS.US') FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.time())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.time()] shouldBeEqualTo expectedTime

            // FIXME: MySQL_V8 에서는 LocalDate 에 대해 Year 함수가 제대로 동작하지 않는다 --> Driver 문제인 듯
            // 에러 메시지: Unexpected value of type Int: 2025-01-01 of java.sql.Date
            // Assumptions.assumeTrue { testDB != TestDB.MYSQL_V8 }
            if (testDB != TestDB.MYSQL_V8) {
                // SELECT Extract(YEAR FROM tester.timestampz_column) FROM tester WHERE tester.id = 1
                tester.select(tester.timestampWithTimeZone.year())
                    .where { tester.id eq nowId }
                    .single()[tester.timestampWithTimeZone.year()] shouldBeEqualTo now.year
            }

            // SELECT Extract(MONTH FROM tester.timestampz_column) FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.month())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.month()] shouldBeEqualTo now.month.value

            // SELECT Extract(DAY FROM tester.timestampz_column) FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.day())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.day()] shouldBeEqualTo now.dayOfMonth

            // SELECT Extract(HOUR FROM tester.timestampz_column) FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.hour())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.hour()] shouldBeEqualTo now.hour

            // SELECT Extract(MINUTE FROM tester.timestampz_column) FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.minute())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.minute()] shouldBeEqualTo now.minute

            // SELECT Extract(SECOND FROM tester.timestampz_column) FROM tester WHERE tester.id = 1
            tester.select(tester.timestampWithTimeZone.second())
                .where { tester.id eq nowId }
                .single()[tester.timestampWithTimeZone.second()] shouldBeEqualTo now.second
        }
    }

    /**
     * [CurrentDateTime] 함수 활용.
     *
     * ```sql
     * SELECT CURRENT_TIMESTAMP
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `current DateTime function`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withDb(testDB) {
            val dateTime = Table.Dual.select(CurrentDateTime).first()[CurrentDateTime]

            // CurrentDateTime=2025-02-27T11:35:17.419449
            log.debug { "CurrentDateTime=$dateTime" }
        }
    }

    /**
     * Postgres에서 [LocalDate], [LocalDateTime]의 Array 방식으로 저장하고 조회한다.
     *
     * ```
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS array_tester (
     *      dates DATE[] DEFAULT ARRAY['2025-02-04'::date] NOT NULL,
     *      datetimes TIMESTAMP[] DEFAULT ARRAY['2025-02-04 09:44:53.184'::timestamp without time zone] NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DateTime as Array`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_H2 + TestDB.POSTGRESQL }

        val defaultDates = listOf(today)
        val defaultDateTimes = listOf(LocalDateTime.now())
        val tester = object: Table("array_tester") {
            val dates = array("dates", JavaLocalDateColumnType()).default(defaultDates)
            val datetimes = array("datetimes", JavaLocalDateTimeColumnType()).default(defaultDateTimes)
        }

        withTables(testDB, tester) {
            tester.insert { }
            val result1 = tester.selectAll().single()
            log.debug { "datetimes=${result1[tester.datetimes]}" }
            result1[tester.dates] shouldBeEqualTo defaultDates
            if (testDB == TestDB.POSTGRESQL) {
                result1[tester.datetimes] shouldBeEqualTo defaultDateTimes.map { it.truncatedTo(MILLIS) }
            } else {
                result1[tester.datetimes] shouldBeEqualTo defaultDateTimes
            }

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO array_tester (dates, datetimes)
             * VALUES (
             *      ARRAY['2020-05-04','2021-05-04','2022-05-04'],
             *      ARRAY['2020-05-04T09:09:09','2021-05-04T09:09:09','2022-05-04T09:09:09']
             * );
             * ```
             */
            val datesInput = List(3) { LocalDate.of(2020 + it, 5, 4) }
            val datetimeInput = List(3) { LocalDateTime.of(2020 + it, 5, 4, 9, 9, 9) }
            tester.insert {
                it[tester.dates] = datesInput
                it[tester.datetimes] = datetimeInput
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT array_tester.dates[3],
             *        array_tester.datetimes[1:2]
             *   FROM array_tester
             *  WHERE Extract(YEAR FROM array_tester.dates[1]) = 2020
             * ```
             */
            val lastDate = tester.dates[3]
            val firstTwoDatetimes = tester.datetimes.slice(1, 2)
            val result2 = tester
                .select(lastDate, firstTwoDatetimes)
                .where { tester.dates[1].year() eq 2020 }
                .single()

            result2[lastDate] shouldBeEqualTo datesInput.last()
            result2[firstTwoDatetimes] shouldBeEqualTo datetimeInput.take(2)
        }
    }

    /**
     * [LocalTime] 을 [time] 컬럼으로 저장하고 조회한다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS with_time (
     *      id SERIAL PRIMARY KEY,
     *      "time" TIME NOT NULL
     * );
     * ```
     * ```sql
     * INSERT INTO with_time ("time") VALUES ('13:05:00');
     *
     * SELECT with_time.id, with_time."time"
     *   FROM with_time
     *  WHERE with_time."time" = '13:05:00';
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select by time literal equality`(testDB: TestDB) {
        val tester = object: IntIdTable("with_time") {
            val time = time("time")
        }
        withTables(testDB, tester) {
            val localTime = LocalTime.of(13, 5)
            val localTimeLiteral = timeLiteral(localTime)

            // UTC time zone
            TimeZone.setDefault(TimeZone.getTimeZone(ZoneOffset.UTC))
            ZoneId.systemDefault().id shouldBeEqualTo "UTC"

            tester.insert {
                it[time] = localTime
            }

            tester.select(tester.id, tester.time)
                .where { tester.time eq localTimeLiteral }  // 조회 시 timeLiteral 로 비교한다.
                .single()[tester.time] shouldBeEqualTo localTime

            tester.select(tester.id, tester.time)
                .where { tester.time eq localTime }  // 조회 시 localTime 으로 비교한다.
                .single()[tester.time] shouldBeEqualTo localTime
        }
    }

    /**
     * [CurrentDate] 를 [date] 컬럼의 default expression 으로 사용한다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_table (
     *      id SERIAL PRIMARY KEY,
     *      "date" DATE DEFAULT CURRENT_DATE NOT NULL
     * );
     *
     * CREATE INDEX test_table_date ON test_table ("date");
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `CurrentDate as default expression`(testDB: TestDB) {
        val tester = object: IntIdTable("test_table") {
            val date: Column<LocalDate> = date("date").index().defaultExpression(CurrentDate)
        }
        withTables(testDB, tester) {
            SchemaUtils.statementsRequiredToActualizeScheme(tester).shouldBeEmpty()
        }
    }
}

private val log = KotlinLogging.logger {}

//
// ---------------------------------------------------------------------------------
//

fun <T: Temporal> T?.isEqualDateTime(other: Temporal?): Boolean = try {
    this shouldTemporalEqualTo other
    true
} catch (_: Exception) {
    false
}

/**
 * DB 마다 지원하는 Java Time 수형의 정밀도가 다르기 때문에, 각 DB 에 맞는 비교 함수를 제공한다.
 *
 * @receiver DB 컬럼 값
 * @param d2 비교할 값
 */
infix fun <T: Temporal> T?.shouldTemporalEqualTo(d2: T?) {
    val d1 = this
    when {
        d1 == null && d2 == null -> return
        d1 == null -> error("d1 is null while d2 is not on ${currentDialectTest.name}")
        d2 == null -> error("d1 is not null while d2 is null on ${currentDialectTest.name}")

        d1 is LocalTime && d2 is LocalTime -> {
            d1.toSecondOfDay() shouldBeEqualTo d2.toSecondOfDay()
            // d1 이 DB에서 읽어온 Temporal 값이어야 한다. (nanos 를 지원하는 Dialect 에서만 비교한다)
            if (d1.nano != 0) {
                d1.nano shouldFractionalPartEqualTo d2.nano
            }
        }

        d1 is LocalDateTime && d2 is LocalDateTime -> {
            d1.toEpochSecond(ZoneOffset.UTC) shouldBeEqualTo d2.toEpochSecond(ZoneOffset.UTC)
            d1.nano shouldFractionalPartEqualTo d2.nano
        }

        d1 is Instant && d2 is Instant -> {
            d1.epochSecond shouldBeEqualTo d2.epochSecond
            d1.nano shouldFractionalPartEqualTo d2.nano
        }

        d1 is OffsetDateTime && d2 is OffsetDateTime -> {
            d1.toLocalDateTime() shouldTemporalEqualTo d2.toLocalDateTime()
            d1.offset shouldBeEqualTo d2.offset
        }

        else -> {
            d1 shouldBeEqualTo d2
        }
    }
}

/**
 * DB 마다 지원하는 nano 단위의 정보를 처리하는 방식이 다릅니다. 각 DB 에 맞는 비교 함수를 제공합니다.
 *
 * @receiver DB 컬럼 중 nanos 값
 * @param nano2 비교할 nanos 값
 */
infix fun Int.shouldFractionalPartEqualTo(nano2: Int) {
    val nano1 = this
    val dialect = currentDialectTest

    when (dialect) {
        // accurate to 100 nanoseconds
        is SQLServerDialect ->
            nano1.nanoRoundTo100Nanos() shouldBeEqualTo nano2.nanoRoundTo100Nanos()

        // microsecond
        is MariaDBDialect ->
            nano1.nanoFloorToMicro() shouldBeEqualTo nano2.nanoFloorToMicro()

        is H2Dialect, is PostgreSQLDialect, is MysqlDialect ->
            when ((dialect as? MysqlDialect)?.isFractionDateTimeSupported()) {
                null, true -> {
                    log.debug { "fractional part nano1: ${nano1.nanoRoundToMicro()}, nano2: ${nano2.nanoRoundToMicro()}" }
                    nano1.nanoRoundToMicro() shouldBeEqualTo nano2.nanoRoundToMicro()
                }

                else -> {} // don't compare fractional part
            }

        // milliseconds
        is OracleDialect ->
            nano1.nanoRoundToMilli() shouldBeEqualTo nano2.nanoRoundToMilli()

        is SQLiteDialect ->
            nano1.nanoFloorToMilli() shouldBeEqualTo nano2.nanoFloorToMilli()

        else ->
            error("Unsupported dialect ${dialect.name}")
    }
}
