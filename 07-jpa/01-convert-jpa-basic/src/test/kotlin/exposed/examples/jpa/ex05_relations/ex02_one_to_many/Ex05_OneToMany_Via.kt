package exposed.examples.jpa.ex05_relations.ex02_one_to_many

import exposed.examples.jpa.ex05_relations.ex02_one_to_many.Ex05_OneToMany_Via.CloudSnowflakeTable.cloudId
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.SizedCollection
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

/**
 * JPA 의 one-to-many 관계를 @JoinTable 방식을 Exposed 로 구현 한 예l
 */
class Ex05_OneToMany_Via: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS clouds (
     *      id SERIAL PRIMARY KEY,
     *      kind VARCHAR(255) NOT NULL,
     *      "length" DOUBLE PRECISION NOT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS clouds (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      kind VARCHAR(255) NOT NULL,
     *      `length` DOUBLE PRECISION NOT NULL
     * );
     * ```
     */
    object CloudTable: IntIdTable("clouds") {
        val kind = varchar("kind", 255)
        val length = double("length")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS snowflakes (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      description TEXT NULL
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS snowflakes (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` VARCHAR(255) NOT NULL,
     *      description text NULL
     * );
     * ```
     */
    object SnowflakeTable: IntIdTable("snowflakes") {
        val name = varchar("name", 255)
        val description = text("description").nullable()
    }

    /**
     * [CloudTable] 과 [SnowflakeTable] 의 many-to-many 관계를 표현하기 위한 조인 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS cloud_snowflakes (
     *      cloud_id INT,
     *      snowflake_id INT,
     *
     *      CONSTRAINT pk_cloud_snowflakes PRIMARY KEY (cloud_id, snowflake_id),
     *
     *      CONSTRAINT fk_cloud_snowflakes_cloud_id__id FOREIGN KEY (cloud_id)
     *      REFERENCES clouds(id) ON DELETE CASCADE ON UPDATE CASCADE,
     *
     *      CONSTRAINT fk_cloud_snowflakes_snowflake_id__id FOREIGN KEY (snowflake_id)
     *      REFERENCES snowflakes(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     *
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS cloud_snowflakes (
     *      cloud_id INT,
     *      snowflake_id INT,
     *
     *      CONSTRAINT pk_cloud_snowflakes PRIMARY KEY (cloud_id, snowflake_id),
     *
     *      CONSTRAINT fk_cloud_snowflakes_cloud_id__id FOREIGN KEY (cloud_id)
     *      REFERENCES clouds(id) ON DELETE CASCADE ON UPDATE CASCADE,
     *
     *      CONSTRAINT fk_cloud_snowflakes_snowflake_id__id FOREIGN KEY (snowflake_id)
     *      REFERENCES snowflakes(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     * ```
     */
    object CloudSnowflakeTable: Table("cloud_snowflakes") {
        val cloudId =
            reference("cloud_id", CloudTable, onDelete = ReferenceOption.CASCADE, onUpdate = ReferenceOption.CASCADE)
        val snowflakeId = reference(
            "snowflake_id",
            SnowflakeTable,
            onDelete = ReferenceOption.CASCADE,
            onUpdate = ReferenceOption.CASCADE
        )

        override val primaryKey = PrimaryKey(cloudId, snowflakeId)
    }

    class Cloud(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Cloud>(CloudTable)

        var kind by CloudTable.kind
        var length by CloudTable.length

        // one-to-many unidirectional association
        var producedSnowflakes: SizedIterable<Snowflake> by Snowflake via CloudSnowflakeTable

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("kind", kind)
            .toString()
    }

    class Snowflake(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Snowflake>(SnowflakeTable)

        var name by SnowflakeTable.name
        var description by SnowflakeTable.description

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    private fun fakeCound() = Cloud.new {
        kind = faker.name().name()
        length = faker.random().nextDouble(0.0, 40.0)
    }

    private fun fakeSnowflake() = Snowflake.new {
        name = faker.name().name()
        description = faker.lorem().paragraph()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 관계 설정`(testDB: TestDB) {
        withTables(testDB, CloudTable, SnowflakeTable, CloudSnowflakeTable) {
            val snowflake1 = fakeSnowflake()
            val snowflake2 = fakeSnowflake()
            val cloud = fakeCound()
            cloud.producedSnowflakes = SizedCollection(snowflake1, snowflake2)

            entityCache.clear()

            val cloud2 = Cloud.findById(cloud.id)!!
            cloud2.producedSnowflakes.count() shouldBeEqualTo 2L
            cloud2.producedSnowflakes.toSet() shouldContainSame setOf(snowflake1, snowflake2)

            // Remove snowflake
            val snowflakes: List<Snowflake> = cloud2.producedSnowflakes.toList()
            cloud2.producedSnowflakes = SizedCollection(snowflakes.drop(1))

            entityCache.clear()

            // 관계된 snowflakes 개수 조회
            cloud2.producedSnowflakes.count() shouldBeEqualTo 1L

            // 기존 정보는 삭제되지 않음
            Cloud.count() shouldBeEqualTo 1L
            Snowflake.count() shouldBeEqualTo 2L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many unidirectional association`(testDB: TestDB) {
        withTables(testDB, CloudTable, SnowflakeTable, CloudSnowflakeTable) {
            val snowflake1 = fakeSnowflake()
            val snowflake2 = fakeSnowflake()
            val cloud = fakeCound()

            CloudSnowflakeTable.insert {
                it[cloudId] = cloud.id
                it[snowflakeId] = snowflake1.id
            }
            CloudSnowflakeTable.insert {
                it[cloudId] = cloud.id
                it[snowflakeId] = snowflake2.id
            }
            entityCache.clear()

            val cloud2 = Cloud.findById(cloud.id)!!
            cloud2.producedSnowflakes.count() shouldBeEqualTo 2L
            cloud2.producedSnowflakes.toSet() shouldContainSame setOf(snowflake1, snowflake2)

            // Remove first relation
            CloudSnowflakeTable
                .deleteWhere {
                    (cloudId eq cloud.id) and (snowflakeId eq snowflake1.id)
                }

            // 관계된 snowflakes 개수 조회 (한개 삭제, 한개 추가)
            CloudSnowflakeTable.selectAll()
                .where { cloudId eq cloud.id }
                .count() shouldBeEqualTo 1L

            // 기존 정보는 삭제되지 않음 
            Cloud.count() shouldBeEqualTo 1L
            Snowflake.count() shouldBeEqualTo 2L

            val snowflake3 = fakeSnowflake()
            CloudSnowflakeTable.insert {
                it[cloudId] = cloud.id
                it[snowflakeId] = snowflake3.id
            }

            entityCache.clear()

            // 관계된 snowflakes 개수 조회 (한개 삭제, 한개 추가)
            CloudSnowflakeTable.selectAll().count() shouldBeEqualTo 2L

            Snowflake.count() shouldBeEqualTo 3L

            val cloud3 = Cloud.findById(cloud.id)!!

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM snowflakes INNER JOIN cloud_snowflakes ON snowflakes.id = cloud_snowflakes.snowflake_id
             *  WHERE cloud_snowflakes.cloud_id = 1
             * ```
             */
            cloud3.producedSnowflakes.count() shouldBeEqualTo 2L

            /**
             * ```sql
             * -- Postgres
             * SELECT snowflakes.id,
             *        snowflakes."name",
             *        snowflakes.description,
             *        cloud_snowflakes.snowflake_id,
             *        cloud_snowflakes.cloud_id
             *   FROM snowflakes INNER JOIN cloud_snowflakes ON snowflakes.id = cloud_snowflakes.snowflake_id
             *  WHERE cloud_snowflakes.cloud_id = 1
             * ```
             */
            cloud3.producedSnowflakes.toSet() shouldContainSame setOf(
                snowflake2,
                snowflake3
            )
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `violation of duplicated primary key`(testDB: TestDB) {
        withTables(testDB, CloudTable, SnowflakeTable, CloudSnowflakeTable) {
            val snowflake1 = fakeSnowflake()
            val snowflake2 = fakeSnowflake()
            val cloud = fakeCound()

            flushCache()

            CloudSnowflakeTable.insert {
                it[cloudId] = cloud.id
                it[snowflakeId] = snowflake1.id
            }
            CloudSnowflakeTable.insert {
                it[cloudId] = cloud.id
                it[snowflakeId] = snowflake2.id
            }

            // Duplicated entry
            assertFailsWith<ExposedSQLException> {
                CloudSnowflakeTable.insert {
                    it[cloudId] = cloud.id
                    it[snowflakeId] = snowflake1.id
                }
            }
        }
    }
}
