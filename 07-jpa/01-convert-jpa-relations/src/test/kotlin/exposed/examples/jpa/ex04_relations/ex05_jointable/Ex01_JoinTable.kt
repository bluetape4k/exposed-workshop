package exposed.examples.jpa.ex04_relations.ex05_jointable

import exposed.examples.jpa.ex04_relations.ex05_jointable.JoinSchema.User
import exposed.examples.jpa.ex04_relations.ex05_jointable.JoinSchema.newUser
import exposed.examples.jpa.ex04_relations.ex05_jointable.JoinSchema.withJoinSchema
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.SchemaUtils
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * JPA 의 one-to-many 관계를 @JoinTable 방식을 Exposed 로 구현 한 예
 */
class Ex01_JoinTable: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create join schema`(testDB: TestDB) {
        withDb(testDB) {
            try {
                SchemaUtils.create(*JoinSchema.allTables)
            } finally {
                SchemaUtils.drop(*JoinSchema.allTables)
            }
        }
    }

    /**
     * @JoinTable 을 이용해 연관관계를 설정한 경우, 연관된 엔티티를 로딩할 때, join table을 사용한다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create user with address by join table`(testDB: TestDB) {
        withJoinSchema(testDB) {
            val user = newUser()
            entityCache.clear()

            /**
             * Eager loading with `load(User::addresses)`
             *
             * ```sql
             * -- Postgres
             *
             * SELECT join_user.id,
             *        join_user."name"
             *   FROM join_user
             *  WHERE join_user.id = 1;
             *
             * -- eager loading
             * SELECT join_address.id,
             *        join_address.street,
             *        join_address.city,
             *        join_address.zipcode,
             *        user_address.id,
             *        user_address.user_id,
             *        user_address.address_id,
             *        user_address.addr_type
             *   FROM join_address
             *          INNER JOIN user_address ON user_address.address_id = join_address.id
             *  WHERE user_address.user_id = 1
             * ```
             */
            val loaded = User.findById(user.id)!!.load(User::addresses)

            loaded.addresses.count() shouldBeEqualTo 2

            loaded.addresses.forEach { addr ->
                log.debug { "Address: $addr" }
            }

            /**
             * ```sql
             * -- Postgres
             * SELECT user_address.id,
             *        user_address.user_id,
             *        user_address.address_id,
             *        user_address.addr_type
             *   FROM user_address
             *  WHERE user_address.user_id = 1
             *  ORDER BY user_address.addr_type ASC
             * ```
             */
            loaded.userAddresses.forEach { userAddr ->
                log.debug { "UserAddress: $userAddr, ${userAddr.user}, ${userAddr.address}" }
            }
        }
    }
}
