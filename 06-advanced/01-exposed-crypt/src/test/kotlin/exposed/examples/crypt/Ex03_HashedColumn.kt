package exposed.examples.crypt

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.crypt.BCryptHasher
import org.jetbrains.exposed.v1.crypt.hash
import org.jetbrains.exposed.v1.crypt.hashed
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * DSL 방식으로 평문을 저장하지 않는 단방향 해시 컬럼을 정의하고 검증하는 예제.
 */
class Ex03_HashedColumn : AbstractExposedTest() {
    companion object {
        private const val PASSWORD = "s3cret"
        private const val WRONG_PASSWORD = "s3cr3t"
        private const val RECOVERY_CODE = "r3covery"
        private const val BCRYPT_STRENGTH = 4

        private val PASSWORD_HASHER = BCryptHasher(strength = BCRYPT_STRENGTH)
    }

    object Users : IntIdTable("hashed_users") {
        val password = text("password").hashed(PASSWORD_HASHER)
        val recoveryCode = varchar("recovery_code", 60).nullable().hashed()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `hashed DSL verifies a password without storing plaintext`(testDB: TestDB) {
        val rawUsers = object : IntIdTable("hashed_users") {
            val password = text("password")
            val recoveryCode = varchar("recovery_code", 60).nullable()
        }

        withTables(testDB, Users) {
            val id =
                Users.insertAndGetId {
                    it[password] = PASSWORD_HASHER.hash(PASSWORD)
                    it[recoveryCode] = Users.recoveryCode.hash(RECOVERY_CODE)
                }
            val nullableId =
                Users.insertAndGetId {
                    it[password] = PASSWORD_HASHER.hash(PASSWORD)
                    it[recoveryCode] = null
                }

            val raw = rawUsers.selectAll().where { rawUsers.id eq id }.single()
            raw[rawUsers.password].contains(PASSWORD).shouldBeFalse()

            val stored = Users.selectAll().where { Users.id eq id }.single()
            stored[Users.password].matches(PASSWORD).shouldBeTrue()
            stored[Users.password].matches(WRONG_PASSWORD).shouldBeFalse()
            stored[Users.recoveryCode]?.matches(RECOVERY_CODE).shouldBeEqualTo(true)

            val nullable = Users.selectAll().where { Users.id eq nullableId }.single()
            nullable[Users.recoveryCode].shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `stored hashed value can be assigned without hashing again`(testDB: TestDB) {
        withTables(testDB, Users) {
            val originalId = Users.insertAndGetId {
                it[password] = PASSWORD_HASHER.hash(PASSWORD)
            }
            val original = Users.selectAll().where { Users.id eq originalId }.single()[Users.password]

            val copiedId = Users.insertAndGetId { it[password] = original }
            val copied = Users.selectAll().where { Users.id eq copiedId }.single()[Users.password]

            copied.encodedValue shouldBeEqualTo original.encodedValue
            copied.matches(PASSWORD).shouldBeTrue()
        }
    }
}
