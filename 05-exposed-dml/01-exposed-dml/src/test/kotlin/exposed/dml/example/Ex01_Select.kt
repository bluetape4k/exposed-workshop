package exposed.dml.example

import exposed.shared.dml.DMLTestData.withCitiesAndUsers
import exposed.shared.entities.BoardSchema.Board
import exposed.shared.entities.BoardSchema.Boards
import exposed.shared.entities.BoardSchema.Categories
import exposed.shared.entities.BoardSchema.Category
import exposed.shared.entities.BoardSchema.Post
import exposed.shared.entities.BoardSchema.Posts
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotContain
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex01_Select: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * 간단한 조건절을 가진 SELECT 문
     *
     * ```sql
     * -- Postgres
     * SELECT users.id,
     *        users."name",
     *        users.city_id,
     *        users.flags
     *   FROM users
     *  WHERE users.id = 'andrey'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SELECT ALL - 하나의 조건`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val row = users.selectAll()
                .where { users.id eq "andrey" }
                .single()

            val userId = row[users.id]
            val userName = row[users.name]
            log.debug { "User id: $userId, name: $userName" }
            userName shouldBeEqualTo "Andrey"
        }
    }

    /**
     * WHERE 조건들이 `AND` 로 연결된 경우
     *
     * ```sql
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE (users.id = 'andrey')
     *    AND (users."name" IS NOT NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SELECT ALL - 복수의 AND 조건`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val row = users.selectAll()
                .where { users.id eq "andrey" }
                .andWhere { users.name.isNotNull() }
                .single()

            val userId = row[users.id]
            val userName = row[users.name]
            log.debug { "User id: $userId, name: $userName" }
            userName shouldBeEqualTo "Andrey"

            val row2 = users.selectAll()
                .where { (users.id eq "andrey") and (users.name.isNotNull()) }   // where 함수 내에서 조합해도 된다
                .single()

            val userId2 = row2[users.id]
            val userName2 = row2[users.name]
            log.debug { "User id: $userId2, name: $userName2" }
            userName2 shouldBeEqualTo "Andrey"
        }
    }

    /**
     * WHERE 조건들이 `OR` 로 연결된 경우
     *
     * ```sql
     * -- Postgres
     * SELECT users.id,
     *        users."name",
     *        users.city_id,
     *        users.flags
     *   FROM users
     *  WHERE (users.id = 'andrey')
     *     OR (users."name" = 'Andrey')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SELECT ALL - 복수 조건이 OR 인 경우`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val row = users.selectAll()
                .where { users.id.eq("andrey") or users.name.eq("Andrey") } // orWhere 를 써도 된다.
                .single()

            val userId = row[users.id]
            val userName = row[users.name]
            log.debug { "User id: $userId, name: $userName" }
            userName shouldBeEqualTo "Andrey"
        }
    }

    /**
     * WHERE 조건에 NOT EQUAL에 해당하는 `<>` 연산자 사용
     *
     * ```sql
     * -- Postgres
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE users.id <> 'andrey'
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `조건절에 not equal 사용`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll()
                .where { users.id neq "andrey" }
                .toList()

            rows.map { it[users.id] } shouldNotContain "andrey"
        }
    }


    /**
     * [SizedIterable] 을 사용한 SELECT 문
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `SizedIterable 수형에 해당하는 쿼리 실행`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, users, _ ->

            // SELECT cities.city_id, cities."name" FROM cities
            cities.selectAll().shouldNotBeEmpty()

            // SELECT cities.city_id, cities."name" FROM cities WHERE cities."name" = 'Qwertt'
            cities.selectAll()
                .where { cities.name eq "Qwertt" }.shouldBeEmpty()

            // SELECT COUNT(*) FROM cities WHERE cities."name" = 'Qwertt'
            cities.selectAll()
                .where { cities.name eq "Qwertt" }
                .count() shouldBeEqualTo 0L

            // SELECT COUNT(*) FROM cities
            cities.selectAll().count() shouldBeEqualTo 3L

            val cityId: Int? = null

            // SELECT COUNT(*) FROM users WHERE users.city_id IS NULL
            users.selectAll()
                .where { users.cityId eq cityId }
                .count() shouldBeEqualTo 2L   // isNull() 을 사용해도 된다.
        }
    }

    /**
     * `inList`, `notInList` 를 사용한 조회
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with single expression 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            /**
             * ```sql
             * SELECT users.id, users."name", users.city_id, users.flags
             *   FROM users
             *  WHERE users.id IN ('andrey', 'alex')
             *  ORDER BY users."name" ASC
             * ```
             */
            val r1 = users
                .selectAll()
                .where { users.id inList listOf("andrey", "alex") }
                .orderBy(users.name)
                .toList()

            r1.size shouldBeEqualTo 2
            r1[0][users.name] shouldBeEqualTo "Alex"
            r1[1][users.name] shouldBeEqualTo "Andrey"

            /**
             * ```sql
             * SELECT users.id, users."name", users.city_id, users.flags
             *   FROM users
             *  WHERE users.id NOT IN ('ABC', 'DEF')
             * ```
             */
            val r2 = users.selectAll()
                .where { users.id notInList listOf("ABC", "DEF") }
                .toList()

            users.selectAll().count() shouldBeEqualTo r2.size.toLong()
        }
    }

    /**
     * `inList` 에 Pair 형식으로 사용하기
     *
     * ```sql
     * SELECT users.id, users."name", users.city_id, users.flags
     *   FROM users
     *  WHERE (users.id, users."name") IN (('andrey', 'Andrey'), ('sergey', 'Sergey'))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with pair expression 02`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { _, users, _ ->
            val rows = users.selectAll()
                .where {
                    (users.id to users.name) inList listOf("andrey" to "Andrey", "sergey" to "Sergey")
                }
                .toList()

            rows shouldHaveSize 2
            rows[0][users.name] shouldBeEqualTo "Andrey"
            rows[1][users.name] shouldBeEqualTo "Sergey"
        }
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inList with entityID columns`(testDB: TestDB) {
        withTables(testDB, Posts, Boards, Categories) {
            val board1 = Board.new {
                name = "board1"
            }
            val post1 = Post.new {
                board = board1
            }
            Post.new {
                category = Category.new { title = "category1" }
            }

            /**
             * ```sql
             * SELECT posts.id, posts.board, posts.parent, posts.category, posts."optCategory"
             *   FROM posts
             *  WHERE posts.board = 1
             * ```
             */
            val result1 = Posts
                .selectAll()
                .where {
                    Posts.boardId inList listOf(board1.id)   // 항목이 한개라면 `eq` 로 대체 가능
                }
                .singleOrNull()
                ?.get(Posts.id)

            result1 shouldBeEqualTo post1.id

            /**
             * `inList` with `EntityID` columns
             *
             * ```sql
             * SELECT board.id, board."name"
             *   FROM board
             *  WHERE board.id IN (1, 2, 3, 4, 5)
             * ```
             */
            val result2 = Board.find {
                Boards.id inList listOf(1, 2, 3, 4, 5)
            }.singleOrNull()
            result2 shouldBeEqualTo board1

            /**
             * `notInList` with entityID columns
             *
             * ```sql
             * SELECT board.id, board."name"
             *   FROM board
             *  WHERE board.id  NOT IN (1, 2, 3, 4, 5)
             * ```
             */
            val result3 = Board.find {
                Boards.id notInList listOf(1, 2, 3, 4, 5)
            }.singleOrNull()
            result3.shouldBeNull()
        }
    }

    /**
     * `inSubQuery` 연산자 예제
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM cities
     *  WHERE cities.city_id IN (SELECT cities.city_id
     *                             FROM cities
     *                            WHERE cities.city_id = 2)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inSubQuery 연산자 예제 - 01`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val subQuery: Query = cities.select(cities.id).where { cities.id eq 2 }

            val r: Query = cities
                .selectAll()
                .where { cities.id inSubQuery subQuery }

            r.count() shouldBeEqualTo 1L
        }
    }

    /**
     * `notInSubQuery` 연산자 사용
     *
     * ```sql
     * -- Postgres
     * SELECT COUNT(*)
     *   FROM cities
     *  WHERE cities.city_id NOT IN (SELECT cities.city_id FROM cities)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `notInSubQuery 연산자 예제`(testDB: TestDB) {
        withCitiesAndUsers(testDB) { cities, _, _ ->
            val subQuery: Query = cities.select(cities.id)

            val r: Query = cities
                .selectAll()
                .where { cities.id notInSubQuery subQuery }

            r.count() shouldBeEqualTo 0L
        }
    }
}
