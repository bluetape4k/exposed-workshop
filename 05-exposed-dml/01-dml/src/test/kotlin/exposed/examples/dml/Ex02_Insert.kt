package exposed.examples.dml

import exposed.shared.dml.DMLTestData
import exposed.shared.dml.DMLTestData.Cities
import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.assertFailAndRollback
import exposed.shared.tests.inProperCase
import exposed.shared.tests.withDb
import exposed.shared.tests.withSuspendedDb
import exposed.shared.tests.withTables
import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.exposed.sql.BatchInsertOnConflictDoNothing
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.fail
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.UUIDColumnType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.substring
import org.jetbrains.exposed.v1.core.trim
import org.jetbrains.exposed.v1.core.wrapAsExpression
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.insertIgnoreAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.BatchInsertBlockingExecutable
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.*

class Ex02_Insert: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * [insertAndGetId] 는 [IntIdTable] 처럼 entityID를 가지는 테이블에 대해서 사용할 수 있습니다.
     *
     * ```sql
     * INSERT INTO TMP (FOO) VALUES ('name-1')
     * ```
     *
     * ```sql
     * INSERT INTO TMP (FOO) VALUES ('name-2')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and get id 01`(testDB: TestDB) {
        val idTable = object: IntIdTable("tmp") {
            val name = varchar("foo", 10).uniqueIndex()
        }

        withTables(testDB, idTable) {
            val id1 = idTable.insertAndGetId { it[name] = "name-1" }
            idTable.selectAll().count() shouldBeEqualTo 1L
            id1.value shouldBeEqualTo 1

            val id2 = idTable.insertAndGetId { it[name] = "name-2" }
            idTable.selectAll().count() shouldBeEqualTo 2L
            id2.value shouldBeEqualTo 2

            val id3 = idTable.insert {
                it[name] = "name-3"
            } get idTable.id

            val id4 = idTable.insert {
                it[name] = "name-4"
            }[idTable.id]

            assertFailAndRollback("Unique constraint failed") {
                idTable.insertAndGetId { it[name] = "name-1" }
            }
        }
    }


    /**
     * [insertIgnoreAndGetId] 는 `INSERT` 시 예외가 발생하면, 아무 일도 하지 않고,
     * 예외가 발생하지 않으면, `INSERT` 된 ROW의 ID를 반환합니다.
     *
     * ```sql
     * -- Postgres
     * INSERT INTO tmp (foo) VALUES ('1') ON CONFLICT DO NOTHING;
     * INSERT INTO tmp (foo) VALUES ('2') ON CONFLICT DO NOTHING;
     *
     * -- 예외가 발생하므로 아무 일도 하지 않습니다.
     * INSERT INTO tmp (id, foo) VALUES (100, '2') ON CONFLICT DO NOTHING;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert ignore and get id 01`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES_LIKE + TestDB.MYSQL_V8 }

        val idTable = object: IntIdTable("tmp") {
            val name = varchar("foo", 10).uniqueIndex()
        }

        withTables(testDB, idTable) {
            // INSERT INTO tmp (foo) VALUES ('1') ON CONFLICT DO NOTHING
            val id1 = idTable.insertIgnoreAndGetId { it[name] = "1" }
            idTable.selectAll().count() shouldBeEqualTo 1L
            id1!!.value shouldBeEqualTo 1

            // INSERT INTO tmp (foo) VALUES ('2') ON CONFLICT DO NOTHING
            val id2 = idTable.insertIgnoreAndGetId { it[name] = "2" }
            idTable.selectAll().count() shouldBeEqualTo 2L
            id2!!.value shouldBeEqualTo 2

            // INSERT INTO tmp (foo) VALUES ('2') ON CONFLICT DO NOTHING
            val idNull = idTable.insertIgnoreAndGetId { it[name] = "2" }
            idTable.selectAll().count() shouldBeEqualTo 2L
            idNull.shouldBeNull()

            // INSERT INTO tmp (id, foo) VALUES (100, '2') ON CONFLICT DO NOTHING
            val shouldNotReturnProvidedIdOnConflict = idTable.insertIgnoreAndGetId {
                it[idTable.id] = EntityID(100, idTable)
                it[idTable.name] = "2"
            }
            idTable.selectAll().count() shouldBeEqualTo 2L
            shouldNotReturnProvidedIdOnConflict.shouldBeNull()
        }
    }

    /**
     * [insertAndGetId] 는 다른 컬럼이라도 `id`에 해당하는 [EntityID]로 지정할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and get id when column has different name and get value by id column`(testDB: TestDB) {
        /**
         * code 컬럼은 Identity 컬럼으로도 사용된다.
         *
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS testtablewithid (code INT NOT NULL)
         * ```
         */
        val testTableWithId = object: IdTable<Int>("testTableWithId") {
            val code = integer("code")
            override val id: Column<EntityID<Int>> = code.entityId()
        }

        withTables(testDB, testTableWithId) {
            // INSERT INTO testtablewithid (code) VALUES (1)
            val id1 = testTableWithId.insertAndGetId {
                it[code] = 1
            }
            id1.shouldNotBeNull()
            id1.value shouldBeEqualTo 1

            // INSERT INTO testtablewithid (code) VALUES (2)
            val id2 = testTableWithId.insert {
                it[code] = 2
            } get testTableWithId.id

            id2.shouldNotBeNull()
            id2.value shouldBeEqualTo 2
        }
    }

    /**
     * ID 컬럼 명이 다른 경우에도, ID 컬럼을 통해 INSERT, SELECT 가 가능해야 합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_id_and_column_table (
     *      example_column VARCHAR(200) NOT NULL
     * );
     *
     * INSERT INTO test_id_and_column_table (example_column)
     * VALUES ('Mkf9JW');
     *
     * SELECT test_id_and_column_table.example_column
     *   FROM test_id_and_column_table
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `id and column have different names and get value by original column`(testDB: TestDB) {
        val exampleTable = object: IdTable<String>("test_id_and_column_table") {
            val exampleColumn = varchar("example_column", 200)
            override val id: Column<EntityID<String>> = exampleColumn.entityId()
        }

        withTables(testDB, exampleTable) {
            val value = Base58.randomString(6)

            exampleTable.insert {
                it[exampleColumn] = value
            }

            val resultValue = exampleTable.selectAll().map { it[exampleTable.exampleColumn] }.single()
            resultValue shouldBeEqualTo value
        }
    }

    /**
     * [insertIgnore] 는 INSERT 시 예외가 발생하면, 예외를 무시합니다.
     *
     * ```sql
     * -- MySQL V8
     * INSERT IGNORE INTO tmp (id, foo) VALUES (1, '1');
     * INSERT IGNORE INTO tmp (id, foo) VALUES (1, '2');
     * ```
     *
     * ```sql
     * -- Postgres
     * INSERT INTO tmp (id, foo) VALUES (1, '1') ON CONFLICT DO NOTHING;
     * INSERT INTO tmp (id, foo) VALUES (1, '2') ON CONFLICT DO NOTHING;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insertIgnore with predefined id`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_POSTGRES_LIKE + TestDB.H2_MYSQL + TestDB.MYSQL_V8 }

        val idTable = object: IntIdTable("tmp") {
            val name = varchar("foo", 10).uniqueIndex()
        }

        withTables(testDB, idTable) {
            // ID가 중복되지 않아서, INSERT가 수행됩니다.
            val insertedStatement: InsertStatement<Long> = idTable.insertIgnore {
                it[idTable.id] = EntityID(1, idTable)
                it[idTable.name] = "1"
            }
            insertedStatement[idTable.id].value shouldBeEqualTo 1
            insertedStatement.insertedCount shouldBeEqualTo 1

            // ID가 중복되었다는 예외가 발생, 에외는 무시되고, INSERT는 취소됩니다.
            val notInsertedStatement: InsertStatement<Long> = idTable.insertIgnore {
                it[idTable.id] = EntityID(1, idTable)
                it[idTable.name] = "2"
            }
            notInsertedStatement[idTable.id].value shouldBeEqualTo 1
            notInsertedStatement.insertedCount shouldBeEqualTo 0
        }
    }

    /**
     * [batchInsert] 는 여러 개의 ROW를 한 번에 INSERT 작업을 수행합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val cityNames = listOf("Paris", "Moscow", "Helsinki")

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO cities ("name") VALUES ('Paris');
             * INSERT INTO cities ("name") VALUES ('Moscow');
             * INSERT INTO cities ("name") VALUES ('Helsinki');
             * ```
             */
            val allCitiesIDs: List<ResultRow> = cities.batchInsert(cityNames) { name ->
                this[cities.name] = name
            }
            allCitiesIDs shouldHaveSize cityNames.size

            val userNamesWithCityIds = allCitiesIDs.mapIndexed { index, rows ->
                "UserFrom${cityNames[index]}" to rows[cities.id] as Number
            }

            /**
             * ```sql
             * -- Postgres
             * INSERT INTO users (id, "name", city_id) VALUES ('mdhXRY', 'UserFromParis', 4)
             * INSERT INTO users (id, "name", city_id) VALUES ('c5vVGN', 'UserFromMoscow', 5)
             * INSERT INTO users (id, "name", city_id) VALUES ('niR8mW', 'UserFromHelsinki', 6)
             * ```
             */
            val generatedIds: List<ResultRow> = users.batchInsert(userNamesWithCityIds) { (userName, cityId) ->
                this[users.id] = Base58.randomString(6)
                this[users.name] = userName
                this[users.cityId] = cityId.toInt()
            }
            generatedIds shouldHaveSize userNamesWithCityIds.size

            users.selectAll()
                .where { users.name inList userNamesWithCityIds.map { it.first } }
                .count() shouldBeEqualTo userNamesWithCityIds.size.toLong()
        }
    }

    /**
     * [Sequence] 를 사용하여 데이터를 BATCH INSERT 합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert with sequence`(testDB: TestDB) {
        val cities = DMLTestData.Cities

        withTables(testDB, cities) {
            val batchSize = 100
            val names = generateSequence {
                TimebasedUuid.Reordered.nextIdAsString()
            }.take(batchSize)

            val inserted = cities.batchInsert(names) { name ->
                this[cities.name] = name
            }
            inserted shouldHaveSize batchSize

            cities.selectAll().count() shouldBeEqualTo batchSize.toLong()
        }
    }

    /**
     * Insert 후 생성된 ID를 반환합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS cities (
     *      city_id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL
     * );
     * ```
     * ```sql
     * INSERT INTO cities ("name") VALUES ('FooCity')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and get generted key 01`(testDB: TestDB) {

        withTables(testDB, Cities) {
            val id: Int = Cities.insert {
                it[name] = "FooCity"
            } get Cities.id  // [Cities.id]로 표현할 수 있다.

            Cities.selectAll().last()[Cities.id] shouldBeEqualTo id
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_longid (
     *      id BIGSERIAL PRIMARY KEY,
     *      "name" TEXT NOT NULL
     * )
     * ```
     */
    object TestLongIdTable: Table("test_longid") {
        val id = long("id").autoIncrement()
        val name = text("name")

        override val primaryKey = PrimaryKey(id)
    }

    /**
     * Insert 후 생성된 ID를 반환합니다.
     *
     * ```sql
     * -- Postgres
     * INSERT INTO test_longid ("name") VALUES ('Foo')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and get generated key 02`(testDB: TestDB) {
        withTables(testDB, TestLongIdTable) {
            val id: Long = TestLongIdTable.insert {
                it[name] = "Foo"
            } get TestLongIdTable.id

            TestLongIdTable.selectAll().last()[TestLongIdTable.id] shouldBeEqualTo id
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test_intid (
     *      id SERIAL PRIMARY KEY,
     *      "name" TEXT NOT NULL
     * )
     * ```
     */
    object IntIdTestTable: IntIdTable("test_intid") {
        val name = text("name")
    }

    /**
     * [insertAndGetId] 는 insert 시 생성된 [EntityID] 값을 반환합니다.
     *
     * ```sql
     * -- Postgres
     * INSERT INTO test_intid ("name") VALUES ('Foo')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert and get generated key 03`(testDB: TestDB) {
        withTables(testDB, IntIdTestTable) {
            val id: EntityID<Int> = IntIdTestTable.insertAndGetId {
                it[name] = "Foo"
            }

            val loadedId = IntIdTestTable.selectAll().last()[IntIdTestTable.id]
            loadedId shouldBeEqualTo id
            loadedId.value shouldBeEqualTo id.value
            loadedId.table shouldBeEqualTo id.table
        }
    }


    /**
     * 사용자가 제공하는 [EntityID] 값을 사용하여 데이터를 삽입합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS stringtable (
     *      id VARCHAR(15) NOT NULL,
     *      "name" TEXT NOT NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with predefined id`(testDB: TestDB) {

        val stringTable = object: IdTable<String>("stringTable") {
            override val id: Column<EntityID<String>> = varchar("id", 15).entityId()
            val name = text("name")
        }
        withTables(testDB, stringTable) {

            // INSERT INTO stringtable (id, "name") VALUES ('id1', 'Foo')
            val entityID = EntityID("id1", stringTable)
            val id1 = stringTable.insertAndGetId {
                it[stringTable.id] = entityID
                it[stringTable.name] = "Foo"
            }
            id1 shouldBeEqualTo entityID

            // INSERT INTO stringtable (id, "name") VALUES ('testId', 'Bar')
            val entityID2 = EntityID("testId", stringTable)
            val id2 = stringTable.insertAndGetId {
                it[stringTable.id] = entityID2
                it[stringTable.name] = "Bar"
            }
            id2 shouldBeEqualTo entityID2

            val row1 = stringTable.selectAll().where { stringTable.id eq entityID }.singleOrNull()
            row1?.get(stringTable.id) shouldBeEqualTo entityID

            val row2 = stringTable.selectAll().where { stringTable.id like "test%" }.singleOrNull()
            row2?.get(stringTable.id) shouldBeEqualTo entityID2
        }
    }

    /**
     * Foreign key 를 가지는 테이블에 [EntityID]를 삽입합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS idtable (id SERIAL PRIMARY KEY);
     *
     * CREATE TABLE IF NOT EXISTS standardtable (
     *      "externalId" INT NOT NULL,
     *
     *      CONSTRAINT fk_standardtable_externalid__id FOREIGN KEY ("externalId") REFERENCES idtable(id)
     *      ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * INSERT INTO idtable  DEFAULT VALUES;
     * INSERT INTO standardtable ("externalId") VALUES (1);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with foreign id`(testDB: TestDB) {
        val idTable = object: IntIdTable("idTable") {}
        val standardTable = object: Table("standardTable") {
            val externalId: Column<EntityID<Int>> = reference("externalId", idTable.id)
        }
        withTables(testDB, idTable, standardTable) {
            val id1: EntityID<Int> = idTable.insertAndGetId {}
            standardTable.insert {
                it[externalId] = id1
            }

            val externalIdColumn = standardTable.externalId
            val rows = standardTable.select(externalIdColumn).map { it[externalIdColumn] }
            rows shouldBeEqualTo listOf(id1)
        }
    }

    /**
     * 컬럼 값을 DB의 함수들을 이용한 Expression 으로 삽입합니다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with expression`(testDB: TestDB) {
        val tbl = object: IntIdTable("testInsert") {
            val nullableInt = integer("nullableInt").nullable()
            val string = varchar("stringCol", 255)
        }

        fun expression(value: String) = stringLiteral(value).trim().substring(2, 4)

        fun verify(value: String) {
            val row = tbl.selectAll().where { tbl.string eq value }.single()
            row[tbl.string] shouldBeEqualTo value
        }

        withTables(testDB, tbl) {
            /**
             * ```sql
             * INSERT INTO testinsert ("stringCol") VALUES (SUBSTRING(TRIM(' _exp1_ '), 2, 4))
             * ```
             */
            tbl.insert {
                it[string] = expression(" _exp1_ ")  // SUBSTRING(TRIM(' _exp1_ '), 2, 4)
            }
            verify("exp1")

            /**
             * ```sql
             * INSERT INTO testinsert ("stringCol", "nullableInt") VALUES (SUBSTRING(TRIM(' _exp2_ '), 2, 4), 5)
             * ```
             */
            tbl.insert {
                it[string] = expression(" _exp2_ ")  // SUBSTRING(TRIM(' _exp2_ '), 2, 4)
                it[nullableInt] = 5
            }
            verify("exp2")

            /**
             * ```sql
             * INSERT INTO testinsert ("stringCol", "nullableInt") VALUES (SUBSTRING(TRIM(' _exp3_ '), 2, 4), NULL)
             * ```
             */
            tbl.insert {
                it[string] = expression(" _exp3_ ")  // SUBSTRING(TRIM(' _exp3_ '), 2, 4)
                it[nullableInt] = null
            }
            verify("exp3")
        }
    }

    /**
     * [wrapAsExpression]을 이용하여, 특정 쿼리의 결과를 Expression으로 컬럼 값으로 지정할 수 있습니다.
     *
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS testinsert1 (
     *      id SERIAL PRIMARY KEY,
     *      "stringCol" VARCHAR(20) NOT NULL
     * )
     * ```
     * ```sql
     * CREATE TABLE IF NOT EXISTS testinsert2 (
     *      id SERIAL PRIMARY KEY,
     *      "stringCol" VARCHAR(20) NULL
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with column expression`(testDB: TestDB) {
        val tbl1 = object: IntIdTable("testInsert1") {
            val string1 = varchar("stringCol", 20)
        }
        val tbl2 = object: IntIdTable("testInsert2") {
            val string2 = varchar("stringCol", 20).nullable()
        }

        fun verify(value: String) {
            val row = tbl2.selectAll().where { tbl2.string2 eq value }.single()
            row[tbl2.string2] shouldBeEqualTo value
        }

        withTables(testDB, tbl1, tbl2) {
            /**
             * ```sql
             * INSERT INTO testinsert1 ("stringCol") VALUES ('_exp1_')
             * ```
             */
            val id = tbl1.insertAndGetId {
                it[string1] = "_exp1_"
            }

            /**
             * 쿼리 결과물을 [wrapAsExpression]을 통해 Expression으로 변환하여 사용합니다.
             * ```sql
             * INSERT INTO testinsert2 ("stringCol")
             * VALUES ((SELECT SUBSTRING(TRIM(testinsert1."stringCol"), 2, 4)
             *            FROM testinsert1
             *           WHERE testinsert1.id = 1))
             * ```
             */
            val expr1 = tbl1.string1.trim().substring(2, 4)
            tbl2.insert {
                it[string2] = wrapAsExpression(tbl1.select(expr1).where { tbl1.id eq id })
            }
            verify("exp1")
        }
    }


    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS ordereddata (
     *      id SERIAL PRIMARY KEY,
     *      "name" TEXT NOT NULL,
     *      "order" INT DEFAULT 0 NOT NULL
     * )
     * ```
     */
    private object OrderedDataTable: IntIdTable() {
        val name = text("name")
        val order = integer("order").default(0)
    }

    class OrderedData(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<OrderedData>(OrderedDataTable)

        var name by OrderedDataTable.name
        var order by OrderedDataTable.order

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("name", name)
                .add("order", order)
                .toString()
    }

    /**
     * DAO 를 사용하여 INSERT 합니다.
     *
     * ```sql
     * INSERT INTO ordereddata ("name", "order") VALUES ('foo', 20);
     * INSERT INTO ordereddata ("name", "order") VALUES ('bar', 10);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert with column named with keyword`(testDB: TestDB) {
        withTables(testDB, OrderedDataTable) {
            val foo = OrderedData.new {
                name = "foo"
                order = 20
            }
            val bar = OrderedData.new {
                name = "bar"
                order = 10
            }

            val orders: List<OrderedData> = OrderedData.all()
                .orderBy(OrderedDataTable.order to SortOrder.ASC)
                .toList()

            orders shouldBeEqualTo listOf(bar, foo)
        }
    }


    /**
     * Subquery 결과 값을 이용하여 INSERT, UPDATE 하기
     *
     * Insert using subquery in Postgres:
     * ```sql
     * INSERT INTO tab1 (id)
     * VALUES ((SELECT tab2.id
     *            FROM tab2
     *           WHERE tab2.id = 'foo'))
     * ```
     *
     * Update using subquery in Postgres:
     * ```sql
     * UPDATE tab1
     *    SET id=(SELECT tab2.id
     *              FROM tab2
     *             WHERE tab2.id = 'bar')
     *  WHERE tab1.id = 'foo'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `subquery in an insert or update statement`(testDB: TestDB) {
        val tbl1 = object: Table("tab1") {
            val id = varchar("id", 10)
        }
        val tbl2 = object: Table("tab2") {
            val id = varchar("id", 10)
        }

        withTables(testDB, tbl1, tbl2) {
            // Initial data
            tbl2.insert { it[id] = "foo" }
            tbl2.insert { it[id] = "bar" }

            // Insert using subquery
            tbl1.insert {
                it[id] = tbl2.select(tbl2.id).where { tbl2.id eq "foo" }
            }
            tbl1.select(tbl1.id).single()[tbl1.id] shouldBeEqualTo "foo"

            // Update using subquery
            tbl1.update({ tbl1.id eq "foo" }) {
                it[id] = tbl2.select(tbl2.id).where { tbl2.id eq "bar" }
            }
            tbl1.select(tbl1.id).single()[tbl1.id] shouldBeEqualTo "bar"
        }
    }

    /**
     * `clientDefault` 를 이용하여, 클라이언트에서 생성된 값을 사용하여 INSERT 합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS charidtable (
     *      id VARCHAR(50) PRIMARY KEY,
     *      foo INT NOT NULL
     * )
     * ```
     * ```sql
     * -- Postgres
     * INSERT INTO charidtable (id, foo) VALUES ('XfwhfY', 42)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `generated key 04`(testDB: TestDB) {
        val charIdTable = object: IdTable<String>("charIdTable") {
            override val id = varchar("id", 50)
                .clientDefault { Base58.randomString(24) }
                .entityId()

            val foo = integer("foo")

            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, charIdTable) {
            val id = charIdTable.insertAndGetId {
                it[foo] = 42
            }

            id.value.shouldNotBeNull()
            log.debug { "id=${id.value}" }
        }
    }

    private fun rollbackSupportDbs() =
        TestDB.ALL_H2 + TestDB.ALL_MYSQL_MARIADB + TestDB.ALL_POSTGRES - TestDB.MYSQL_V5

    /**
     * 일반적인 트랜잭션에서 Constraint 예외가 발생하면, 해당 트랜잭션은 Rollback 되어야 합니다.
     *
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS testrollback (
     *      id SERIAL PRIMARY KEY,
     *      foo INT NOT NULL,
     *
     *      CONSTRAINT check_TestRollback_0 CHECK (foo > 0)
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `rollback on constraint exception with normal transactions`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in rollbackSupportDbs() }

        val testTable = object: IntIdTable("TestRollback") {
            val foo = integer("foo").check { it greater 0 }
        }
        try {
            try {
                withDb(testDB) {
                    SchemaUtils.create(testTable)
                    testTable.insert { it[foo] = 1 }
                    testTable.insert { it[foo] = 0 }    // foo > 0 조건을 만족하지 않음 (예외 발생)
                }
                fail("예외가 발생해서 Rollback 이 수행되어야 합니다.")
            } catch (e: Throwable) {
                // Nothing to do
            }

            withDb(testDB) {
                testTable.selectAll().empty().shouldBeTrue()
            }
        } finally {
            withDb(testDB) {
                SchemaUtils.drop(testTable)
            }
        }
    }

    /**
     * Suspend 트랜잭션에서 Constraint 예외가 발생하면, 해당 트랜잭션은 Rollback 되어야 합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS testrollback (
     *      id SERIAL PRIMARY KEY,
     *      foo INT NOT NULL,
     *
     *      CONSTRAINT check_TestRollback_0 CHECK (foo > 0)
     * )
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `rollback on constraint exception with suspend transactions`(testDB: TestDB) = runSuspendIO {
        Assumptions.assumeTrue { testDB in rollbackSupportDbs() }

        val testTable = object: IntIdTable("TestRollback") {
            val foo = integer("foo").check { it greater 0 }
        }
        try {
            try {
                runBlocking {
                    withSuspendedDb(testDB) {
                        SchemaUtils.create(testTable)
                        testTable.insert { it[foo] = 1 }
                        testTable.insert { it[foo] = 0 } // foo > 0 조건을 만족하지 않음 (예외 발생)
                    }
                    fail("예외가 발생해서 Rollback 이 수행되어야 합니다.")
                }
            } catch (e: Throwable) {
                // log.warn(e) { "Fail to insert" }
            }

            withSuspendedDb(testDB) {
                testTable.selectAll().empty().shouldBeTrue()
            }
        } finally {
            withSuspendedDb(testDB) {
                SchemaUtils.drop(testTable)
            }
        }
    }

    /**
     * Batch Insert with ON CONFLICT DO NOTHING - [batchInsert] 시, 예외가 발생하면 해당 예외를 무시하고, 다음 작업을 수행합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (id VARCHAR(10) NOT NULL);
     * ALTER TABLE tester ADD CONSTRAINT tester_id_unique UNIQUE (id);
     *
     * INSERT INTO tester (id) VALUES ('foo');
     * INSERT INTO tester (id) VALUES ('foo') ON CONFLICT (id) DO NOTHING;
     * INSERT INTO tester (id) VALUES ('bar') ON CONFLICT (id) DO NOTHING;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batch insert number of inserted rows`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB in TestDB.ALL_MYSQL_MARIADB + TestDB.ALL_POSTGRES_LIKE }

        val tester = object: Table("tester") {
            val id = varchar("id", 10).uniqueIndex()
        }
        withTables(testDB, tester) {
            tester.insert { it[id] = "foo" }

            val executable = BatchInsertBlockingExecutable(
                statement = BatchInsertOnConflictDoNothing(tester)
            )

            val numInserted = executable.run {
                statement.addBatch()
                statement[tester.id] = "foo"        // 중복되므로 insert 되지 않음

                statement.addBatch()
                statement[tester.id] = "bar"        // 중복되지 않으므로 추가됨

                execute(this@withTables)
            }
            numInserted shouldBeEqualTo 1
        }
    }

    /**
     * INSERT 시 `databaseGenerated()` 를 사용하여 특정 컬럼 값을 입력합니다.
     *
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS generatedtable (
     *      id SERIAL PRIMARY KEY,
     *      amount INT NULL,
     *      computed_amount INT GENERATED ALWAYS AS (AMOUNT + 1 ) STORED NULL
     * )
     * ```
     *
     * ```sql
     * INSERT INTO GENERATEDTABLE (AMOUNT) VALUES (99);
     * INSERT INTO GENERATEDTABLE (AMOUNT) VALUES (NULL);
     * ```
     *
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert into nullable generated column`(testDB: TestDB) {
        withDb(testDB) {
            val generatedTable = object: IntIdTable("generatedTable") {
                val amount = integer("amount").nullable()
                val computedAmount = integer("computed_amount").nullable().databaseGenerated().apply {
                    if (testDB in TestDB.ALL_H2) {
                        withDefinition("GENERATED ALWAYS AS (AMOUNT + 1 )")
                    } else {
                        withDefinition("GENERATED ALWAYS AS (AMOUNT + 1 ) STORED")
                    }
                }
            }
            try {
                val computedName = generatedTable.computedAmount.name.inProperCase()
                val computedType = generatedTable.computedAmount.columnType.sqlType()
                val computation = "${generatedTable.amount.name.inProperCase()} + 1"

                val createStatement =
                    """
                    CREATE TABLE ${addIfNotExistsIfSupported()}${generatedTable.tableName.inProperCase()} (
                        ${generatedTable.id.descriptionDdl()},
                        ${generatedTable.amount.descriptionDdl()},
                    """.trimIndent()

                when (testDB) {
                    // MariaDB does not support GENERATED ALWAYS AS with any null constraint definition
                    in TestDB.ALL_MARIADB -> {
                        exec("${createStatement.trimIndent()} $computedName $computedType GENERATED ALWAYS AS ($computation) STORED)")
                    }
                    else -> SchemaUtils.create(generatedTable)
                }

                assertFailAndRollback("Generated columns are auto-drived and read-only") {
                    generatedTable.insert {
                        it[amount] = 99
                        it[computedAmount] = 100
                    }
                }

                generatedTable.insert {
                    it[amount] = 99
                }

                val result1 = generatedTable.selectAll().single()
                result1[generatedTable.computedAmount] shouldBeEqualTo result1[generatedTable.amount]?.plus(1)

                generatedTable.insert {
                    it[amount] = null
                }

                val result2 = generatedTable.selectAll().where { generatedTable.amount.isNull() }.single()
                result2[generatedTable.amount].shouldBeNull()
                result2[generatedTable.computedAmount].shouldBeNull()
            } finally {
                SchemaUtils.drop(generatedTable)
            }
        }
    }


    /**
     * INSERT 시 값이 제공되지 않으면, 기본값이나 NULL로 설정됩니다.
     *
     * NOTE: Postgres 만 지원합니다.
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS tester (
     *      default_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
     * )
     * ```
     * 아무런 값을 제공하지 않으면 기본값이 설정됩니다.
     * ```sql
     * INSERT INTO tester DEFAULT VALUES
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert returns values from default expression`(testDB: TestDB) {

        Assumptions.assumeTrue(testDB in TestDB.ALL_POSTGRES)

        val tester = object: Table("tester") {
            val defaultDate = timestamp("default_date").defaultExpression(CurrentTimestamp)
        }

        withTables(testDB, tester) {
            val entry = tester.insert { }

            log.debug { "default_date=${entry[tester.defaultDate]}" }
            entry[tester.defaultDate].shouldNotBeNull()
        }
    }

    /**
     * Postgres 에서는 `gen_random_uuid` 함수를 이용하여 기본 키로 사용할 수 있습니다.
     *
     * Postgres:
     * ```sql
     * CREATE TABLE IF NOT EXISTS test_uuid_table (
     *      id uuid DEFAULT gen_random_uuid() PRIMARY KEY
     * )
     * ```
     * ```sql
     * INSERT INTO test_uuid_table  DEFAULT VALUES
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `database generated uuid as primary key`(testDB: TestDB) {
        // Postgres 만 지원됩니다.
        Assumptions.assumeTrue(testDB in TestDB.ALL_POSTGRES)

        val randomPGUUID = object: CustomFunction<UUID>("gen_random_uuid", UUIDColumnType()) {}

        val tester = object: IdTable<UUID>("test_uuid_table") {
            override val id: Column<EntityID<UUID>> = uuid("id").defaultExpression(randomPGUUID).entityId()
            override val primaryKey = PrimaryKey(id)
        }

        withTables(testDB, tester) {
            val entry: EntityID<UUID> = tester.insertAndGetId { }
            log.debug { "generated uuid id=${entry.value}" }
            entry.value.shouldNotBeNull()
        }
    }
}
