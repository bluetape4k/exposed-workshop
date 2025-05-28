package exposed.examples.custom.columns.compress

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.io.compressor.Compressors
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CompressedBinaryColumnTypeTest: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS t1 (
     *      id SERIAL PRIMARY KEY,
     *      lz4_data bytea NULL,
     *      snappy_data bytea NULL,
     *      zstd_data bytea NULL
     * )
     * ```
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS T1 (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      lz4_data VARBINARY(4096) NULL,
     *      snappy_data VARBINARY(4096) NULL,
     *      zstd_data VARBINARY(4096) NULL
     * )
     * ```
     */
    private object T1: IntIdTable() {
        val lzData: Column<ByteArray?> = compressedBinary("lz4_data", 4096, Compressors.LZ4).nullable()
        val snappyData: Column<ByteArray?> = compressedBinary("snappy_data", 4096, Compressors.Snappy).nullable()
        val zstdData: Column<ByteArray?> = compressedBinary("zstd_data", 4096, Compressors.Zstd).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var lz4Data: ByteArray? by T1.lzData
        var snappyData: ByteArray? by T1.snappyData
        var zstdData: ByteArray? by T1.zstdData

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = id.hashCode()
        override fun toString(): String = "E1(id=$id)"
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 데이터를 압축하여 byte array 컬럼에 저장합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = Fakers.randomString(1024, 2048)
            val bytes = text.toByteArray()

            val id = T1.insertAndGetId {
                it[lzData] = bytes
                it[snappyData] = bytes
                it[zstdData] = bytes
            }

            entityCache.clear()

            val row = T1.selectAll().where { T1.id eq id }.single()
            row[T1.lzData]!!.toUtf8String() shouldBeEqualTo text
            row[T1.snappyData]!!.toUtf8String() shouldBeEqualTo text
            row[T1.zstdData]!!.toUtf8String() shouldBeEqualTo text
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식으로 null 값을 byte array 컬럼에 저장합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val id = T1.insertAndGetId {
                it[lzData] = null
                it[snappyData] = null
                it[zstdData] = null
            }
            entityCache.clear()

            val row = T1.selectAll().where { T1.id eq id }.single()
            row[T1.lzData].shouldBeNull()
            row[T1.snappyData].shouldBeNull()
            row[T1.zstdData].shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식으로 속성을 압축하여 byte array 컬럼에 저장합니다`(testDB: TestDB) {
        val text = Fakers.randomString(1024, 2048)
        val bytes = text.toByteArray()

        withTables(testDB, T1) {
            val e1 = E1.new {
                lz4Data = bytes
                snappyData = bytes
                zstdData = bytes
            }

            entityCache.clear()

            val loaded = E1.findById(e1.id)!!
            loaded shouldBeEqualTo e1
            loaded.lz4Data!!.toUtf8String() shouldBeEqualTo text
            loaded.snappyData!!.toUtf8String() shouldBeEqualTo text
            loaded.zstdData!!.toUtf8String() shouldBeEqualTo text
        }
    }
}
