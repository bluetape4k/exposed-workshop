package exposed.examples.crypt

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.entityToStringBuilder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.crypt.BCryptHasher
import org.jetbrains.exposed.v1.crypt.hash
import org.jetbrains.exposed.v1.crypt.hashed
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * DAO(Entity) 방식으로 단방향 해시 컬럼을 저장하고 검증하는 예제.
 */
class Ex04_HashedColumnWithEntity : AbstractExposedTest() {
    companion object {
        private const val PASSWORD = "s3cret"
        private const val WRONG_PASSWORD = "s3cr3t"
        private const val RECOVERY_CODE = "r3covery"
        private const val BCRYPT_STRENGTH = 4

        private val hasher = BCryptHasher(strength = BCRYPT_STRENGTH)
    }

    object Users : IntIdTable("hashed_dao_users") {
        val password = text("password").hashed(hasher)
        val recoveryCode = varchar("recovery_code", 60).nullable().hashed(hasher)
    }

    class UserEntity(
        id: EntityID<Int>,
    ) : IntEntity(id) {
        companion object : IntEntityClass<UserEntity>(Users)

        var password by Users.password
        var recoveryCode by Users.recoveryCode

        override fun equals(other: Any?): Boolean = idEquals(other)

        override fun hashCode(): Int = idHashCode()

        override fun toString(): String =
            entityToStringBuilder()
                .add("password", password)
                .add("recoveryCode", recoveryCode)
                .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `hashed columns are available through DAO entities`(testDB: TestDB) {
        withTables(testDB, Users) {
            val created =
                UserEntity.new {
                    password = Users.password.hash(PASSWORD)
                    recoveryCode = Users.recoveryCode.hash(RECOVERY_CODE)
                }
            val id = created.id

            entityCache.clear()
            val loaded = UserEntity.findById(id) ?: error("hashed user was not stored")

            loaded.password.matches(PASSWORD).shouldBeTrue()
            loaded.password.matches(WRONG_PASSWORD).shouldBeFalse()
            loaded.recoveryCode?.matches(RECOVERY_CODE).shouldBeEqualTo(true)

            loaded.recoveryCode = null
            entityCache.clear()
            val withoutRecoveryCode = UserEntity.findById(id) ?: error("hashed user was not stored")
            withoutRecoveryCode.recoveryCode.shouldBeNull()
        }
    }
}
