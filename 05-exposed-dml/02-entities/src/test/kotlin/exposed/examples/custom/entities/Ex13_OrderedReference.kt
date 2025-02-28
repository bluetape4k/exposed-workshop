package exposed.examples.custom.entities

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * one-to-many 관계에서 referrersOn, optionalReferrersOn 함수를 사용하여
 * 참조되는 엔티티 ([SizedIterable]) 들을 정렬하는 방법을 설명합니다.
 */
class Ex13_OrderedReference: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS users (
     *      id SERIAL PRIMARY KEY
     * )
     * ```
     */
    object Users: IntIdTable()

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS userratings (
     *      id SERIAL PRIMARY KEY,
     *      "value" INT NOT NULL,
     *      "user" INT NOT NULL,
     *
     *      CONSTRAINT fk_userratings_user__id FOREIGN KEY ("user")
     *      REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object UserRatings: IntIdTable() {
        val value: Column<Int> = integer("value")
        val user: Column<EntityID<Int>> = reference("user", Users)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS usernullableratings (
     *      id SERIAL PRIMARY KEY,
     *      "value" INT NOT NULL,
     *      "user" INT NULL,
     *
     *      CONSTRAINT fk_usernullableratings_user__id FOREIGN KEY ("user")
     *      REFERENCES users(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * )
     * ```
     */
    object UserNullableRatings: IntIdTable() {
        val value: Column<Int> = integer("value")
        val user: Column<EntityID<Int>?> = reference("user", Users).nullable()
    }

    class UserRatingDefaultOrder(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserRatingDefaultOrder>(UserRatings)

        var value: Int by UserRatings.value
        var user: UserDefaultOrder by UserDefaultOrder referencedOn UserRatings.user

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    class UserNullableRatingDefaultOrder(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserNullableRatingDefaultOrder>(UserNullableRatings)

        var value: Int by UserNullableRatings.value
        var user: UserDefaultOrder? by UserDefaultOrder optionalReferencedOn UserNullableRatings.user

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    class UserDefaultOrder(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserDefaultOrder>(Users)

        /**
         * ratings 을 [UserRatings.value]로 올림차순 정렬합니다.
         */
        val ratings: SizedIterable<UserRatingDefaultOrder>
                by UserRatingDefaultOrder referrersOn
                        UserRatings.user orderBy UserRatings.value

        /**
         * nullableRatings 을 [UserNullableRatings.value]로 올림차순 정렬합니다.
         */
        val nullableRatings: SizedIterable<UserNullableRatingDefaultOrder>
                by UserNullableRatingDefaultOrder optionalReferrersOn
                        UserNullableRatings.user orderBy UserNullableRatings.value

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().toString()
    }

    private val unsortedRatingValues = listOf(0, 3, 1, 2, 4, 4, 5, 4, 5, 6, 9, 8)

    private fun withOrderedReferenceTestTables(testDB: TestDB, statement: Transaction.(TestDB) -> Unit) {
        withTables(testDB, Users, UserRatings, UserNullableRatings) {
            val userId = Users.insertAndGetId {}
            unsortedRatingValues.forEach { value ->
                UserRatings.insert {
                    it[user] = userId
                    it[UserRatings.value] = value
                }
                UserNullableRatings.insert {
                    it[user] = userId
                    it[UserNullableRatings.value] = value
                }
                UserNullableRatings.insert {
                    it[user] = null
                    it[UserNullableRatings.value] = value
                }
            }

            entityCache.clear()

            statement(testDB)
        }
    }

    /**
     * Ratings are ordered by value in ascending order
     *
     * ```sql
     * SELECT userratings.id,
     *        userratings."value",
     *        userratings."user"
     *   FROM userratings
     *  WHERE userratings."user" = 1
     *  ORDER BY userratings."value" ASC;
     * ```
     *
     * Nullable ratings are ordered by value in ascending order
     * and then by id in descending order
     *
     * ```sql
     * SELECT usernullableratings.id,
     *        usernullableratings."value",
     *        usernullableratings."user"
     *   FROM usernullableratings
     *  WHERE usernullableratings."user" = 1
     *  ORDER BY usernullableratings."value" ASC;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `default order`(testDB: TestDB) {
        withOrderedReferenceTestTables(testDB) {
            val user = UserDefaultOrder.all().first()

            unsortedRatingValues
                .sorted()
                .zip(user.ratings)
                .forEach { (value, rating) ->
                    log.debug { "rating: ${rating.value}, value=$value" }
                    rating.value shouldBeEqualTo value
                }

            unsortedRatingValues
                .sorted()
                .zip(user.nullableRatings)
                .forEach { (value, rating) ->
                    log.debug { "rating: ${rating.value}, value=$value" }
                    rating.value shouldBeEqualTo value
                }
        }
    }

    class UserRatingMultiColumn(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserRatingMultiColumn>(UserRatings)

        var value: Int by UserRatings.value
        var user: UserMultiColumn by UserMultiColumn referencedOn UserRatings.user

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    class UserNullableRatingMultiColumn(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserNullableRatingMultiColumn>(UserNullableRatings)

        var value: Int by UserNullableRatings.value
        var user: UserMultiColumn? by UserMultiColumn optionalReferencedOn UserNullableRatings.user

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    class UserMultiColumn(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserMultiColumn>(Users)

        /**
         * ratings 을 [UserRatings.value], [UserRatings.id]로 내림차순 정렬합니다.
         */
        val ratings: SizedIterable<UserRatingMultiColumn> by UserRatingMultiColumn
            .referrersOn(UserRatings.user)
            .orderBy(UserRatings.value to SortOrder.DESC)
            .orderBy(UserRatings.id to SortOrder.DESC)

        /**
         * nullableRatings 을 [UserNullableRatings.value], [UserNullableRatings.id]로 내림차순 정렬합니다.
         */
        val nullableRatings: SizedIterable<UserNullableRatingMultiColumn> by UserNullableRatingMultiColumn
            .optionalReferrersOn(UserNullableRatings.user)
            .orderBy(
                UserNullableRatings.value to SortOrder.DESC,
                UserNullableRatings.id to SortOrder.DESC
            )

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `multi column order`(testDB: TestDB) {
        withOrderedReferenceTestTables(testDB) {
            /**
             * ```sql
             * -- Postgres
             * SELECT users.id FROM users;
             *
             * SELECT userratings.id,
             *        userratings."value",
             *        userratings."user"
             *   FROM userratings
             *  WHERE userratings."user" = 1
             *  ORDER BY userratings."value" DESC,
             *           userratings.id DESC;
             * ```
             */
            val ratings = UserMultiColumn.all().first().ratings.toList()

            /**
             * ```sql
             * -- Postgres
             * SELECT users.id FROM users;
             *
             * SELECT usernullableratings.id,
             *        usernullableratings."value",
             *        usernullableratings."user"
             *   FROM usernullableratings
             *  WHERE usernullableratings."user" = 1
             *  ORDER BY usernullableratings."value" DESC,
             *           usernullableratings.id DESC;
             * ```
             */
            val nullableRatings = UserMultiColumn.all().first().nullableRatings.toList()

            // value 가 내림차순의 순서로 정렬되어야 합니다.
            // value 가 같다면 ID 가 내림차순으로 정렬되어야 합니다.
            fun assertRatingsOrdered(current: UserRatingMultiColumn, prev: UserRatingMultiColumn) {
                current.value shouldBeLessOrEqualTo prev.value
                if (current.value == prev.value) {
                    current.id.value shouldBeLessOrEqualTo prev.id.value
                }
            }

            fun assertNullableRatingsOrdered(
                current: UserNullableRatingMultiColumn,
                prev: UserNullableRatingMultiColumn,
            ) {
                current.value shouldBeLessOrEqualTo prev.value
                if (current.value == prev.value) {
                    current.id.value shouldBeLessOrEqualTo prev.id.value
                }
            }

            for (i in 1 until ratings.size) {
                assertRatingsOrdered(ratings[i], ratings[i - 1])
            }

            for (i in 1 until nullableRatings.size) {
                assertNullableRatingsOrdered(nullableRatings[i], nullableRatings[i - 1])
            }
        }
    }

    class UserRatingChainedColumn(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserRatingChainedColumn>(UserRatings)

        var value: Int by UserRatings.value
        var user: UserChainedColumn by UserChainedColumn referencedOn UserRatings.user

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("value", value)
            .toString()
    }

    class UserChainedColumn(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<UserChainedColumn>(Users)

        /**
         * ratings 을 [UserRatings.value], [UserRatings.id]로 내림차순 정렬합니다.
         */
        val ratings: SizedIterable<UserRatingChainedColumn> by UserRatingChainedColumn
            .referrersOn(UserRatings.user)
            .orderBy(UserRatings.value to SortOrder.DESC)       // value DESC
            .orderBy(UserRatings.id to SortOrder.DESC)          // id DESC

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `chained orderBy`(testDB: TestDB) {
        withOrderedReferenceTestTables(testDB) {
            /**
             * ```sql
             * -- Postgres
             * SELECT USERS.ID FROM USERS;
             *
             * SELECT userratings.id,
             *        userratings."value",
             *        userratings."user"
             *   FROM userratings
             *  WHERE userratings."user" = 1
             *  ORDER BY userratings."value" DESC,
             *           userratings.id DESC;
             * ```
             */
            val ratings = UserChainedColumn.all().first().ratings.toList()

            // value 가 내림차순의 순서로 정렬되어야 합니다.
            // value 가 같다면 ID 가 내림차순으로 정렬되어야 합니다.
            fun assertRatingsOrdered(current: UserRatingChainedColumn, prev: UserRatingChainedColumn) {
                current.value shouldBeLessOrEqualTo prev.value
                if (current.value == prev.value) {
                    current.id.value shouldBeLessOrEqualTo prev.id.value
                }
            }

            for (i in 1 until ratings.size) {
                assertRatingsOrdered(ratings[i], ratings[i - 1])
            }
        }
    }
}
