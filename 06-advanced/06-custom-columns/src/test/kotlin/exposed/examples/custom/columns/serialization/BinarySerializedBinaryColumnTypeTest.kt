package exposed.examples.custom.columns.serialization

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

class BinarySerializedBinaryColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS t1 (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      lz4_fury bytea NULL,
     *      zstd_fury bytea NULL,
     *      lz4_kryo bytea NULL,
     *      zstd_kryo bytea NULL
     * )
     * ```
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS T1 (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` VARCHAR(50) NOT NULL,
     *      lz4_fury VARBINARY(4096) NULL,
     *      zstd_fury VARBINARY(4096) NULL,
     *      lz4_kryo VARBINARY(4096) NULL,
     *      zstd_kryo VARBINARY(4096) NULL
     * )
     * ```
     */
    private object T1: IntIdTable() {
        val name = varchar("name", 50)

        val lz4Fury = binarySerializedBinary<Embeddable>(
            "lz4_fury",
            4096,
            BinarySerializers.LZ4Fury
        ).nullable()

        val lz4Kryo = binarySerializedBinary<Embeddable>(
            "lz4_kryo",
            4096,
            BinarySerializers.LZ4Kryo
        ).nullable()

        val zstdFury = binarySerializedBinary<Embeddable2>(
            "zstd_fury",
            4096,
            BinarySerializers.ZstdFury
        ).nullable()

        val zstdKryo = binarySerializedBinary<Embeddable2>(
            "zstd_kryo",
            4096,
            BinarySerializers.ZstdKryo
        ).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name

        var lz4Fury by T1.lz4Fury
        var lz4Kryo by T1.lz4Kryo

        var zstdFury by T1.zstdFury
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

    data class Embeddable2(
        val name: String,
        val age: Int,
        val address: String,
        val zipcode: String,
    ): Serializable

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 Object 를 Binary Serializer를 이용해 DB에 저장한다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val embedded = Embeddable("Alice", 20, "Seoul")
            val embedded2 = Embeddable2("John", 30, "Seoul", "12914")

            val id = T1.insertAndGetId {
                it[T1.name] = "Alice"

                it[T1.lz4Fury] = embedded
                it[T1.zstdFury] = embedded2
                it[T1.lz4Kryo] = embedded
                it[T1.zstdKryo] = embedded2
            }
            flushCache()

            val row = T1.selectAll().where { T1.id eq id }.single()

            row[T1.id] shouldBeEqualTo id

            row[T1.lz4Fury] shouldBeEqualTo embedded
            row[T1.zstdFury] shouldBeEqualTo embedded2

            row[T1.lz4Kryo] shouldBeEqualTo embedded
            row[T1.zstdKryo] shouldBeEqualTo embedded2
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 Object 를 Binary Serializer를 이용해 DB에 저장한다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val embedded = Embeddable("Alice", 20, "Seoul")
            val embedded2 = Embeddable2("John", 30, "Seoul", "12914")
            val e1 = E1.new {
                name = "Alice"

                lz4Fury = embedded
                zstdFury = embedded2

                lz4Kryo = embedded
                zstdKryo = embedded2
            }
            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded shouldBeEqualTo e1

            loaded.lz4Fury shouldBeEqualTo embedded
            loaded.zstdFury shouldBeEqualTo embedded2

            loaded.lz4Kryo shouldBeEqualTo embedded
            loaded.zstdKryo shouldBeEqualTo embedded2
        }
    }
}
