package exposed.examples.custom.columns.serialization

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.core.serializable.binarySerializedBlob
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

class BinarySerializedBlobColumnTypeTest: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     *  CREATE TABLE IF NOT EXISTS t1 (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      lz4_fory bytea NULL,
     *      lz4_kryo bytea NULL,
     *      zstd_fory bytea NULL,
     *      zstd_kryo bytea NULL
     * )
     * ```
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS T1 (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` VARCHAR(50) NOT NULL,
     *      lz4_fory BLOB NULL,
     *      lz4_kryo BLOB NULL,
     *      zstd_fory BLOB NULL,
     *      zstd_kryo BLOB NULL
     * )
     * ```
     */
    private object T1: IntIdTable() {
        val name = varchar("name", 50)

        val lz4Fory = binarySerializedBlob<Embeddable>("lz4_fory", BinarySerializers.LZ4Fory).nullable()
        val lz4Kryo = binarySerializedBlob<Embeddable>("lz4_kryo", BinarySerializers.LZ4Kryo).nullable()
        val zstdFory = binarySerializedBlob<Embeddable2>("zstd_fory", BinarySerializers.ZstdFory).nullable()
        val zstdKryo = binarySerializedBlob<Embeddable2>("zstd_kryo", BinarySerializers.ZstdKryo).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name

        var lz4Fory by T1.lz4Fory
        var lz4Kryo by T1.lz4Kryo

        var zstdFory by T1.zstdFory
        var zstdKryo by T1.zstdKryo

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "E1(id=$id)"
    }

    data class Embeddable(
        val name: String,
        val age: Int,
        val address: String,
    ): Serializable

    // Schema evolution을 위해 추가된 필드
    data class Embeddable2(
        val name: String,
        val age: Int,
        val address: String,
        val zipcode: String,
    ): Serializable

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 Object 를 Binary Serializer를 이용해 DB Blob에 저장한다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val embedded = Embeddable("Alice", 20, "Seoul")
            val embedded2 = Embeddable2("John", 30, "Seoul", "12914")

            val id = T1.insertAndGetId {
                it[T1.name] = "Alice"

                it[T1.lz4Fory] = embedded
                it[T1.lz4Kryo] = embedded
                it[T1.zstdFory] = embedded2
                it[T1.zstdKryo] = embedded2
            }

            entityCache.clear()

            val row = T1.selectAll().where { T1.id eq id }.single()

            row[T1.id] shouldBeEqualTo id

            row[T1.lz4Fory] shouldBeEqualTo embedded
            row[T1.lz4Kryo] shouldBeEqualTo embedded
            row[T1.zstdFory] shouldBeEqualTo embedded2
            row[T1.zstdKryo] shouldBeEqualTo embedded2
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 Object 를 Binary Serializer를 이용해 DB Blob에 저장한다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val embedded = Embeddable("Alice", 20, "Seoul")
            val embedded2 = Embeddable2("John", 30, "Seoul", "12914")
            val e1 = E1.new {
                name = "Alice"

                lz4Fory = embedded
                lz4Kryo = embedded
                zstdFory = embedded2
                zstdKryo = embedded2
            }

            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1

            loaded.lz4Fory shouldBeEqualTo embedded
            loaded.lz4Kryo shouldBeEqualTo embedded
            loaded.zstdFory shouldBeEqualTo embedded2
            loaded.zstdKryo shouldBeEqualTo embedded2
        }
    }
}
