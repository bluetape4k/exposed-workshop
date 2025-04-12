package exposed.examples.crypt

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.crypt.Algorithms
import org.jetbrains.exposed.crypt.encryptedBinary
import org.jetbrains.exposed.crypt.encryptedVarchar
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex02_EncryptedColumnWithEntity: AbstractExposedTest() {

    companion object: KLogging() {
        private val encryptor = Algorithms.AES_256_PBE_GCM("passwd", "12345678")
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS test (
     *      id SERIAL PRIMARY KEY,
     *      "varchar" VARCHAR(100) NOT NULL,
     *      "binary" bytea NOT NULL
     * )
     * ```
     */
    object TestTable: IntIdTable() {
        val varchar = encryptedVarchar("varchar", 100, encryptor)
        val binary = encryptedBinary("binary", 100, encryptor)
    }

    class ETest(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<ETest>(TestTable)

        var varchar: String by TestTable.varchar
        var binary: ByteArray by TestTable.binary

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("varchar", varchar)
            .add("binary", binary.contentToString())
            .toString()
    }

    /**
     * 암호화된 컬럼에 문자열과 ByteArray 값을 저장하기
     *
     * ```sql
     * -- Postgres
     * INSERT INTO test ("varchar", "binary")
     * VALUES (BkaUzt6JpQIeylblnFquTYZUsMwxBwrplpqu/fUGdiL1xLfBzDYQ, [B@10a0d93a)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `encrypted columns with DAO`(testDB: TestDB) {
        withTables(testDB, TestTable) {
            val varcharValue = "varchar"
            val binaryValue = "binary".toByteArray()

            val entity = ETest.new {
                varchar = varcharValue
                binary = binaryValue
            }

            entityCache.clear()

            entity.varchar shouldBeEqualTo varcharValue
            entity.binary shouldBeEqualTo binaryValue

            // Entity를 통해 조회
            ETest.all().first().let {
                it.varchar shouldBeEqualTo varcharValue
                it.binary shouldBeEqualTo binaryValue
            }

            // DSL을 통해 조회
            TestTable.selectAll().first().let {
                it[TestTable.varchar] shouldBeEqualTo varcharValue
                it[TestTable.binary] shouldBeEqualTo binaryValue
            }
        }
    }

    /**
     * find by encrypted value
     *
     * Exposed Encryptor는 매번 다른 값으로 암호화하기 때문에, WHERE 절에 쓸 수는 없습니다.
     * Jasypt 를 사용해서 매번 같은 값으로 암호화를 하도록 하면 가능합니다.
     *
     * 참고: [Exposed Crypt Module](https://debop.notion.site/Exposed-Crypt-Module-1c32744526b0802da419d5ce74d2c5f3)
     *
     * ```sql
     * INSERT INTO TEST ("varchar", "binary")
     * VALUES (UagVRR403hrjcUmKvA3j/Zs43+2UjmcC4XJl7DoaiWuktd2SHYKr, [B@31f295b6)
     * ```
     */
    @Disabled("Exposed Encryptor는 매번 다른 값으로 암호화하기 때문에, WHERE 절에 쓸 수는 없습니다.")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `find by encrypted value`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB == TestDB.H2 }

        withTables(testDB, TestTable) {
            val varcharValue = "varchar"
            val binaryValue = "binary".toByteArray()

            ETest.new {
                varchar = varcharValue
                binary = binaryValue
            }

            entityCache.clear()

            // Hibernate 처럼 암호화된 컬럼으로 검색이 불가능합니다.
            ETest.find { TestTable.varchar eq varcharValue }.first().let {
                it.varchar shouldBeEqualTo varcharValue
                it.binary shouldBeEqualTo binaryValue
            }
        }
    }
}
