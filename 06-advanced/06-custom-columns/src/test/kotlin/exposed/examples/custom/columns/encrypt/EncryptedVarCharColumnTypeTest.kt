package exposed.examples.custom.columns.encrypt

import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.crypto.encrypt.Encryptors
import io.bluetape4k.exposed.core.encrypt.encryptedVarChar
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Column
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

class EncryptedVarCharColumnTypeTest: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS t1 (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(50) NOT NULL,
     *      aes_str VARCHAR(1024) NULL,
     *      rc4_str VARCHAR(1024) NULL,
     *      triple_des_str VARCHAR(1024) NULL
     * );
     * ```
     *
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS T1 (
     *      id INT AUTO_INCREMENT PRIMARY KEY,
     *      `name` VARCHAR(50) NOT NULL,
     *      aes_str VARCHAR(1024) NULL,
     *      rc4_str VARCHAR(1024) NULL,
     *      triple_des_str VARCHAR(1024) NULL
     * );
     * ```
     */
    private object T1: IntIdTable("T1") {
        val name = varchar("name", 50)

        val aesString: Column<String?> = encryptedVarChar("aes_str", 1024, Encryptors.AES).nullable()
        val rc4String: Column<String?> = encryptedVarChar("rc4_str", 1024, Encryptors.RC4).nullable()
        val tripleDesString: Column<String?> =
            encryptedVarChar("triple_des_str", 1024, Encryptors.TripleDES).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var name by T1.name

        var aesString: String? by T1.aesString
        var rc4String: String? by T1.rc4String
        var tripleDesString: String? by T1.tripleDesString

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("aesString", aesString)
            .add("rc4String", rc4String)
            .add("tripleDesString", tripleDesString)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `필드 값을을 암호화하여 VarChar 컬럼에 저장합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = Fakers.randomString(255, 512)

            val id = T1.insertAndGetId {
                it[T1.name] = "Encryption"

                it[T1.aesString] = text
                it[T1.rc4String] = text
                it[T1.tripleDesString] = text
            }

            entityCache.clear()

            val row = T1.selectAll().where { T1.id eq id }.single()

            row[T1.name] shouldBeEqualTo "Encryption"
            row[T1.aesString] shouldBeEqualTo text
            row[T1.rc4String] shouldBeEqualTo text
            row[T1.tripleDesString] shouldBeEqualTo text
        }
    }

    /**
     * exposed-crypt 랑 달리 암호화된 값을 그대로 비교합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT t1.id, t1."name", t1.aes_str, t1.rc4_str, t1.triple_des_str
     *   FROM t1
     *  WHERE t1.rc4_str = r4YnE6KiXJTMrB4S0qK-jmVXPKer7d1eJagtKd5LJX2DmOHiCo9LyrVYtZVXy2gG6lx47w1S2WC5vOkq;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `암호화 컬럼으로 검색하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = "동해물과 백두산이 마르고 닳도록"

            val id = T1.insertAndGetId {
                it[T1.name] = "Encryption"

                it[T1.aesString] = text
                it[T1.rc4String] = text
                it[T1.tripleDesString] = text
            }
            id._value.shouldNotBeNull()

            entityCache.clear()

            // exposed-crypt 랑 달리 암호화된 값을 그대로 비교합니다.
            val row = T1.selectAll().where { T1.rc4String eq text }.single()

            row[T1.name] shouldBeEqualTo "Encryption"
            row[T1.aesString] shouldBeEqualTo text
            row[T1.rc4String] shouldBeEqualTo text
            row[T1.tripleDesString] shouldBeEqualTo text
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `엔티티 속성 값을을 암호화하여 VarChar 컬럼에 저장합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = Fakers.randomString(255, 512)

            val e1 = E1.new {
                name = "Encryption"

                aesString = text
                rc4String = text
                tripleDesString = text
            }

            entityCache.clear()

            val loaded = E1.findById(e1.id)!!

            loaded.name shouldBeEqualTo "Encryption"
            loaded.aesString shouldBeEqualTo text
            loaded.rc4String shouldBeEqualTo text
            loaded.tripleDesString shouldBeEqualTo text
        }
    }

    /**
     * exposed-crypt 랑 달리 암호화된 값을 그대로 비교합니다.
     *
     * ```sql
     * -- Postgres
     * SELECT t1.id, t1."name", t1.aes_str, t1.rc4_str, t1.triple_des_str
     *   FROM t1
     *  WHERE t1.aes_str = t05Obf7QpRrKyocCsapQzpvYwQ_btvMx0YwC8nWYr_w=
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `암호화한 컬럼 값을 기준으로 검색합니다`(testDB: TestDB) {
        withTables(testDB, T1) {
            val text = "동해물과 백두산이 마르고 닳도록"

            val e1 = E1.new {
                name = "Encryption"

                aesString = text
                rc4String = text
                tripleDesString = text
            }

            entityCache.clear()

            val loaded = E1.find { T1.aesString eq e1.aesString }.single()

            loaded.name shouldBeEqualTo "Encryption"
            loaded.aesString shouldBeEqualTo text
            loaded.rc4String shouldBeEqualTo text
            loaded.tripleDesString shouldBeEqualTo text
        }
    }
}
