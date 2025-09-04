package exposed.examples.dml

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.expectException
import exposed.shared.tests.withTables
import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.concat
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.minus
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.core.statements.BatchUpsertStatement
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.UpsertBuilder
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.core.times
import org.jetbrains.exposed.v1.exceptions.UnsupportedByDialectException
import org.jetbrains.exposed.v1.jdbc.batchUpsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*
import kotlin.properties.Delegates

class Ex04_Upsert: JdbcExposedTestBase() {

    companion object: KLogging()

    // these DB require key columns from ON clause to be included in the derived source table (USING clause)
    private val upsertViaMergeDB = TestDB.ALL_H2

    /**
     * Primary Key 기준으로 [upsert] 하기
     *
     * ```sql
     * -- MySQL V8
     * INSERT INTO auto_inc_table (`name`) VALUES ('B')
     *    AS NEW ON DUPLICATE KEY UPDATE `name`=NEW.`name`;
     *
     * INSERT INTO auto_inc_table (id, `name`) VALUES (1, 'C')
     *     AS NEW ON DUPLICATE KEY UPDATE id=NEW.id, `name`=NEW.`name`;
     * ```
     *
     * ```sql
     * -- Postgres
     * INSERT INTO auto_inc_table ("name") VALUES ('B')
     *  ON CONFLICT (id) DO UPDATE SET "name"=EXCLUDED."name";
     *
     * INSERT INTO auto_inc_table (id, "name") VALUES (1, 'C')
     *     ON CONFLICT (id) DO UPDATE SET "name"=EXCLUDED."name";
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with PK conflict`(testDB: TestDB) {
        withTables(testDB, AutoIncTable) {
            val id1 = AutoIncTable.insert {
                it[name] = "A"
            } get AutoIncTable.id


            // INSERT
            AutoIncTable.upsert {
                if (testDB in upsertViaMergeDB)
                    it[id] = 2
                it[name] = "B"
            }

            // UPDATE
            AutoIncTable.upsert {
                it[id] = id1
                it[name] = "C"
            }

            AutoIncTable.selectAll().forEach {
                log.debug { "id: ${it[AutoIncTable.id]}, name: ${it[AutoIncTable.name]}" }
            }
            AutoIncTable.selectAll().count().toInt() shouldBeEqualTo 2

            val updatedResult = AutoIncTable.selectAll().where { AutoIncTable.id eq id1 }.single()
            updatedResult[AutoIncTable.name] shouldBeEqualTo "C"
        }
    }


    /**
     * [upsert] with Composite PK Conflict
     *
     * ```sql
     * -- Postgres
     * INSERT INTO tester (id_a, id_b, "name") VALUES (1, 1, 'A');
     *
     * INSERT INTO tester (id_a, id_b, "name") VALUES (7, 1, 'B')
     *      ON CONFLICT (id_a, id_b) DO UPDATE SET "name"=EXCLUDED."name";
     *
     * INSERT INTO tester (id_a, id_b, "name") VALUES (99, 99, 'C')
     *      ON CONFLICT (id_a, id_b) DO UPDATE SET "name"=EXCLUDED."name";
     *
     * INSERT INTO tester (id_a, id_b, "name") VALUES (1, 1, 'D')
     *      ON CONFLICT (id_a, id_b) DO UPDATE SET "name"=EXCLUDED."name";
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with composite PK conflict`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id_a INT,
         *      id_b INT,
         *      "name" VARCHAR(64) NOT NULL,
         *
         *      CONSTRAINT pk_tester PRIMARY KEY (id_a, id_b)
         * );
         * ```
         */
        val tester = object: Table("tester") {
            val idA = integer("id_a")
            val idB = integer("id_b")
            val name = varchar("name", 64)

            override val primaryKey = PrimaryKey(idA, idB)
        }

        withTables(testDB, tester) {
            val insertStmt = tester.insert {
                it[idA] = 1
                it[idB] = 1
                it[name] = "A"
            }

            // Insert - PK 컬럼 2개 중 1개의 constraint 만 일치하는 경우
            tester.upsert {
                it[idA] = 7
                it[idB] = insertStmt get tester.idB
                it[name] = "B"
            }

            // Insert - 2개의 constraint 가 모두 일치하지 않는 경우
            tester.upsert {
                it[idA] = 99
                it[idB] = 99
                it[name] = "C"
            }

            // Update - 2개의 constraint 가 모두 일치하는 경우
            tester.upsert {
                it[idA] = insertStmt get tester.idA     // 1
                it[idB] = insertStmt get tester.idB     // 1
                it[name] = "D"
            }

            tester
                .selectAll()
                .forEach {
                    log.debug { "idA: ${it[tester.idA]}, idB: ${it[tester.idB]}, name: ${it[tester.name]}" }
                }
            tester.selectAll().count() shouldBeEqualTo 3L

            val updatedResult = tester
                .selectAll()
                .where { tester.idA eq insertStmt[tester.idA] }
                .single()

            updatedResult[tester.name] shouldBeEqualTo "D"
        }
    }


    /**
     * [upsert] 할 모든 컬럼이 PK 에 속한 경우, conflict 시에 update 를 수행한다. (그래봐야 모든 컬럼이 PK 이므로 Update 해도 동일한 값이다)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with all columns in PK`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      user_id VARCHAR(32),
         *      key_id VARCHAR(32),
         *
         *      CONSTRAINT pk_tester PRIMARY KEY (user_id, key_id)
         * );
         * ```
         */
        val tester = object: Table("tester") {
            val userId = varchar("user_id", 32)
            val keyId = varchar("key_id", 32)

            override val primaryKey = PrimaryKey(userId, keyId)
        }

        /**
         * ```sql
         * INSERT INTO tester (user_id, key_id) VALUES ('User A', 'Key A')
         *      ON CONFLICT (user_id, key_id) DO
         *      UPDATE SET user_id=EXCLUDED.user_id,
         *                 key_id=EXCLUDED.key_id
         * ```
         */
        fun upsertOnlyKeyColumns(values: Pair<String, String>) {
            tester.upsert {
                it[userId] = values.first
                it[keyId] = values.second
            }
        }

        withTables(testDB, tester) {
            val primaryKeyValues = Pair("User A", "Key A")

            // 새로운 행 추가
            upsertOnlyKeyColumns(primaryKeyValues)

            // 기존 컬럼을 `update`
            upsertOnlyKeyColumns(primaryKeyValues)

            tester.selectAll().forEach {
                log.debug { "userId: ${it[tester.userId]}, keyId: ${it[tester.keyId]}" }
            }
            val result = tester.selectAll().singleOrNull()

            result.shouldNotBeNull()
            val resultValues = Pair(result[tester.userId], result[tester.keyId])
            resultValues shouldBeEqualTo primaryKeyValues
        }
    }


    /**
     * unique index 를 기준으로 [upsert]
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with unique index conflict`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withTables(testDB, Words) {
            /**
             * Insert
             * ```sql
             * -- Postgres
             * INSERT INTO words ("name", "count") VALUES ('A', 10)
             *  ON CONFLICT ("name") DO UPDATE SET "count"=EXCLUDED."count"
             * ```
             */
            val wordA = Words.upsert {
                it[word] = "A"
                it[count] = 10
            } get Words.word

            /**
             * Insert - word 가 conflict 되지 않는 경우
             * ```sql
             * -- Postgres
             * INSERT INTO words ("name", "count") VALUES ('B', 10)
             *  ON CONFLICT ("name") DO UPDATE SET "count"=EXCLUDED."count"
             * ```
             */
            Words.upsert {
                it[word] = "B"
                it[count] = 10
            }

            /**
             * Update - word 가 conflict 되는 경우
             * ```sql
             * -- Postgres
             * INSERT INTO words ("name", "count") VALUES ('A', 9)
             *      ON CONFLICT ("name") DO
             *      UPDATE SET "count"=EXCLUDED."count"
             * ```
             */
            Words.upsert {
                it[word] = wordA     // "A"
                it[count] = 9
            }

            Words.selectAll().forEach {
                log.debug { "word: ${it[Words.word]}, count: ${it[Words.count]}" }
            }
            Words.selectAll().count() shouldBeEqualTo 2L   // A, B

            // SELECT words."name", words."count" FROM words WHERE words."name" = 'A'
            val updatedResult = Words.selectAll().where { Words.word eq wordA }.single()
            updatedResult[Words.count] shouldBeEqualTo 9
        }
    }

    /**
     * Upsert with manual conflict keys
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with manual conflict keys`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in (TestDB.ALL_MYSQL_LIKE + TestDB.ALL_H2_V1) }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id_a INT NOT NULL,
         *      id_b INT NOT NULL,
         *      "name" VARCHAR(64) NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_id_a_unique UNIQUE (id_a);
         * ALTER TABLE tester ADD CONSTRAINT tester_id_b_unique UNIQUE (id_b);
         * ```
         */
        val tester = object: Table("tester") {
            val idA = integer("id_a").uniqueIndex()
            val idB = integer("id_b").uniqueIndex()
            val name = varchar("name", 64)
        }

        withTables(testDB, tester) {
            // insert
            val oldIdA = tester.insert {
                it[idA] = 1
                it[idB] = 1
                it[name] = "A"
            } get tester.idA

            /**
             * updated - idA 가 충돌하는 경우에는 Update
             * ```sql
             * -- Postgres
             * INSERT INTO tester (id_a, id_b, "name") VALUES (1, 2, 'B')
             *      ON CONFLICT (id_a) DO
             *      UPDATE SET id_b=EXCLUDED.id_b,
             *                 "name"=EXCLUDED."name"
             * ```
             */
            val newIdB = tester.upsert(tester.idA) {
                it[idA] = oldIdA
                it[idB] = 2
                it[name] = "B"
            } get tester.idB        // 2

            tester.selectAll().single()[tester.name] shouldBeEqualTo "B"

            /**
             * updated - idB 가 충돌하는 경우에는 Update
             * ```sql
             * -- Postgres
             * INSERT INTO tester (id_a, id_b, "name") VALUES (99, 2, 'C')
             *     ON CONFLICT (id_b) DO
             *     UPDATE SET id_a=EXCLUDED.id_a,
             *                "name"=EXCLUDED."name"
             * ```
             */
            val newIdA = tester.upsert(tester.idB) {
                it[idA] = 99
                it[idB] = newIdB
                it[name] = "C"
            } get tester.idA        // 99

            // idA: 99, idB: 2, name: C
            tester.selectAll().forEach {
                log.debug { "idA: ${it[tester.idA]}, idB: ${it[tester.idB]}, name: ${it[tester.name]}" }
            }
            tester.selectAll().single()[tester.name] shouldBeEqualTo "C"

            if (testDB in upsertViaMergeDB) {
                /**
                 * upsert 시에 conflict keys 를 지정할 수 있다.
                 *
                 * ```sql
                 * -- H2
                 * MERGE INTO TESTER T
                 * USING (VALUES (99, 2, 'D')) S(ID_A, ID_B, "name") ON (T.ID_A=S.ID_A AND T.ID_B=S.ID_B)
                 *  WHEN MATCHED THEN UPDATE
                 *      SET T."name"=S."name"
                 *  WHEN NOT MATCHED THEN
                 *      INSERT (ID_A, ID_B, "name") VALUES(S.ID_A, S.ID_B, S."name")
                 * ```
                 */
                tester.upsert(tester.idA, tester.idB) {
                    it[idA] = newIdA        // 99
                    it[idB] = newIdB        // 2
                    it[name] = "D"
                }

                val result = tester.selectAll().single()
                result[tester.idA] shouldBeEqualTo newIdA
                result[tester.idB] shouldBeEqualTo newIdB
                result[tester.name] shouldBeEqualTo "D"
            }
        }
    }


    /**
     * UUID 수형의 Primary Key를 기준으로 [upsert] 하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with UUID Key conflict`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      id uuid PRIMARY KEY,
         *      title TEXT NOT NULL
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val id = uuid("id").autoGenerate()
            val title = text("title")

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, tester) {
            /**
             * Insert
             * ```sql
             * -- Postgres
             * INSERT INTO tester (id, title)
             * VALUES ('2a6167bc-d495-4de7-b9f7-0b52b3ab8c3c', 'A')
             *      ON CONFLICT (id) DO UPDATE SET title=EXCLUDED.title
             * ```
             */
            val uuid1 = tester.upsert {
                it[title] = "A"
            } get tester.id

            /**
             * Update (id가 동일한 경우)
             * ```sql
             * -- Postgres
             * INSERT INTO tester (id, title)
             * VALUES ('2a6167bc-d495-4de7-b9f7-0b52b3ab8c3c', 'B')
             *      ON CONFLICT (id) DO UPDATE SET title=EXCLUDED.title
             * ```
             */
            tester.upsert {
                it[id] = uuid1
                it[title] = "B"
            }

            val result = tester.selectAll().single()
            result[tester.id] shouldBeEqualTo uuid1
            result[tester.title] shouldBeEqualTo "B"
        }
    }

    /**
     * Unique 제약조건이 없는 테이블에 대한 Upsert 는 INSERT 만 수행한다. (단, MySQL, SQLite 에서만 가능)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with no unique constraints`(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      "name" VARCHAR(64) NOT NULL
         * )
         * ```
         */
        val tester = object: Table("tester") {
            val name = varchar("name", 64)
        }

        val okWithNoUniquenessDB = TestDB.ALL_MYSQL_LIKE + TestDB.ALL_MARIADB_LIKE

        withTables(testDB, tester) {
            if (testDB in okWithNoUniquenessDB) {
                /**
                 * MySQL, MariaDB 만 가능합니다.
                 * ```sql
                 * -- MySQL V8
                 * INSERT INTO tester (`name`) VALUES ('A')
                 * AS NEW ON DUPLICATE KEY UPDATE `name`=NEW.`name`
                 * ```
                 */
                tester.upsert {
                    it[name] = "A"
                }
                tester.selectAll().count().toInt() shouldBeEqualTo 1
            } else {
                expectException<UnsupportedByDialectException> {
                    tester.upsert {
                        it[name] = "A"
                    }
                }
            }
        }
    }

    /**
     * [upsert] 작업 시 update 작업에서만 특정 컬럼을 변경할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with manual update assignment`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withTables(testDB, Words) {
            val testWord = "Test"

            /**
             * `upsert` 실행 시, `update` 작업이라면 `count` 컬럼이 1씩 증가한다.
             *
             * 첫번째는 Insert, 두번째, 세번째는 Update 를 수행하도록 한다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO words ("name") VALUES ('Test')
             * ON CONFLICT ("name") DO
             *      UPDATE SET "count"=(words."count" + 1)
             * ```
             */
            repeat(3) {
                Words.upsert(onUpdate = { it[Words.count] = Words.count + 1 }) {
                    it[word] = testWord
                }
            }
            Words.selectAll().single()[Words.count] shouldBeEqualTo 3  // count 기본값이 1, 2번 증가 -> 3

            /**
             * `upsert` 실행 시, `update` 작업이 수행할 조건이라면 `count` 컬럼이 1000 으로 변경된다.
             *
             * ```sql
             * -- MySQL V8
             * INSERT INTO words (`name`) VALUES ('Test')
             *  AS NEW ON DUPLICATE KEY UPDATE `count`=1000
             * ```
             * ```sql
             * -- Postgres
             * INSERT INTO words ("name") VALUES ('Test')
             *  ON CONFLICT ("name") DO
             *  UPDATE SET "count"=1000
             * ```
             */
            val updatedCount = 1000
            Words.upsert(onUpdate = { it[Words.count] = updatedCount }) {
                it[word] = testWord
            }
            Words.selectAll().single()[Words.count] shouldBeEqualTo updatedCount
        }
    }

    /**
     * [upsert] 작업 중 update 작업이 수행될 때, 추가적인 작업을 수행할 수 있습니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with multiple manual updates`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      item VARCHAR(64) NOT NULL,
         *      amount INT DEFAULT 25 NOT NULL,
         *      gains INT DEFAULT 100 NOT NULL,
         *      losses INT DEFAULT 100 NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_item_unique UNIQUE (item);
         * ```
         */
        val tester = object: Table("tester") {
            val item = varchar("item", 64).uniqueIndex()
            val amount = integer("amount").default(25)
            val gains = integer("gains").default(100)
            val losses = integer("losses").default(100)
        }

        /**
         * [upsert] 작업 중 update 작업이 수행될 때, 추가적인 작업을 지정합니다.
         */
        fun UpsertBuilder.adjustGainAndLoss(statement: UpdateStatement) {
            statement[tester.gains] = tester.gains + tester.amount
            statement[tester.losses] = tester.losses - insertValue(tester.amount)
        }

        withTables(testDB, tester) {
            // INSERT INTO tester (item) VALUES ('Item A') ON CONFLICT (item) DO UPDATE SET item=EXCLUDED.item
            val itemA = tester.upsert {
                it[item] = "Item A"
            } get tester.item

            /**
             * `upsert` 작업에서 `update` 시에 `onUpdate` 를 지정하여 다른 작업을 수행할 수 있도록 한다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester (item, gains, losses, amount) VALUES ('Item B', 200, 0, 25)
             *      ON CONFLICT (item) DO
             *      UPDATE SET gains=(tester.gains + tester.amount),
             *                 losses=(tester.losses - EXCLUDED.amount)
             * ```
             */
            tester.upsert(onUpdate = { adjustGainAndLoss(it) }) {
                it[item] = "Item B"
                it[gains] = 200
                it[losses] = 0
                // `amount` must be passed explicitly now due to usage of that column inside the custom onUpdate statement
                // There is an option to call `tester.amount.defaultValueFun?.let { it() }!!`,
                // it looks ugly but prevents regression on changes in default value
                it[amount] = 25
            }

            val insertResult = tester.selectAll().where { tester.item neq itemA }.single()
            insertResult[tester.amount] shouldBeEqualTo 25
            insertResult[tester.gains] shouldBeEqualTo 200
            insertResult[tester.losses] shouldBeEqualTo 0

            /**
             * `upsert` 작업에서 `update` 시에 `onUpdate` 를 지정하여 다른 작업을 수행할 수 있도록 한다.
             *
             * insert 시에 amount=10, gains=200, losses=0 이고, update 시에는 gains=100 + 25, losses=100-10 이 된다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester (item, amount, gains, losses) VALUES ('Item A', 10, 200, 0)
             *      ON CONFLICT (item) DO
             *      UPDATE SET gains=(tester.gains + tester.amount),
             *                 losses=(tester.losses - EXCLUDED.amount)
             * ```
             */
            tester.upsert(onUpdate = { adjustGainAndLoss(it) }) {
                it[item] = itemA
                it[amount] = 10
                it[gains] = 200
                it[losses] = 0
            }

            val updateResult = tester.selectAll().where { tester.item eq itemA }.single()
            updateResult[tester.gains] shouldBeEqualTo 100 + 25
            updateResult[tester.losses] shouldBeEqualTo 100 - 10
        }
    }

    /**
     * [upsert] 작업 중 update 시에 expression 사용하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with column expression`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      word VARCHAR(256) NOT NULL,
         *      phrase VARCHAR(256) DEFAULT ('Phrase') NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_word_unique UNIQUE (word);
         * ```
         */
        val defaultPhrase = "Phrase"
        val tester = object: Table("tester") {
            val word = varchar("word", 256).uniqueIndex()
            val phrase = varchar("phrase", 256).defaultExpression(stringParam(defaultPhrase))
        }

        withTables(testDB, tester) {
            val testWord = "Test"

            // insert default expression
            tester.upsert {
                it[word] = testWord
            }
            tester.selectAll().single()[tester.phrase] shouldBeEqualTo defaultPhrase

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO tester (word) VALUES ('Test')
             * ON CONFLICT (word) DO
             *      UPDATE SET phrase=CONCAT_WS(' - ',tester.word, tester.phrase)
             * ```
             */
            tester.upsert(
                onUpdate = { it[tester.phrase] = concat(" - ", listOf(tester.word, tester.phrase)) }
            ) {
                it[word] = testWord
            }
            tester.selectAll().single()[tester.phrase] shouldBeEqualTo "$testWord - $defaultPhrase"

            /**
             * 멀리라인, 특수문자가 들어간 문자열로 Update 하는 예
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester (word) VALUES ('Test')
             * ON CONFLICT (word) DO
             *      UPDATE SET phrase='This is a phrase with a new line\nand some other difficult strings ''\n\nIndentation should be preserved'
             * ```
             */
            val multilinePhrase =
                """
                This is a phrase with a new line
                and some other difficult strings '

                Indentation should be preserved
                """.trimIndent()

            tester.upsert(
                onUpdate = { it[tester.phrase] = multilinePhrase }
            ) {
                it[word] = testWord
            }
            tester.selectAll().single()[tester.phrase] shouldBeEqualTo multilinePhrase

            /**
             * [upsert] 수행 중 insert 시에 expression 을 사용
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester (word, phrase) VALUES ('Test 2', CONCAT('foo', 'bar'))
             * ON CONFLICT (word) DO
             *      UPDATE SET phrase=EXCLUDED.phrase
             * ```
             */
            tester.upsert {
                it[word] = "$testWord 2"
                it[phrase] = concat(stringLiteral("foo"), stringLiteral("bar")) // 'foobar'
            }

            tester.selectAll()
                .where { tester.word eq "$testWord 2" }
                .single()[tester.phrase] shouldBeEqualTo "foobar"
        }
    }

    /**
     * [upsert] 작업 중 update 시에 함수를 이용하여 컬럼 값을 변경하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with manual update using insert values`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        val tester = object: Table("tester") {
            val id = integer("id").uniqueIndex()
            val word = varchar("name", 64)
            val count = integer("count").default(1)
        }

        withTables(testDB, tester) {
            tester.insert {
                it[id] = 1
                it[word] = "Word A"
            }

            tester.selectAll().single()[tester.count] shouldBeEqualTo 1

            /**
             * ```sql
             * -- MySQL V8
             * INSERT INTO tester (id, `name`, `count`) VALUES (1, 'Word B', 9)
             * AS NEW ON DUPLICATE KEY UPDATE `count`=(100 * NEW.`count`)
             * ```
             * ```sql
             * -- Postgres
             * INSERT INTO tester (id, "name", "count") VALUES (1, 'Word B', 9)
             * ON CONFLICT (id) DO UPDATE SET "count"=(100 * EXCLUDED."count")
             * ```
             */
            tester.upsert(
                onUpdate = { it[tester.count] = intLiteral(100) times insertValue(tester.count) }
            ) {
                it[id] = 1
                it[word] = "Word B"
                it[count] = 9
            }
            val result = tester.selectAll().single()
            result[tester.count] shouldBeEqualTo 100 * 9

            val newWords = listOf(
                Triple(2, "Word B", 2),
                Triple(1, "Word A", 3),
                Triple(3, "Word C", 4),
            )

            /**
             * id: 1 인 경우에만 Update 된다.
             *
             * ```sql
             * -- MySQL V8
             * INSERT INTO tester (id, `name`, `count`) VALUES (2, 'Word B', 2)
             *      AS NEW ON DUPLICATE KEY
             *      UPDATE `name`=CONCAT(tester.`name`, ' || ', NEW.`count`),
             *             `count`=(1 + NEW.`count`)
             * ```
             * ```sql
             * -- Postgres
             * INSERT INTO tester (id, "name", "count") VALUES (2, 'Word B', 2)
             *      ON CONFLICT (id) DO
             *      UPDATE SET "name"=CONCAT(tester."name", ' || ', EXCLUDED."count"),
             *                 "count"=(1 + EXCLUDED."count")
             * ```
             */
            tester.batchUpsert(
                newWords,
                onUpdate = {
                    it[tester.word] = concat(tester.word, stringLiteral(" || "), insertValue(tester.count))
                    it[tester.count] = intLiteral(1) plus insertValue(tester.count)
                }
            ) { (id, word, count) ->
                this[tester.id] = id
                this[tester.word] = word
                this[tester.count] = count
            }
            tester.selectAll().count().toInt() shouldBeEqualTo 3
            tester.selectAll().forEach {
                log.debug { "id: ${it[tester.id]}, word: ${it[tester.word]}, count: ${it[tester.count]}" }
            }

            val updatedWord = tester.selectAll().where { tester.id eq 1 }.single()

            updatedWord[tester.word] shouldBeEqualTo "Word A || 3"
            updatedWord[tester.count] shouldBeEqualTo 1 + 3
        }
    }

    /**
     * [upsert] 에서 Update 작업일 때, 특정 컬럼을 제외할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with update excluding columns`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      item VARCHAR(64) NOT NULL,
         *      code uuid NOT NULL,
         *      gains INT NOT NULL,
         *      losses INT NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_item_unique UNIQUE (item)
         * ```
         */
        val tester = object: Table("tester") {
            val item = varchar("item", 64).uniqueIndex()
            val code = uuid("code").clientDefault { UUID.randomUUID() }
            val gains = integer("gains")
            val losses = integer("losses")
        }

        withTables(testDB, tester, configure = { useNestedTransactions = true }) {
            // insert
            val itemA = "Item A"
            tester.upsert {
                it[item] = itemA
                it[gains] = 50
                it[losses] = 50
            }

            val (insertCode, insertGains, insertLosses) = tester.selectAll().single().let {
                Triple(it[tester.code], it[tester.gains], it[tester.losses])
            }

            transaction {
                /**
                 * upsert 에 지정되지 않은 컬럼(code)은 기본값으로 업데이트 된다.
                 *
                 * ```sql
                 * -- Postgres
                 * INSERT INTO tester (code, item, gains, losses)
                 * VALUES ('c901e3df-286f-4e3b-a053-a363fcbe32e9', 'Item A', 200, 0)
                 * ON CONFLICT (item) DO
                 *      UPDATE SET code=EXCLUDED.code,
                 *                 gains=EXCLUDED.gains,
                 *                 losses=EXCLUDED.losses
                 * ```
                 */
                tester.upsert {
                    it[item] = itemA
                    it[gains] = 200
                    it[losses] = 0
                }

                val (updateCode, updateGain, updateLosses) = tester.selectAll().single().let {
                    Triple(it[tester.code], it[tester.gains], it[tester.losses])
                }
                updateCode shouldNotBeEqualTo insertCode
                updateGain shouldNotBeEqualTo insertGains
                updateLosses shouldNotBeEqualTo insertLosses

                rollback()
            }

            /**
             * `onUpdateExclude` 에 지정된 컬럼은 업데이트 시 제외된다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester (code, item, gains, losses)
             * VALUES ('c901e3df-286f-4e3b-a053-a363fcbe32e9', 'Item A', 200, 0)
             * ON CONFLICT (item) DO
             *      UPDATE SET losses=EXCLUDED.losses  -- code, gains 는 업데이트 되지 않음
             * ```
             */
            tester.upsert(onUpdateExclude = listOf(tester.code, tester.gains)) {
                it[item] = itemA
                it[gains] = 200
                it[losses] = 0
            }

            val (updateCode, updateGain, updateLosses) = tester.selectAll().single().let {
                Triple(it[tester.code], it[tester.gains], it[tester.losses])
            }

            updateCode shouldBeEqualTo insertCode       // upsert 에서 제외
            updateGain shouldBeEqualTo insertGains      // upsert 에서 제외
            updateLosses shouldNotBeEqualTo insertLosses  // updatedLoses = 0, insertLosses = 50
        }
    }

    /**
     * [upsert] 작업 중 update 시에 where 조건 걸기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with Where`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in (TestDB.ALL_MYSQL_LIKE + upsertViaMergeDB) }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      "name" VARCHAR(64) NOT NULL,
         *      address VARCHAR(256) NOT NULL,
         *      age INT NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_name_unique UNIQUE ("name");
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val name = varchar("name", 64).uniqueIndex()
            val address = varchar("address", 256)
            val age = integer("age")
        }

        withTables(testDB, tester) {
            val id1: EntityID<Int> = tester.insertAndGetId {
                it[name] = "A"
                it[address] = "Place A"
                it[age] = 10
            }

            // insert
            val unchanged: InsertStatement<Number> = tester.insert {
                it[name] = "B"
                it[address] = "Place B"
                it[age] = 50
            }

            val ageTooLow = tester.age less intLiteral(15)

            /**
             * Update 시에 where 조건에 맞는 행만 업데이트 된다
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester ("name", address, age)
             * VALUES ('A', 'Address A', 20)
             * ON CONFLICT ("name") DO
             *      UPDATE SET address=EXCLUDED.address,
             *             age=EXCLUDED.age
             *       WHERE tester.age < 15
             * ```
             */
            val updatedAge = tester.upsert(tester.name, where = { ageTooLow }) {
                it[name] = "A"
                it[address] = "Address A"
                it[age] = 20
            } get tester.age        // 20

            log.debug { "updatedAge: $updatedAge" }

            /**
             * Update 시에 where 조건에 맞는 행만 업데이트 된다
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester ("name", address, age)
             * VALUES ('B', 'Address B', 20)
             * ON CONFLICT ("name") DO
             *      UPDATE SET address=EXCLUDED.address,
             *                 age=EXCLUDED.age
             *       WHERE tester.age < 15
             * ```
             */
            tester.upsert(tester.name, where = { ageTooLow }) {
                it[name] = "B"
                it[address] = "Address B"
                it[age] = 20
            }

            val rows = tester.selectAll().toList()
            rows.forEach {
                log.debug { "id: ${it[tester.id]}, name: ${it[tester.name]}, address: ${it[tester.address]}, age: ${it[tester.age]}" }
            }
            rows shouldHaveSize 2

            val unchangedResult = tester.selectAll().where { tester.id eq unchanged[tester.id] }.single()
            unchangedResult[tester.address] shouldBeEqualTo unchanged[tester.address]

            val updatedResult = tester.selectAll().where { tester.id eq id1 }.single()
            updatedResult[tester.age] shouldBeEqualTo updatedAge
        }
    }

    /**
     * [upsert] 시에 `where` 조건 절 사용하기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with where parameterized`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in (TestDB.ALL_MYSQL_LIKE + upsertViaMergeDB) }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (
         *      id SERIAL PRIMARY KEY,
         *      "name" VARCHAR(64) NOT NULL,
         *      age INT NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_name_unique UNIQUE ("name")
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val name = varchar("name", 64).uniqueIndex()
            val age = integer("age")
        }

        withTables(testDB, tester) {
            val id1 = tester.insert {
                it[name] = "Anya"
                it[age] = 10
            } get tester.id

            /**
             * Insert
             * ```sql
             * -- Postgres
             * INSERT INTO tester ("name", age) VALUES ('Anna', 50)
             * ON CONFLICT (id) DO
             *      UPDATE SET "name"=EXCLUDED."name",
             *                 age=EXCLUDED.age
             * ```
             */
            tester.upsert {
                it[name] = "Anna"
                it[age] = 50
            }

            val nameStartsWithA = tester.name like "A%"
            val nameEndsWithA = tester.name like stringLiteral("%a")
            val nameIsNotAnna = tester.name neq stringParam("Anna")
            val updatedAge = 20

            /**
             * `upsert` 작업 중 `update` 시에만 조건 절을 지정할 수 있다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO tester ("name", age) VALUES ('Anya', 20)
             * ON CONFLICT ("name") DO
             *      UPDATE SET age=EXCLUDED.age
             *       WHERE (tester."name" LIKE 'A%') AND (tester."name" LIKE '%a') AND (tester."name" <> 'Anna')
             * ```
             */
            tester.upsert(tester.name, where = { nameStartsWithA and nameEndsWithA and nameIsNotAnna }) {
                it[name] = "Anya"
                it[age] = updatedAge
            }

            tester.selectAll().forEach {
                log.debug { "id: ${it[tester.id]}, name: ${it[tester.name]}, age: ${it[tester.age]}" }
            }
            tester.selectAll().count() shouldBeEqualTo 2L

            val updatedResult = tester.selectAll().where { tester.age eq updatedAge }.single()
            updatedResult[tester.id] shouldBeEqualTo id1
        }
    }

    /**
     * [upsert] 작업에 subQuery 사용하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert with subQuery`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester_1 (
         *      id SERIAL PRIMARY KEY,
         *      "name" VARCHAR(32) NOT NULL
         * )
         * ```
         */
        val tester1 = object: IntIdTable("tester_1") {
            val name = varchar("name", 32)
        }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester_2 (
         *      id SERIAL PRIMARY KEY,
         *      "name" VARCHAR(32) NOT NULL
         * )
         * ```
         */
        val tester2 = object: IntIdTable("tester_2") {
            val name = varchar("name", 32)
        }

        withTables(testDB, tester1, tester2) {
            val id1 = tester1.insertAndGetId {
                it[name] = "foo"
            }
            val id2 = tester1.insertAndGetId {
                it[name] = "bar"
            }

            /**
             * `upsert` 작업 중 `insert` 시에는 subquery 결과를 사용한다.
             * ```sql
             * -- MySQL V8
             * INSERT INTO tester_2 (`name`)
             * VALUES ((SELECT tester_1.`name`
             *            FROM tester_1
             *           WHERE tester_1.id = 1))
             * AS NEW ON DUPLICATE KEY UPDATE `name`=NEW.`name`
             * ```
             * ```sql
             * -- Postgres
             * INSERT INTO tester_2 ("name")
             * VALUES ((SELECT tester_1."name"
             *            FROM tester_1
             *           WHERE tester_1.id = 1))
             *      ON CONFLICT (id) DO UPDATE SET "name"=EXCLUDED."name"
             * ```
             */
            val query1 = tester1.select(tester1.name).where { tester1.id eq id1 }
            val id3 = tester2.upsert {
                if (testDB in upsertViaMergeDB)
                    it[id] = 1
                it[name] = query1
            } get tester2.id

            tester2.selectAll().single()[tester2.name] shouldBeEqualTo "foo"

            /**
             * `upsert` 작업 중 `name` 컬럼에 subquery 결과를 사용한다.
             *
             * ```sql
             * -- MySQL V8
             * INSERT INTO tester_2 (id, `name`)
             * VALUES (1, (SELECT tester_1."name" FROM tester_1 WHERE tester_1.id = 2))
             * AS NEW ON DUPLICATE KEY UPDATE id=NEW.id, `name`=NEW.`name`
             * ```
             * ```sql
             * -- Postgres
             * INSERT INTO tester_2 (id, "name")
             * VALUES (1, (SELECT tester_1."name" FROM tester_1 WHERE tester_1.id = 2))
             * ON CONFLICT (id) DO UPDATE SET "name"=EXCLUDED."name"
             * ```
             */
            val query2 = tester1.select(tester1.name).where { tester1.id eq id2 }
            tester2.upsert {
                it[id] = id3
                it[name] = query2
            }
            tester2.selectAll().single()[tester2.name] shouldBeEqualTo "bar"
        }
    }

    /**
     * [batchUpsert] 작업 중 conflict 가 없다면 모든 행이 INSERT 된다.
     *
     * ```sql
     * -- MySQL V8
     * INSERT INTO words (`name`, `count`) VALUES ('Word A', 10)
     *      AS NEW ON DUPLICATE KEY
     *      UPDATE `name`=NEW.`name`, `count`=NEW.`count`;
     * INSERT INTO words (`name`, `count`) VALUES ('Word B', 20)
     *      AS NEW ON DUPLICATE KEY
     *      UPDATE `name`=NEW.`name`, `count`=NEW.`count`;
     * -- ...
     * INSERT INTO words (`name`, `count`) VALUES ('Word J', 100)
     *      AS NEW ON DUPLICATE KEY
     *      UPDATE `name`=NEW.`name`, `count`=NEW.`count`;
     * ```
     *
     * ```sql
     * -- Postgres
     * INSERT INTO words ("name", "count") VALUES ('Word A', 10)
     *      ON CONFLICT ("name") DO
     *      UPDATE SET "count"=EXCLUDED."count";
     * INSERT INTO words ("name", "count") VALUES ('Word B', 20)
     *      ON CONFLICT ("name") DO
     *      UPDATE SET "count"=EXCLUDED."count";
     * -- ...
     * INSERT INTO words ("name", "count") VALUES ('Word J', 100)
     *      ON CONFLICT ("name") DO
     *      UPDATE SET "count"=EXCLUDED."count";
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert with no conflict`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withTables(testDB, Words) {
            val amountOfWords = 10
            val allWords: List<Pair<String, Int>> = List(amountOfWords) { i ->
                "Word ${'A' + i}" to amountOfWords * i + amountOfWords     // Pair("Word A", 10), Pair("Word B", 20), ...
            }

            val generatedWords: List<ResultRow> = Words.batchUpsert(allWords) { (word, count) ->
                this[Words.word] = word
                this[Words.count] = count
            }

            generatedWords.forEach {
                log.debug { "Generated Word: ${it[Words.word]}, ${it[Words.count]}" }
            }
            generatedWords shouldHaveSize amountOfWords
            // 모든 행이 추가되었다.
            Words.selectAll().count() shouldBeEqualTo amountOfWords.toLong()
        }
    }

    /**
     * [batchUpsert] 작업 시 conflict 가 있다면 UPDATE 작업을 수행한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert with conflict`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withTables(testDB, Words) {
            val vowels = listOf("A", "E", "I", "O", "U")
            val alphabet = ('A'..'Z').map { it.toString() }
            val lettersWithDuplicates = alphabet + vowels

            Words.batchUpsert(
                lettersWithDuplicates,
                onUpdate = { it[Words.count] = Words.count + 1 }
            ) { letter ->
                this[Words.word] = letter
            }

            Words.selectAll().count().toInt() shouldBeEqualTo alphabet.size
            Words.selectAll().forEach {
                val expectedCount = if (it[Words.word] in vowels) 2 else 1
                it[Words.count] shouldBeEqualTo expectedCount
            }
        }
    }

    /**
     * [batchUpsert] 작업 시 sequence 를 사용하기
     *
     * ```sql
     * -- Postgres
     * INSERT INTO words ("name") VALUES ('RC1TL3Sy4B9B')
     *      ON CONFLICT ("name") DO
     *          UPDATE SET "name"=EXCLUDED."name";
     *
     * -- ...
     * INSERT INTO words ("name") VALUES ('iF15dJo55zHS')
     *      ON CONFLICT ("name") DO
     *          UPDATE SET "name"=EXCLUDED."name";
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpdate with sequence`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withTables(testDB, Words) {
            val amountOfWords = 25
            val allWords = List(amountOfWords) { Base58.randomString(12) }.asSequence()

            Words.batchUpsert(allWords) { word ->
                this[Words.word] = word
            }

            Words.selectAll().count() shouldBeEqualTo amountOfWords.toLong()
        }
    }

    /**
     * [batchUpsert] 작업 시 조건절 사용하기
     *
     * ```sql
     * -- Postgres
     * INSERT INTO words ("name") VALUES ('A')
     *   ON CONFLICT ("name") DO
     *      UPDATE SET "count"=(words."count" + 1)
     *       WHERE words."name" IN ('A', 'E', 'I');  -- update 시 'A', 'E', 'I' 만 count를 증가시킨다.
     *
     * -- ~~~
     *
     * INSERT INTO words ("name") VALUES ('U')
     *   ON CONFLICT ("name") DO
     *      UPDATE SET "count"=(words."count" + 1)
     *       WHERE words."name" IN ('A', 'E', 'I');  -- update 시 'A', 'E', 'I' 만 count를 증가시킨다.
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchUpsert with where`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in (TestDB.ALL_MYSQL_LIKE + upsertViaMergeDB) }

        withTables(testDB, Words) {
            val vowels = listOf("A", "E", "I", "O", "U")
            val alphabet = ('A'..'Z').map { it.toString() }
            val lettersWithDuplicates = alphabet + vowels

            val firstThreeVowels = vowels.take(3)

            Words.batchUpsert(
                lettersWithDuplicates,
                onUpdate = { it[Words.count] = Words.count + 1 },
                // PostgresNG throws IndexOutOfBound if shouldReturnGeneratedValues == true
                // Related issue in pgjdbc-ng repository: https://github.com/impossibl/pgjdbc-ng/issues/545
                shouldReturnGeneratedValues = false,
                where = { Words.word inList firstThreeVowels }
            ) { letter ->
                this[Words.word] = letter
            }

            // 중복된 것은 update 되기 때문에 alphabet 숫자만큼만 존재한다.
            Words.selectAll().count().toInt() shouldBeEqualTo alphabet.size

            Words.selectAll().forEach {
                val expectedCount = if (it[Words.word] in firstThreeVowels) 2 else 1
                it[Words.count] shouldBeEqualTo expectedCount
            }
        }
    }

    /**
     * [batchUpsert] 작업 중 INSERT 된 행의 수를 얻기
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Inserted Count With BatchUpsert`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_H2_V1 }

        withTables(testDB, AutoIncTable) {
            // SQL Server requires statements to be executed before results can be obtained
            val isNotSqlServer = true // testDB != TestDB.SQLSERVER
            val data = listOf(1 to "A", 2 to "B", 3 to "C")
            val newDataSize = data.size
            var statement: BatchUpsertStatement by Delegates.notNull()

            /**
             * all new rows inserted
             *
             * ```sql
             * -- Postgres
             * INSERT INTO auto_inc_table (id, "name") VALUES (1, 'A')
             *     ON CONFLICT (id) DO UPDATE SET "name"=EXCLUDED."name"
             * ```
             */
            AutoIncTable.batchUpsert(data, shouldReturnGeneratedValues = isNotSqlServer) { (id, name) ->
                statement = this
                this[AutoIncTable.id] = id
                this[AutoIncTable.name] = name
            }
            statement.insertedCount shouldBeEqualTo newDataSize  // insert 작업 수

            /**
             * 기존 행은 기존 컬럼 값으로 설정합니다.
             */
            val isH2MysqlMode = testDB in setOf(TestDB.H2_MYSQL, TestDB.H2_MARIADB)
            var expected = if (isH2MysqlMode) 0 else newDataSize

            AutoIncTable.batchUpsert(data, shouldReturnGeneratedValues = isNotSqlServer) { (id, name) ->
                statement = this
                this[AutoIncTable.id] = id
                this[AutoIncTable.name] = name
            }
            statement.insertedCount shouldBeEqualTo expected  // insert 작업 수

            /**
             * 이미 존재하는 행은 update 되고, 1개의 새로운 행이 추가된다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO auto_inc_table (id, "name")
             * VALUES (1, 'newA')
             * ON CONFLICT (id) DO
             *      UPDATE SET "name"=EXCLUDED."name"
             * ```
             * ```sql
             * -- Postgres
             * INSERT INTO auto_inc_table (id, "name")
             * VALUES (4, 'D')
             * ON CONFLICT (id) DO
             *      UPDATE SET "name"=EXCLUDED."name"
             * ```
             *
             */
            val updatedData = data.map { it.first to "new${it.second}" } + (4 to "D")
            expected = if (testDB in TestDB.ALL_MYSQL_LIKE) newDataSize * 2 + 1 else newDataSize + 1

            AutoIncTable.batchUpsert(updatedData, shouldReturnGeneratedValues = isNotSqlServer) { (id, name) ->
                statement = this
                this[AutoIncTable.id] = id
                this[AutoIncTable.name] = name
            }
            statement.insertedCount shouldBeEqualTo expected  // insert 작업 수

            AutoIncTable.selectAll().count() shouldBeEqualTo updatedData.size.toLong()
        }
    }

    /**
     * UUID 수형의 Primary Key 를 기준으로 [upsert] 작업 하기 (Postgres 전용)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `Upsert With UUID PrimaryKey`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }

        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS tester (
         *      id uuid PRIMARY KEY,
         *      test_key INT NOT NULL,
         *      test_value TEXT NOT NULL
         * );
         *
         * ALTER TABLE tester ADD CONSTRAINT tester_test_key_unique UNIQUE (test_key);
         * ```
         */
        val tester = object: UUIDTable("tester", "id") {
            val key = integer("test_key").uniqueIndex()
            val value = text("test_value")
        }

        /**
         * NOTE: 현재는 Postgres 만 올바른 UUID 를 직접 결과 집합에서 반환한다. 다른 데이터베이스에서는 'upsert' 명령에서 잘못된 ID 가 반환된다.
         */
        withTables(testDB, tester) {
            val insertId = tester.insertAndGetId {
                it[key] = 1
                it[value] = "one"
            }

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO upsert_test (id, test_key, test_value)
             * VALUES ('e8a41a28-f9d8-418d-aa48-d0603d10f44e', 1, 'two')
             * ON CONFLICT (test_key) DO
             *      UPDATE SET test_value=EXCLUDED.test_value  -- primary key인 id 는 업데이트 되지 않음
             * ```
             */
            val upsertId: EntityID<UUID> = tester.upsert(
                keys = arrayOf(tester.key),
                onUpdateExclude = listOf(tester.id),
            ) {
                it[key] = 1
                it[value] = "two"                    // value 가 "two" 로 업데이트 된다.
            }.resultedValues!!.first()[tester.id]

            upsertId shouldBeEqualTo insertId
            tester.selectAll()
                .where { tester.id eq insertId }
                .first()[tester.value] shouldBeEqualTo "two"
        }
    }

    /**
     * UUID 가 PrimaryKey 일 때, [batchUpsert] 사용하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `BatchUpsert With UUID PrimaryKey`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES }

        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS batch_upsert_test (
         *      id uuid PRIMARY KEY,
         *      test_key INT NOT NULL,
         *      test_value TEXT NOT NULL
         * );
         *
         * ALTER TABLE batch_upsert_test
         *   ADD CONSTRAINT batch_upsert_test_test_key_unique UNIQUE (test_key);
         * ```
         */
        val tester = object: UUIDTable("batch_upsert_test", "id") {
            val key = integer("test_key").uniqueIndex()
            val value = text("test_value")
        }

        withTables(testDB, tester) {
            val insertId = tester.insertAndGetId {
                it[key] = 1
                it[value] = "one"
            }

            /**
             * `batchUpdate` 시에 conflict를 판단하는 컬럼을 `tester.key`로 하고, Update 작업 시에는 `tester.id` 는 제외한다.
             *
             * ```sql
             * -- Postgres
             * INSERT INTO batch_upsert_test (test_key, test_value, id)
             * VALUES (1, 'two', '6b594a66-dc19-4f9e-a3f1-2349eead4548')
             *     ON CONFLICT (test_key) DO
             *     UPDATE SET test_value=EXCLUDED.test_value
             * ```
             */
            val upsertId: EntityID<UUID> = tester.batchUpsert(
                data = listOf(1 to "two"),
                keys = arrayOf(tester.key),
                onUpdateExclude = listOf(tester.id),
            ) {
                this[tester.key] = it.first
                this[tester.value] = it.second
            }.first()[tester.id]

            upsertId shouldBeEqualTo insertId
            tester.selectAll()
                .where { tester.id eq insertId }
                .first()[tester.value] shouldBeEqualTo "two"
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS auto_inc_table (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(64) NOT NULL
     * )
     * ```
     */
    private object AutoIncTable: Table("auto_inc_table") {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 64)

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS words (
     *      "name" VARCHAR(64) NOT NULL,
     *      "count" INT DEFAULT 1 NOT NULL
     * );
     *
     * ALTER TABLE words ADD CONSTRAINT words_name_unique UNIQUE ("name");
     * ```
     */
    private object Words: Table("words") {
        val word = varchar("name", 64).uniqueIndex()
        val count = integer("count").default(1)
    }
}
