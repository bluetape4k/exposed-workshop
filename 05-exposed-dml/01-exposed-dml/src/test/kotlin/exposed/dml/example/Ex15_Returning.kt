package exposed.dml.example

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.times
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteReturning
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.ReturningStatement
import org.jetbrains.exposed.sql.updateReturning
import org.jetbrains.exposed.sql.upsertReturning
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * [insertReturning], [upsertReturning], [deleteReturning], [updateReturning] 함수를 사용하는 예제입니다.
 *
 * 단, 이 함수들은 Postgres, MariaDB 에서만 지원합니다.
 */
class Ex15_Returning: AbstractExposedTest() {

    companion object: KLogging()

    private val updateReturningSupportedDb = TestDB.ALL_POSTGRES
    private val returningSupportedDb = updateReturningSupportedDb + TestDB.ALL_MARIADB

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS items (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(32) NOT NULL,
     *      price DOUBLE PRECISION NOT NULL
     * )
     * ```
     */
    object Items: IntIdTable("items") {
        val name = varchar("name", 32)
        val price = double("price")
    }

    class Item(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Item>(Items)

        var name by Items.name
        var price by Items.price

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("price", price)
            .toString()
    }

    /**
     * [insertReturning] 은 행을 추가하고, 추가된 행 ([ResultRow])을 반환합니다.
     *
     * 참고: [insertReturning] 은 Postgres, MariaDB 에서만 지원
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert returning`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in returningSupportedDb }

        withTables(testDB, Items) {
            /**
             * 기본적으로 테이블의 모든 컬럼을 반환한다.
             * ```sql
             * INSERT INTO items ("name", price) VALUES ('A', 99.0)
             * RETURNING items.id, items."name", items.price
             * ```
             */
            val result1 = Items.insertReturning {
                it[name] = "A"
                it[price] = 99.0
            }.single()

            result1[Items.id].value shouldBeEqualTo 1
            result1[Items.name] shouldBeEqualTo "A"
            result1[Items.price] shouldBeEqualTo 99.0

            /**
             * 특정 컬럼만 반환하도록 지정
             * ```sql
             * INSERT INTO items ("name", price) VALUES ('B', 200.0)
             * RETURNING items.id, items."name"
             * ```
             */
            val result2 = Items.insertReturning(listOf(Items.id, Items.name)) {
                it[name] = "B"
                it[price] = 200.0
            }.single()

            result2[Items.id].value shouldBeEqualTo 2
            result2[Items.name] shouldBeEqualTo "B"
            assertFailsWith<IllegalStateException> { // Items.price not in record set
                result2[Items.price]
            }

            Items.selectAll().count().toInt() shouldBeEqualTo 2
        }
    }

    /**
     * [insertReturning] 시 예외를 무시할 수 있는 옵션 사용 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert ignore returning`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in returningSupportedDb }

        val tester = object: Table("tester") {
            val item = varchar("item", 21).uniqueIndex()
        }

        withTables(testDB, tester) {
            tester.insert {
                it[item] = "Item A"
            }
            tester.selectAll().count().toInt() shouldBeEqualTo 1

            /**
             * Unique index 위배로 insert 무시 (ignoreErrors = true)
             *
             * ```sql
             * INSERT INTO tester (item) VALUES ('Item A')
             * ON CONFLICT DO NOTHING
             * RETURNING tester.item
             * ```
             */
            val resultWithConflict = tester.insertReturning(ignoreErrors = true) {
                it[item] = "Item A"
            }.singleOrNull()

            resultWithConflict.shouldBeNull()
            tester.selectAll().count().toInt() shouldBeEqualTo 1

            /**
             * Unique index 위배가 없으므로, INSERT 된다. (ignoreErrors = true)
             *
             * ```sql
             * INSERT INTO tester (item) VALUES ('Item B')
             * ON CONFLICT DO NOTHING
             * RETURNING tester.item
             * ```
             */
            val resultWithoutConflict = tester.insertReturning(ignoreErrors = true) {
                it[item] = "Item B"
            }.single()

            resultWithoutConflict[tester.item] shouldBeEqualTo "Item B"
            tester.selectAll().count().toInt() shouldBeEqualTo 2
        }
    }

    /**
     * [upsertReturning] 을 통해, 작업된 행을 반환하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert returing`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in returningSupportedDb }

        withTables(testDB, Items) {
            /**
             * 기본적으로 작업된 행의 모든 컬럼을 [ResultRow]로 반환합니다.
             *
             * ```sql
             * -- Postgres
             *  INSERT INTO items ("name", price) VALUES ('A', 99.0)
             *  ON CONFLICT (id) DO
             *      UPDATE SET "name"=EXCLUDED."name",
             *                 price=EXCLUDED.price
             *  RETURNING items.id, items."name", items.price
             *  ```
             */
            val result1 = Items.upsertReturning {
                it[name] = "A"
                it[price] = 99.0
            }.single()

            result1[Items.id].value shouldBeEqualTo 1
            result1[Items.name] shouldBeEqualTo "A"
            result1[Items.price] shouldBeEqualTo 99.0

            /**
             * 특정 컬럼만 지정해서 반환 받을 수 있습니다. (returning)
             *
             * insert 시에는 price=200.0으로, update 시에는 price=items.price * 10.0으로 변경
             *
             * ```sql
             * INSERT INTO items (id, "name", price) VALUES (1, 'B', 200.0)
             * ON CONFLICT (id) DO
             *      UPDATE SET price=(items.price * 10.0)
             * RETURNING items."name", items.price
             * ```
             */
            val result2 = Items.upsertReturning(
                returning = listOf(Items.name, Items.price),
                onUpdate = { it[Items.price] = Items.price * 10.0 }
            ) {
                it[id] = 1
                it[name] = "B"
                it[price] = 200.0
            }.single()

            result2[Items.name] shouldBeEqualTo "A"
            result2[Items.price] shouldBeEqualTo 990.0

            if (testDB != TestDB.MARIADB) {
                /**
                 * `returning` 을 사용하여 `Items.name` 컬럼만 반환합니다.
                 *
                 * `onUpdateExclude` 를 사용하여, update 시에 price 컬럼을 제외하고 업데이트
                 *
                 * ```sql
                 * INSERT INTO items (id, "name", price) VALUES (1, 'B', 200.0)
                 * ON CONFLICT (id) DO
                 *      UPDATE SET "name"=EXCLUDED."name"
                 *       WHERE items.price > 500.0
                 * RETURNING items."name"
                 * ```
                 *
                 */
                val result3 = Items.upsertReturning(
                    returning = listOf(Items.name),
                    onUpdateExclude = listOf(Items.price),
                    where = { Items.price greater 500.0 }
                ) {
                    it[id] = 1
                    it[name] = "B"
                    it[price] = 200.0
                }.single()

                result3[Items.name] shouldBeEqualTo "B"
            }

            Items.selectAll().count().toInt() shouldBeEqualTo 1
        }
    }

    /**
     * `upsertReturning` 을 사용하여 DAO를 반환하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `upsert returning with DAO`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in returningSupportedDb }

        withTables(testDB, Items) {
            /**
             * 기본 사용 시에는 모든 컬럼을 반환합니다. `wrapRow`를 사용하여 [ResultRow]를 엔티티로 변환합니다.
             *
             * ```sql
             * INSERT INTO items ("name", price) VALUES ('A', 99.0)
             * ON CONFLICT (id) DO
             *      UPDATE SET "name"=EXCLUDED."name",
             *                 price=EXCLUDED.price
             * RETURNING items.id, items."name", items.price
             * ```
             */
            val result1: Item = Items.upsertReturning {
                it[name] = "A"
                it[price] = 99.0
            }.let {
                Item.wrapRow(it.single())
            }

            result1.id.value shouldBeEqualTo 1
            result1.name shouldBeEqualTo "A"
            result1.price shouldBeEqualTo 99.0

            /**
             * ID가 1인 레코드가 이미 존재하므로, 해당 레코드만 업데이트하고, 업데이트된 레코드를 반환합니다.
             *
             * ```sql
             * INSERT INTO items (id, "name", price) VALUES (1, 'B', 200.0)
             * ON CONFLICT (id) DO
             *      UPDATE SET "name"=EXCLUDED."name",
             *                 price=EXCLUDED.price
             * RETURNING items.id, items."name", items.price
             * ```
             */
            val result2: Item = Items.upsertReturning {
                it[id] = 1
                it[name] = "B"
                it[price] = 200.0
            }.let {
                Item.wrapRow(it.single())
            }
            result2.id.value shouldBeEqualTo 1
            result2.name shouldBeEqualTo "B"
            result2.price shouldBeEqualTo 200.0

            Items.selectAll().count().toInt() shouldBeEqualTo 1
            Item.all().count().toInt() shouldBeEqualTo 1
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `returning with no results`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in returningSupportedDb }

        withTables(testDB, Items) {
            // 실행이 되려면, `singleOrNull()` 또는 `single()`을 호출해야 합니다.
            // 실행이 되지 않았으므로 `ReturningStatement`가 반환됩니다.
            val stmt: ReturningStatement = Items.insertReturning {
                it[name] = "A"
                it[price] = 99.0
            } //.singleOrNull()
            assertIs<ReturningStatement>(stmt)

            Items.selectAll().count() shouldBeEqualTo 0L

            // 삭제될 것이 없으므로, 삭제된 레코드가 없습니다.
            Items.deleteReturning().toList().shouldBeEmpty()
        }
    }

    /**
     * [deleteReturning] 은 삭제된 행을 반환합니다.
     *
     * `DELETE ... RETURNING` 구문은 Postgres, SQLite 에서만 지원
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete returning`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in returningSupportedDb }

        withTables(testDB, Items) {
            Items.batchInsert(listOf("A" to 99.0, "B" to 100.0, "C" to 200.0)) { (n, p) ->
                this[Items.name] = n
                this[Items.price] = p
            }
            Items.selectAll().count() shouldBeEqualTo 3L

            /**
             * price가 200.0인 레코드를 삭제하고, 삭제된 행을 반환합니다.
             *
             * ```sql
             * -- Postgres
             * DELETE FROM items
             *  WHERE items.price = 200.0
             * RETURNING items.id, items."name", items.price
             * ```
             */
            val result1: ResultRow = Items.deleteReturning(where = { Items.price eq 200.0 }).single()
            result1[Items.id].value shouldBeEqualTo 3
            result1[Items.name] shouldBeEqualTo "C"
            result1[Items.price] shouldBeEqualTo 200.0

            Items.selectAll().count() shouldBeEqualTo 2L

            /**
             * 모든 Items 행을 삭제하고, 삭제된 행의 id를 반환합니다.
             *
             * ```sql
             * -- Postgres
             * DELETE FROM items RETURNING items.id
             * ```
             */
            val result2 = Items.deleteReturning(listOf(Items.id)).map { it[Items.id].value }
            result2 shouldBeEqualTo listOf(1, 2)

            Items.selectAll().count() shouldBeEqualTo 0L
        }
    }

    /**
     * [updateReturning] 은 업데이트된 행을 반환합니다.
     *
     * `UPDATE ... RETURNING` 구문은 Postgres, SQLite 에서만 지원
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update returning`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in updateReturningSupportedDb }

        withTables(testDB, Items) {
            val input = listOf("A" to 99.0, "B" to 100.0, "C" to 200.0)
            Items.batchInsert(input) { (n, p) ->
                this[Items.name] = n
                this[Items.price] = p
            }

            /**
             * price가 99.0 이하인 레코드의 price를 10배로 업데이트하고, 업데이트된 행을 반환합니다.
             *
             * ```sql
             * UPDATE items
             *    SET price=(items.price * 10.0)
             *  WHERE items.price <= 99.0
             * RETURNING items.id, items."name", items.price
             * ```
             */
            val result1: ResultRow = Items
                .updateReturning(where = { Items.price lessEq 99.0 }) {
                    it[price] = price * 10.0
                }
                .single()

            result1[Items.id].value shouldBeEqualTo 1
            result1[Items.name] shouldBeEqualTo "A"
            result1[Items.price] shouldBeEqualTo 990.0

            /**
             * 모든 레코드의 name을 소문자로 변경하고, 변경된 name을 반환합니다.
             *
             * ```sql
             * UPDATE items
             *    SET "name"=LOWER(items."name")
             * RETURNING items."name"
             * ```
             */
            val result2: List<String> = Items
                .updateReturning(listOf(Items.name)) {
                    it[name] = name.lowerCase()
                }
                .map { it[Items.name] }
            result2.toSet() shouldBeEqualTo input.map { it.first.lowercase() }.toSet()

            /**
             * 모든 레코드의 price를 `0.0`으로 변경하고, price 컬럼의 alias 만 반환합니다.
             *
             * ```sql
             * UPDATE items
             *    SET price=0.0
             * RETURNING new_price
             * ```
             */
            val newPrice = Items.price.alias("new_price")
            val result3: List<Double> = Items
                .updateReturning(listOf(newPrice)) {
                    it[price] = 0.0
                }
                .map { it[newPrice] }

            result3 shouldHaveSize 3
            result3.all { it == 0.0 }.shouldBeTrue()

            Items.selectAll().count() shouldBeEqualTo 3L
        }
    }
}
