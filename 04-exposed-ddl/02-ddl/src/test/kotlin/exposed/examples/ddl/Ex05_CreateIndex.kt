package exposed.examples.ddl

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.sql.getIndices
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Coalesce
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.exists
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.stringLiteral
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex05_CreateIndex: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * 일반적인 인덱스 생성하기
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     *
     * CREATE INDEX tester_by_name ON tester ("name");
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `표준 인덱스 정의`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val id = integer("id").autoIncrement()
            val name = varchar("name", 255)

            override val primaryKey = PrimaryKey(id)

            // 인덱스 정의 
            val byName = index("tester_by_name", isUnique = false, name)
        }

        withDb(testDB) {
            SchemaUtils.createMissingTablesAndColumns(tester)

            val ddl = tester.ddl.single()
            log.info { "tester ddl: $ddl" }

            tester.exists().shouldBeTrue()

            SchemaUtils.drop(tester)
        }
    }

    /**
     * Hash Index 생성하기
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     *
     * CREATE INDEX tester_by_name ON tester USING HASH ("name");
     * ```
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS tester (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` VARCHAR(255) NOT NULL
     * );
     *
     * CREATE INDEX tester_by_name ON tester (`name`) USING HASH;
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Hash Index 생성하기`(testDB: TestDB) {
        val tester = object: Table("tester") {
            val id = integer("id").autoIncrement()
            val name = varchar("name", 255)

            override val primaryKey = PrimaryKey(id)

            // Hash index 정의
            val byName = index("tester_by_name", isUnique = false, name, indexType = "HASH")
        }

        withDb(testDB) {
            SchemaUtils.createMissingTablesAndColumns(tester)

            val ddl = tester.ddl.single()
            log.info { "tester ddl: $ddl" }

            tester.exists().shouldBeTrue()

            SchemaUtils.drop(tester)
        }
    }

    /**
     * 특정 조건일 때만 인덱싱 되는 partial index 정의
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      "value" INT NOT NULL,
     *      another_value INT NOT NULL,
     *      flag BOOLEAN DEFAULT FALSE NOT NULL
     * );
     *
     * CREATE INDEX flag_index ON tester (flag, "name")
     *  WHERE tester.flag = TRUE;
     *
     * CREATE INDEX tester_value_name ON tester ("value", "name")
     *  WHERE (tester."name" = 'aaa') AND (tester."value" >= 6);
     *
     * ALTER TABLE tester
     *      ADD CONSTRAINT tester_another_value_unique UNIQUE (another_value);
     * ```
     */
    @Suppress("DEPRECATION")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `특정 조건일 때만 인덱싱되는 partial index 생성`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }

        val tester = object: IntIdTable("tester") {
            val name = varchar("name", 50)
            val value = integer("value")
            val anotherValue = integer("another_value")
            val flag = bool("flag").default(false)

            init {
                index("flag_index", columns = arrayOf(flag, name)) {
                    flag eq true
                }
                index(columns = arrayOf(value, name)) {
                    (name eq "aaa") and (value greaterEq 6)
                }
                uniqueIndex(anotherValue)
            }
        }

        withDb(testDB) {
            SchemaUtils.createMissingTablesAndColumns(tester)

            log.info { "tester ddl: ${tester.ddl.single()}" }
            tester.exists().shouldBeTrue()

            SchemaUtils.drop(tester)
        }
    }

    /**
     * Indexing 조건을 함수로 표현하는 `functional index` 생성하기
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      amount INT NOT NULL,
     *      price INT NOT NULL,
     *      item VARCHAR(32) NULL
     * );
     *
     * CREATE INDEX tester_plus_index ON tester ((tester.amount * tester.price));
     *
     * CREATE INDEX tester_lower ON tester (LOWER(tester.item));
     *
     * CREATE UNIQUE INDEX tester_price_coalesce_unique
     *  ON tester (price, COALESCE(tester.item, '*'));
     * ```
     *
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS tester (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      amount INT NOT NULL,
     *      price INT NOT NULL,
     *      item VARCHAR(32) NULL
     * );
     *
     * CREATE INDEX tester_plus_index ON tester ((tester.amount * tester.price));
     *
     * CREATE INDEX tester_lower ON tester ((LOWER(tester.item)));
     *
     * CREATE UNIQUE INDEX tester_price_coalesce_unique
     *  ON tester (price, (COALESCE(tester.item, '*')));
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `functional index 생성`(testDB: TestDB) {
        // H2 does not support functional indexes
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2 && testDB != TestDB.MYSQL_V5 && testDB != TestDB.MARIADB }

        val tester = object: IntIdTable("tester") {
            val amount = integer("amount")
            val price = integer("price")
            val item = varchar("item", 32).nullable()

            init {
                index(customIndexName = "tester_plus_index", isUnique = false, functions = listOf(amount.times(price)))
                index(isUnique = false, functions = listOf(item.lowerCase()))
                uniqueIndex(columns = arrayOf(price), functions = listOf(Coalesce(item, stringLiteral("*"))))
            }
        }

        withTables(testDB, tester) {
            log.info { "tester ddl: ${tester.ddl.single()}" }
            tester.exists().shouldBeTrue()

            val indices = tester.getIndices()
            indices.forEach {
                log.info { "index=$it" }
            }
            indices shouldHaveSize 3
        }
    }
}
