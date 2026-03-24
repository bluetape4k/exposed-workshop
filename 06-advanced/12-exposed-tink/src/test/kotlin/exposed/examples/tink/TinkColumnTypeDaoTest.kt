package exposed.examples.tink

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.core.tink.tinkDaeadBinary
import io.bluetape4k.exposed.core.tink.tinkDaeadVarChar
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8Bytes
import io.bluetape4k.tink.daead.TinkDaeads
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class TinkColumnTypeDaoTest : AbstractExposedTest() {
    companion object : KLogging()

    /**
     * DAO 암호화 예제 테이블입니다.
     */
    object T1 : IntIdTable() {
        val secret = tinkDaeadVarChar("secret", 255, TinkDaeads.AES256_SIV).index()
        val data = tinkDaeadBinary("data", 512, TinkDaeads.AES256_SIV)
    }

    /**
     * DAO 암호화 예제 엔티티입니다.
     */
    class E1(
        id: EntityID<Int>,
    ) : IntEntity(id) {
        companion object : IntEntityClass<E1>(T1)

        var secret by T1.secret
        var data by T1.data

        override fun equals(other: Any?): Boolean = idEquals(other)

        override fun hashCode(): Int = idHashCode()

        override fun toString(): String =
            entityToStringBuilder()
                .add(E1::secret.name, secret)
                .add(E1::data.name, data)
                .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO 방식의 컬럼 값 암호화`(testDB: TestDB) {
        withTables(testDB, T1) {
            val insertedSecret = faker.name().firstName()
            val insertedData = faker.address().fullAddress().toUtf8Bytes()

            val entity =
                E1.new {
                    secret = insertedSecret
                    data = insertedData
                }

            entityCache.clear()

            val saved = E1.findById(entity.id).shouldNotBeNull()
            E1.findById(-1).shouldBeNull()

            saved.secret shouldBeEqualTo insertedSecret
            saved.data shouldBeEqualTo insertedData

            T1.selectAll().count() shouldBeEqualTo 1L

            val row = T1.selectAll().where { T1.id eq entity.id }.single()

            row[T1.secret] shouldBeEqualTo insertedSecret
            row[T1.data] shouldBeEqualTo insertedData
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `암호화된 컬럼으로 검색하기`(testDB: TestDB) {
        withTables(testDB, T1) {
            val insertedSecret = faker.name().firstName()
            val insertedData = faker.address().fullAddress().toUtf8Bytes()

            val entity =
                E1.new {
                    secret = insertedSecret
                    data = insertedData
                }
            entity.id.value.shouldNotBeNull()
            entityCache.clear()

            /**
             * Tink DAEAD(결정적 AEAD) 암호화는 항상 같은 결과를 반환하므로, WHERE 절로 검색이 가능합니다.
             * ```sql
             * -- Postgres
             * SELECT t1.id, t1."varchar", t1."binary" FROM t1 WHERE t1."varchar" = xHJZumy4xB5idgnKqmp2pQ==
             * ```
             */
            T1.selectAll().where { T1.secret eq insertedSecret }.single().let {
                it[T1.secret] shouldBeEqualTo insertedSecret
                it[T1.data] shouldBeEqualTo insertedData
            }
            E1.find { T1.secret eq insertedSecret }.single().let {
                it.secret shouldBeEqualTo insertedSecret
                it.data shouldBeEqualTo insertedData
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT t1.id, t1."varchar", t1."binary" FROM t1 WHERE t1."binary" = [B@20040c6e
             * ```
             */

            T1.selectAll().where { T1.data eq insertedData }.single().let {
                it[T1.secret] shouldBeEqualTo insertedSecret
                it[T1.data] shouldBeEqualTo insertedData
            }
        }
    }

    @Test
    @Disabled("Exposed 1.1.x double-decryption bug - re-enable after version upgrade")
    fun `암호화된 data 컬럼으로 DAO 검색하기`() {
        withTables(TestDB.H2_V2, T1) {
            val insertedSecret = faker.name().firstName()
            val insertedData = faker.address().fullAddress().toUtf8Bytes()

            val entity = E1.new {
                secret = insertedSecret
                data = insertedData
            }
            entity.id.value
            entityCache.clear()

            E1.find { T1.data eq insertedData }.single().let {
                it.secret shouldBeEqualTo insertedSecret
                it.data shouldBeEqualTo insertedData
            }
        }
    }
}
