package exposed.examples.custom.columns.encrypt

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class BluetapeEncryptedBinaryColumnTypeTest: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS t1 (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      aes_binary bytea NULL,
     *      rc4_binary bytea NULL,
     *      triple_des_binary bytea NULL
     * );
     * ```
     *
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS T1 (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` VARCHAR(50) NOT NULL,
     *      aes_binary VARBINARY(1024) NULL,
     *      rc4_binary VARBINARY(1024) NULL,
     *      triple_des_binary VARBINARY(1024) NULL
     * );
     * ```
     */
    private object T1: IntIdTable("T1") {
        val name = varchar("name", 50)

        val aesBinary: Column<ByteArray?> = bluetapeEncryptedBinary("aes_binary", 1024, Encryptors.AES).nullable()
        val rc4Binary: Column<ByteArray?> = bluetapeEncryptedBinary("rc4_binary", 1024, Encryptors.RC4).nullable()
        val tripleDesBinary: Column<ByteArray?> =
            bluetapeEncryptedBinary("triple_des_binary", 1024, Encryptors.TripleDES).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name

        var aesBinary: ByteArray? by T1.aesBinary
        var rc4Binary: ByteArray? by T1.rc4Binary
        var tripleDesBinary: ByteArray? by T1.tripleDesBinary

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("aesBinary", aesBinary?.toUtf8String())
            .add("rc4Binary", rc4Binary?.toUtf8String())
            .add("tripleDesBinary", tripleDesBinary?.toUtf8String())
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `필드 값을을 암호화하여 Binary 컬럼에 저장합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = Fakers.randomString(255, 512)
            val bytes = text.toUtf8Bytes()

            val id = T1.insertAndGetId {
                it[T1.name] = "Encryption"

                it[T1.aesBinary] = bytes
                it[T1.rc4Binary] = bytes
                it[T1.tripleDesBinary] = bytes
            }

            entityCache.clear()

            val row = T1.selectAll().where { T1.id eq id }.single()

            row[T1.name] shouldBeEqualTo "Encryption"
            row[T1.aesBinary]?.toUtf8String() shouldBeEqualTo text
            row[T1.rc4Binary]?.toUtf8String() shouldBeEqualTo text
            row[T1.tripleDesBinary]?.toUtf8String() shouldBeEqualTo text
        }
    }

    /**
     * exposed-crypt 랑 달리 암호화된 값을 그대로 비교합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT t1.id, t1."name", t1.aes_binary, t1.rc4_binary, t1.triple_des_binary
     *   FROM t1
     *  WHERE t1.aes_binary = [B@6acb45c1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DSL 방식 - 암호화된 컬럼으로 검색합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = "동해물과 백두산이 마르고 닳도록"
            val bytes = text.toUtf8Bytes()

            T1.insert {
                it[T1.name] = "Encryption"

                it[T1.aesBinary] = bytes
                it[T1.rc4Binary] = bytes
                it[T1.tripleDesBinary] = bytes
            }

            entityCache.clear()

            val row = T1.selectAll().where { T1.aesBinary eq bytes }.single()

            row[T1.name] shouldBeEqualTo "Encryption"
            row[T1.aesBinary]?.toUtf8String() shouldBeEqualTo text
            row[T1.rc4Binary]?.toUtf8String() shouldBeEqualTo text
            row[T1.tripleDesBinary]?.toUtf8String() shouldBeEqualTo text
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `엔티티 속성 값을을 암호화하여 Binary 컬럼에 저장합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = Fakers.randomString(255, 512)
            val bytes = text.toUtf8Bytes()

            val e1 = E1.new {
                name = "Encryption"

                aesBinary = bytes
                rc4Binary = bytes
                tripleDesBinary = bytes
            }
            flushCache()

            val loaded = E1.findById(e1.id)!!

            loaded.name shouldBeEqualTo "Encryption"
            loaded.aesBinary?.toUtf8String() shouldBeEqualTo text
            loaded.rc4Binary?.toUtf8String() shouldBeEqualTo text
            loaded.tripleDesBinary?.toUtf8String() shouldBeEqualTo text
        }
    }

    /**
     * exposed-crypt 랑 달리 암호화된 값을 그대로 비교합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT t1.id, t1."name", t1.aes_binary, t1.rc4_binary, t1.triple_des_binary
     *   FROM t1
     *  WHERE t1.aes_binary = [B@18ac4af6
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식 - 암호화된 속성값으로 검색합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = Fakers.randomString(8, 16)
            val bytes = text.toUtf8Bytes()

            val e1 = E1.new {
                name = "Encryption"

                aesBinary = bytes
                rc4Binary = bytes
                tripleDesBinary = bytes
            }
            flushCache()

            val loaded = E1.find { T1.aesBinary eq e1.aesBinary }.single()

            loaded.name shouldBeEqualTo "Encryption"
            loaded.aesBinary?.toUtf8String() shouldBeEqualTo text
            loaded.rc4Binary?.toUtf8String() shouldBeEqualTo text
            loaded.tripleDesBinary?.toUtf8String() shouldBeEqualTo text
        }
    }
}
