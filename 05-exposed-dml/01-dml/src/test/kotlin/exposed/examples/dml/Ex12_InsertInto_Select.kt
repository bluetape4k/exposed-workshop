package exposed.examples.dml

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Random
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.VarCharColumnType
import org.jetbrains.exposed.sql.autoIncColumnType
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.intParam
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.stringParam
import org.jetbrains.exposed.sql.substring
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

/**
 * `INSERT INTO ... SELECT ... FROM ...` 구문 예제 모음
 */
class Ex12_InsertInto_Select: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * Users 테이블에서 일부 컬럼을 선택하여 Cities 테이블에 추가하는 예제
     *
     * ```sql
     * -- Postgres
     * INSERT INTO cities ("name")
     * SELECT SUBSTRING(users."name", 1, 2)
     *   FROM users
     *  ORDER BY users.id ASC
     *  LIMIT 2
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert select example 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->
            val nextVal = cities.id.autoIncColumnType?.nextValExpression
            val substring = users.name.substring(1, 2)
            val slice = listOfNotNull(nextVal, substring)
            val limit = 2

            // users 테이블에서 2개의 city name을 선택하여 cities 테이블에 추가
            cities.insert(users.select(slice).orderBy(users.id).limit(limit))

            // 최근 추가된 2개의 city name 조회 
            val rows = cities
                .select(cities.name)
                .orderBy(cities.id, SortOrder.DESC)
                .limit(limit)
                .toList()

            rows shouldHaveSize 2
            rows[0][cities.name] shouldBeEqualTo "An"   // Andrey
            rows[1][cities.name] shouldBeEqualTo "Al"   // Alex
        }
    }

    /**
     * 같은 테이블에 대해 `INSERT INTO ... SELECT ... FROM ...` 구문을 사용하는 예제
     *
     * ```sql
     * -- Postgres
     * INSERT INTO userdata (user_id, "comment", "value")
     * SELECT userdata.user_id, userdata."comment", 42
     *   FROM userdata
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert select example 02`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, _, userData ->
            // 원본 데이터 개수
            val allUserData = userData.selectAll().count()

            userData.insert(
                userData.select(userData.userId, userData.comment, intParam(42))
            )

            // 새롭게 추가된 데이터 조회 (value = 42)
            val rows = userData.selectAll()
                .where { userData.value eq 42 }
                .orderBy(userData.userId)
                .toList()

            rows.size shouldBeEqualTo allUserData.toInt()
        }
    }

    /**
     * Expression 을 사용하여 `INSERT INTO ... SELECT ... FROM ...` 구문을 사용하는 예제
     *
     * ```sql
     * -- Postgres
     * INSERT INTO users (id, "name", city_id, flags)
     * SELECT SUBSTRING(CAST(RANDOM() AS VARCHAR(255)), 1, 10), 'Foo', 1, 0
     *   FROM users
     * ```
     */
    @Suppress("UNCHECKED_CAST")
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert select example 03`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            // 이렇게 Expresssion 을 사용할 수 있습니다.
            // Random() 은 org.jetbrains.exposed.sql.Random() 이다.
            val userCount = users.selectAll().count()
            val nullableExpression: Expression<BigDecimal?> = Random() as Expression<BigDecimal?>

            users.insert(
                users.select(
                    nullableExpression.castTo(VarCharColumnType()).substring(1, 10),
                    stringParam("Foo"),
                    intParam(1),
                    intLiteral(0)
                )
            )
            val rows = users.selectAll().where { users.name eq "Foo" }.toList()
            rows.size.toLong() shouldBeEqualTo userCount
        }
    }

    /**
     * Expression 을 사용하여 특정 컬럼 값만 `INSERT INTO ... SELECT ... FROM ...` 구문을 사용하는 예제
     *
     * ```sql
     * -- Postgres
     * INSERT INTO users ("name", id)
     * SELECT 'Foo', SUBSTRING(CAST(RANDOM() AS VARCHAR(255)), 1, 10)
     *   FROM users
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert select example 04`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val userCount = users.selectAll().count()

            users.insert(
                users.select(
                    stringParam("Foo"),
                    Random().castTo(VarCharColumnType()).substring(1, 10)
                ),
                columns = listOf(users.name, users.id)
            )

            val rows = users.selectAll().where { users.name eq "Foo" }.toList()
            rows.size shouldBeEqualTo userCount.toInt()
        }
    }

    /**
     * INSERT INTO ... SELECT ... FROM ... 구문에서 INSERT 할 컬럼을 지정하고, 행의 갯수도 LIMIT으로 제한하는 예제
     *
     * ```sql
     * -- Postgres
     * INSERT INTO users ("name", id)
     * SELECT 'Foo', 'Foo'
     *   FROM users
     *  LIMIT 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert-select with same columns in a query`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val fooParam = stringParam("Foo")

            users.insert(
                users.select(fooParam, fooParam).limit(1),
                columns = listOf(users.name, users.id)
            )

            users.selectAll()
                .where { users.name eq "Foo" }
                .count() shouldBeEqualTo 1L
        }
    }
}
