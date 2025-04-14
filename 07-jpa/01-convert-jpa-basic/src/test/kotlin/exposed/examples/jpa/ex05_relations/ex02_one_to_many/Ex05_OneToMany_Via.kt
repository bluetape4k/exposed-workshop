package exposed.examples.jpa.ex05_relations.ex02_one_to_many

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

/**
 * JPA 의 one-to-many 관계를 @JoinTable 방식을 Exposed 로 구현 한 예l
 */
class Ex05_OneToMany_Via: AbstractExposedTest() {

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
        val cloudId = reference("cloud_id", CloudTable, onDelete = CASCADE, onUpdate = CASCADE)
        val snowflakeId = reference("snowflake_id", SnowflakeTable, onDelete = CASCADE, onUpdate = CASCADE)

        override val primaryKey = PrimaryKey(cloudId, snowflakeId)
    }

    class Cloud(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Cloud>(CloudTable)

        var kind by CloudTable.kind
        var length by CloudTable.length

        // one-to-many unidirectional association
        val producedSnowflakes: SizedIterable<Snowflake> by Snowflake via CloudSnowflakeTable

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

            flushCache()
            entityCache.clear()

            val cloud2 = Cloud.findById(cloud.id)!!
            cloud2.producedSnowflakes.count() shouldBeEqualTo 2L
            cloud2.producedSnowflakes.toSet() shouldContainSame setOf(snowflake1, snowflake2)

            // Remove snowflake
            val snowflakeToRemove = cloud2.producedSnowflakes.first()
            snowflakeToRemove.delete()

            val snowflake3 = fakeSnowflake()
            CloudSnowflakeTable.insert {
                it[cloudId] = cloud.id
                it[snowflakeId] = snowflake3.id
            }

            entityCache.clear()

            Snowflake.count() shouldBeEqualTo 2L

            val cloud3 = Cloud.findById(cloud.id)!!

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM snowflakes INNER JOIN cloud_snowflakes ON snowflakes.id = cloud_snowflakes.snowflake_id
             *  WHERE cloud_snowflakes.cloud_id = 1
             * ```
             */

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM snowflakes INNER JOIN cloud_snowflakes ON snowflakes.id = cloud_snowflakes.snowflake_id
             *  WHERE cloud_snowflakes.cloud_id = 1
             * ```
             */

            /**
             * ```sql
             * -- Postgres
             * SELECT COUNT(*)
             *   FROM snowflakes INNER JOIN cloud_snowflakes ON snowflakes.id = cloud_snowflakes.snowflake_id
             *  WHERE cloud_snowflakes.cloud_id = 1
             * ```
             */

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
                snowflake1,
                snowflake2,
                snowflake3
            ) - snowflakeToRemove
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
