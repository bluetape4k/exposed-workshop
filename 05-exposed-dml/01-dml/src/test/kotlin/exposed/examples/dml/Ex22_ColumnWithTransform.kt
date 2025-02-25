package exposed.examples.dml

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnTransformer
import org.jetbrains.exposed.sql.ColumnWithTransform
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.update
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.util.*

/**
 * 컬럼의 수형이나 값을 변환하는 [ColumnTransformer] 를 활용하는 예제
 */
class Ex22_ColumnWithTransform: AbstractExposedTest() {

    companion object: KLogging()

    @JvmInline
    value class TransformDataHolder(val value: Int): Serializable

    class DataHolderTransformer: ColumnTransformer<Int, TransformDataHolder> {
        override fun unwrap(value: TransformDataHolder): Int = value.value
        override fun wrap(value: Int): TransformDataHolder = TransformDataHolder(value)
    }

    class DataHolderNullableTransformer: ColumnTransformer<Int?, TransformDataHolder?> {
        override fun unwrap(value: TransformDataHolder?): Int? = value?.value
        override fun wrap(value: Int?): TransformDataHolder? = value?.let { TransformDataHolder(it) }
    }

    class DataHolderNullTransformer: ColumnTransformer<Int, TransformDataHolder?> {
        override fun unwrap(value: TransformDataHolder?): Int = value?.value ?: 0
        override fun wrap(value: Int): TransformDataHolder? = if (value == 0) null else TransformDataHolder(value)
    }

    /**
     * 회귀적으로 transform을 적용하는 경우, ColumnWithTransform의 수형이 변환된 수형이어야 한다.
     *
     * 예를 들어, Binary Serializer를 적용한 후, Compressor 를 적용하는 경우 등이 있다.
     */
    @Suppress("UNCHECKED_CAST")
    @Test
    fun `recursive unwrap`() {
        val tester1 = object: IntIdTable() {
            val value: Column<TransformDataHolder?> = integer("value")
                .transform(DataHolderTransformer())
                .nullable()
        }
        val columnType1 = tester1.value.columnType as? ColumnWithTransform<Int, TransformDataHolder>
        columnType1.shouldNotBeNull()
        columnType1.unwrapRecursive(TransformDataHolder(1)) shouldBeEqualTo 1
        columnType1.unwrapRecursive(null).shouldBeNull()

        // Transform null into non-null value
        val tester2 = object: IntIdTable() {
            val value = integer("value")
                .nullTransform(DataHolderNullTransformer())
        }
        val columnType2 = tester2.value.columnType as? ColumnWithTransform<Int, TransformDataHolder?>
        columnType2.shouldNotBeNull()
        columnType2.unwrapRecursive(TransformDataHolder(1)) shouldBeEqualTo 1
        columnType2.unwrapRecursive(null) shouldBeEqualTo 0

        // Transform 을 2번 적용하므로, ColumnWithTransform<TransformDataHolder?, Int?> 가 생성되어야 한다.
        val tester3 = object: IntIdTable() {
            val value = integer("value")
                .transform(DataHolderTransformer())
                .nullable()
                .transform(wrap = { it?.value ?: 0 }, unwrap = { TransformDataHolder(it ?: 0) })
        }
        val columnType3 = tester3.value.columnType as? ColumnWithTransform<TransformDataHolder?, Int?>
        columnType3.shouldNotBeNull()
        columnType3.unwrapRecursive(1) shouldBeEqualTo 1
        columnType3.unwrapRecursive(null) shouldBeEqualTo 0
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `simple transforms`(testDB: TestDB) {
        /**
         * `transform` 함수를 이용해 wrapping, unwrapping을 수행하는 예제
         *
         * ```sql
         * CREATE TABLE IF NOT EXISTS simple_transforms (
         *      id SERIAL PRIMARY KEY,
         *      v1 INT NOT NULL,
         *      v2 INT NULL,
         *      v3 INT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("simple_transforms") {
            val v1 = integer("v1")
                .transform(
                    wrap = { TransformDataHolder(it) },
                    unwrap = { it.value }
                )
            val v2 = integer("v2")
                .nullable()
                .transform(
                    wrap = { it?.let { TransformDataHolder(it) } },
                    unwrap = { it?.value }
                )
            val v3 = integer("v3")
                .transform(
                    wrap = { TransformDataHolder(it) },
                    unwrap = { it.value }
                )
                .nullable()
        }

        withTables(testDB, tester) {
            // INSERT INTO simple_transforms (v1, v2, v3) VALUES (1, 2, 3)
            val id1 = tester.insertAndGetId {
                it[v1] = TransformDataHolder(1)
                it[v2] = TransformDataHolder(2)
                it[v3] = TransformDataHolder(3)
            }

            val entry1 = tester.selectAll().where { tester.id eq id1 }.single()
            entry1[tester.v1].value shouldBeEqualTo 1
            entry1[tester.v2]?.value shouldBeEqualTo 2
            entry1[tester.v3]?.value shouldBeEqualTo 3

            // INSERT INTO simple_transforms (v1, v2, v3) VALUES (1, NULL, NULL)
            val id2 = tester.insertAndGetId {
                it[v1] = TransformDataHolder(1)
                it[v2] = null
                it[v3] = null
            }
            val entry2 = tester.selectAll().where { tester.id eq id2 }.single()
            entry2[tester.v1].value shouldBeEqualTo 1
            entry2[tester.v2].shouldBeNull()
            entry2[tester.v3].shouldBeNull()
        }
    }

    /**
     * 여러 개의 `transform` 을 중첩해서 적용하는 예
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `nested transforms`(testDB: TestDB) {
        /**
         * Postgres:
         * ```sql
         * CREATE TABLE IF NOT EXISTS nested_transformer (
         *      id SERIAL PRIMARY KEY,
         *      v1 INT NOT NULL,
         *      v2 INT NULL,
         *      v3 INT NULL,
         *      v4 INT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("nested_transformer") {
            val v1: Column<String> = integer("v1")
                .transform(DataHolderTransformer())
                .transform(
                    wrap = { it.value.toString() },
                    unwrap = { TransformDataHolder(it.toInt()) }
                )

            val v2: Column<String?> = integer("v2")
                .transform(DataHolderTransformer())
                .transform(
                    wrap = { it.value.toString() },
                    unwrap = { TransformDataHolder(it.toInt()) }
                )
                .nullable()

            val v3: Column<String?> = integer("v3")
                .transform(DataHolderTransformer())
                .nullable()
                .transform(
                    wrap = { it?.value.toString() },
                    unwrap = { it?.let { it1 -> TransformDataHolder(it1.toInt()) } }
                )

            val v4: Column<String?> = integer("v4")
                .nullable()
                .transform(DataHolderNullableTransformer())
                .transform(
                    wrap = { it?.value.toString() },
                    unwrap = { it?.let { it1 -> TransformDataHolder(it1.toInt()) } }
                )
        }

        withTables(testDB, tester) {
            val id1 = tester.insertAndGetId {
                it[v1] = "1"
                it[v2] = "2"
                it[v3] = "3"
                it[v4] = "4"
            }

            val entry1 = tester.selectAll().where { tester.id eq id1 }.single()
            entry1[tester.v1] shouldBeEqualTo "1"
            entry1[tester.v2] shouldBeEqualTo "2"
            entry1[tester.v3] shouldBeEqualTo "3"
            entry1[tester.v4] shouldBeEqualTo "4"

            val id2 = tester.insertAndGetId {
                it[v1] = "1"
                it[v2] = null
                it[v3] = null
                it[v4] = null
            }
            val entry2 = tester.selectAll().where { tester.id eq id2 }.single()
            entry2[tester.v1] shouldBeEqualTo "1"
            entry2[tester.v2].shouldBeNull()
            entry2[tester.v3].shouldBeNull()
            entry2[tester.v4].shouldBeNull()
        }
    }

    /**
     * [InsertStatement] 에서 transform 된 값을 읽어오는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `read transformed values from insert statement`(testDB: TestDB) {
        val tester = object: IntIdTable("read_transformed_values") {
            val v1: Column<TransformDataHolder> = integer("v1").transform(DataHolderTransformer())
            val v2: Column<TransformDataHolder?> = integer("v2").nullTransform(DataHolderNullTransformer())
        }

        withTables(testDB, tester) {
            /**
             * ```sql
             * INSERT INTO read_transformed_values (v1, v2) VALUES (1, 0)
             * ```
             */
            val statement: InsertStatement<Number> = tester.insert {
                it[tester.v1] = TransformDataHolder(1)
                it[tester.v2] = null
            }

            statement[tester.v1] shouldBeEqualTo TransformDataHolder(1)
            statement[tester.v2].shouldBeNull()
        }
    }

    /**
     * ```sql
     * CREATE TABLE IF NOT EXISTS TRANSFORM_TABLE (
     *      ID INT AUTO_INCREMENT PRIMARY KEY,
     *      "simple" INT DEFAULT 1 NOT NULL,
     *      CHAINED VARCHAR(128) DEFAULT '2' NOT NULL
     * )
     * ```
     */
    object TransformTable: IntIdTable("transform_table") {
        val simple: Column<TransformDataHolder> = integer("simple")
            .default(1)
            .transform(DataHolderTransformer())

        val chained: Column<TransformDataHolder> = varchar("chained", 128)
            .transform(wrap = { it.toInt() }, unwrap = { it.toString() })
            .transform(DataHolderTransformer())
            .default(TransformDataHolder(2))
    }

    class TransformEntity(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<TransformEntity>(TransformTable)

        var simple: TransformDataHolder by TransformTable.simple
        var chained: TransformDataHolder by TransformTable.chained

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String =
            toStringBuilder()
                .add("simple", simple)
                .add("chained", chained)
                .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transformed values with DAO`(testDB: TestDB) {
        withTables(testDB, TransformTable) {
            val entity = TransformEntity.new {
                simple = TransformDataHolder(120)
                chained = TransformDataHolder(240)
            }
            log.debug { "entity: $entity" }
            entity.simple shouldBeEqualTo TransformDataHolder(120)
            entity.chained shouldBeEqualTo TransformDataHolder(240)

            val row = TransformTable.selectAll().first()
            row[TransformTable.simple] shouldBeEqualTo TransformDataHolder(120)
            row[TransformTable.chained] shouldBeEqualTo TransformDataHolder(240)
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `entity with default value`(testDB: TestDB) {
        withTables(testDB, TransformTable) {
            val entity = TransformEntity.new { }
            entity.simple shouldBeEqualTo TransformDataHolder(1)
            entity.chained shouldBeEqualTo TransformDataHolder(2)

            val row = TransformTable.selectAll().first()
            row[TransformTable.simple] shouldBeEqualTo TransformDataHolder(1)
            row[TransformTable.chained] shouldBeEqualTo TransformDataHolder(2)
        }
    }

    @JvmInline
    value class CustomId(val id: UUID): Serializable

    /**
     * value class 를 entity id 로 사용하는 예제 (`transform` 함수를 이용해 wrapping, unwrapping을 수행)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transform id column`(testDB: TestDB) {

        /**
         * value class인 [CustomId]의 value 수형이 UUID 이므로, 테이블 기본 키의 수형은 UUID 이다.
         * ```sql
         * CREATE TABLE IF NOT EXISTS tester (id uuid PRIMARY KEY)
         * ```
         */
        val tester = object: IdTable<CustomId>("tester") {
            override val id: Column<EntityID<CustomId>> = uuid("id")
                .transform(wrap = { CustomId(it) }, unwrap = { it.id })  // value class 는 이렇게 사용하면 된다.
                .entityId()

            override val primaryKey = PrimaryKey(id)
        }

        /**
         * `reference` 컬럼은 `tester` 테이블의 기본 키를 참조한다. 같은 수형인 UUID 수형으로 정의됩니다.
         *
         * ```sql
         * CREATE TABLE IF NOT EXISTS ref_tester (
         *      id SERIAL PRIMARY KEY,
         *      reference uuid NOT NULL,
         *
         *      CONSTRAINT fk_ref_tester_reference__id FOREIGN KEY (reference) REFERENCES tester(id)
         *          ON DELETE RESTRICT ON UPDATE RESTRICT
         * )
         * ```
         */
        val referenceTester = object: IntIdTable("ref_tester") {
            val reference: Column<EntityID<CustomId>> = reference("reference", tester)
        }

        val uuid = TimebasedUuid.Epoch.nextId()
        withTables(testDB, tester, referenceTester) {
            // CustomId 를 지정 (UUID 값만 저장됨)
            /**
             * ```sql
             * INSERT INTO tester (id) VALUES ('${uuid}')
             * ```
             */
            tester.insert {
                it[id] = CustomId(uuid)
            }
            val transformedId: EntityID<CustomId> = tester.selectAll().single()[tester.id]
            transformedId.value shouldBeEqualTo CustomId(uuid)

            /**
             * `ref_tester` 테이블에 `tester` 테이블의 기본 키를 참조하는 레코드를 추가한다.
             *
             * ```sql
             * INSERT INTO ref_tester (reference) VALUES (0194b78c-d028-73a2-933b-2fe80bf31095)
             * ```
             */
            referenceTester.insert {
                it[reference] = transformedId
            }

            val referenceId = referenceTester.selectAll().single()[referenceTester.reference]
            referenceId.value shouldBeEqualTo CustomId(uuid)
        }
    }

    /**
     * Application에서 null 을 지정하면, DB에는 특정 값(-1)으로 저장되도록 하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `null to non-null transform`(testDB: TestDB) {
        /**
         * ```sql
         * CREATE TABLE IF NOT EXISTS TESTER (
         *      ID INT AUTO_INCREMENT PRIMARY KEY,
         *      "value" INT NULL
         * )
         * ```
         */
        val tester = object: IntIdTable("tester") {
            val value: Column<Int?> = integer("value")
                .nullable()
                .transform(wrap = { if (it == -1) null else it }, unwrap = { it ?: -1 })   // null 이 지정되면 -1로 DB에 저장
        }
        val rawTester = object: IntIdTable("tester") {
            val value: Column<Int?> = integer("value").nullable()
        }

        withTables(testDB, tester) {
            tester.insert {
                it[value] = null
            }

            tester.selectAll().single()[tester.value] shouldBeEqualTo null
            rawTester.selectAll().single()[rawTester.value] shouldBeEqualTo -1
        }
    }

    /**
     * Application에서 null 을 지정하면, DB에는 특정 값(-1)으로 저장되도록 하는 예제
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `null to non-null recursive transform`(testDB: TestDB) {
        val tester = object: IntIdTable("tester") {
            val value: Column<TransformDataHolder?> = integer("value")
                .nullable()
                .transform(wrap = { if (it == -1) null else it }, unwrap = { it ?: -1 })
                .transform(DataHolderNullableTransformer())
        }
        val rawTester = object: IntIdTable("tester") {
            val value: Column<Long?> = long("value").nullable()
        }

        withTables(testDB, tester) {
            val id1 = tester.insertAndGetId {
                it[value] = TransformDataHolder(100)
            }
            tester.selectAll().where { tester.id eq id1 }.single()[tester.value]?.value shouldBeEqualTo 100
            rawTester.selectAll().where { rawTester.id eq id1 }.single()[rawTester.value] shouldBeEqualTo 100L

            val id2 = tester.insertAndGetId {
                it[value] = null
            }

            tester.selectAll().where { tester.id eq id2 }.single()[tester.value]?.value.shouldBeNull()
            rawTester.selectAll().where { rawTester.id eq id2 }.single()[rawTester.value] shouldBeEqualTo -1L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `null transform`(testDB: TestDB) {
        val tester = object: IntIdTable("tester") {
            val value: Column<TransformDataHolder?> = integer("value")
                .nullTransform(DataHolderNullTransformer())
        }
        val rawTester = object: IntIdTable("tester") {
            val value: Column<Int> = integer("value")
        }

        withTables(testDB, tester) {
            val result = tester.insert {
                it[value] = null
            }
            result[tester.value].shouldBeNull()
            tester.selectAll().single()[tester.value].shouldBeNull()
            rawTester.selectAll().single()[rawTester.value] shouldBeEqualTo 0
        }
    }

    /**
     * [ColumnTransformer]를 상속받아 구현한 [DataHolderTransformer] 를 사용하는 예제
     *
     * 여기에 기본 값을 value class로 지정할 수 있다.
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS tester (
     *      id SERIAL PRIMARY KEY,
     *      "value" INT DEFAULT 1 NOT NULL
     * );
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transform with default`(testDB: TestDB) {
        val tester = object: IntIdTable("tester") {
            val value: Column<TransformDataHolder> = integer("value")
                .transform(DataHolderTransformer())
                .default(TransformDataHolder(1))
        }

        withTables(testDB, tester) {
            // 기본 값이 지정되어 있으므로, 값을 지정하지 않아도 된다.
            val entry = tester.insert { }
            entry[tester.value] shouldBeEqualTo TransformDataHolder(1)

            // INSERT INTO tester  DEFAULT VALUES
            tester.selectAll().first()[tester.value] shouldBeEqualTo TransformDataHolder(1)
        }
    }

    /**
     * Batch Insert 시에도 transform 이 적용되는지 확인
     *
     * ```sql
     * INSERT INTO "TEST-BATCH-INSERT" (V1) VALUES (1)
     * INSERT INTO "TEST-BATCH-INSERT" (V1) VALUES (2)
     * INSERT INTO "TEST-BATCH-INSERT" (V1) VALUES (3)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transform in batch insert`(testDB: TestDB) {
        val tester = object: IntIdTable("test-batch-insert") {
            val v1 = integer("v1")
                .transform(wrap = { TransformDataHolder(it) }, unwrap = { it.value })
        }

        withTables(testDB, tester) {
            tester.batchInsert(listOf(1, 2, 3)) {
                this[tester.v1] = TransformDataHolder(it)
            }

            tester.selectAll()
                .orderBy(tester.v1)
                .map { it[tester.v1].value } shouldBeEqualTo listOf(1, 2, 3)
        }
    }

    /**
     * INSERT 시 뿐 아니라 UPDATE 시에도 transform 이 적용되는지 확인
     *
     * ```sql
     * INSERT INTO "TEST-UPDATE" (V1) VALUES (1)
     * ```
     *
     * UPDATE
     * ```sql
     * UPDATE "TEST-UPDATE" SET V1=2 WHERE "TEST-UPDATE".ID = 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `transform in update`(testDB: TestDB) {
        val tester = object: IntIdTable("test-update") {
            val v1 = integer("v1")
                .transform(wrap = { TransformDataHolder(it) }, unwrap = { it.value })
        }

        withTables(testDB, tester) {
            val id = tester.insertAndGetId {
                it[v1] = TransformDataHolder(1)
            }

            tester.update(where = { tester.id eq id }) {
                it[tester.v1] = TransformDataHolder(2)
            }

            tester.selectAll().first()[tester.v1].value shouldBeEqualTo 2
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `wrapRow with aliases`(testDB: TestDB) {
        withTables(testDB, TransformTable) {
            TransformEntity.new {
                simple = TransformDataHolder(10)
            }
            entityCache.clear()

            val tableAlias = TransformTable.alias("tableAlias")
            val e2 = tableAlias.selectAll().map { TransformEntity.wrapRow(it, tableAlias) }.single()
            e2.simple shouldBeEqualTo TransformDataHolder(10)

            entityCache.clear()

            val queryAlias = TransformTable.selectAll().alias("queryAlias")
            val e3 = queryAlias.selectAll().map { TransformEntity.wrapRow(it, queryAlias) }.single()
            e3.simple shouldBeEqualTo TransformDataHolder(10)
        }
    }
}
