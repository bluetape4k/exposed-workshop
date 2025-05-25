package exposed.examples.jpa.ex01_joins

import exposed.shared.mapping.withPersonsAndAddress
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.QueryAlias
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.innerJoin
import org.jetbrains.exposed.v1.jdbc.select
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex06_Convering_Index: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * Covering Index 를 사용하는 Subquery 와 Inner Join 하기
     *
     * 이를 위해 city, zip, id로 index를 구성했습니다.
     *
     * ```sql
     * -- Postgres
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *   INNER JOIN (
     *      SELECT persons.id
     *                 FROM persons INNER JOIN addresses ON addresses.id = persons.address_id
     *                WHERE addresses.city = 'Seoul'
     *   ) p2 ON (persons.id = p2.id)
     *
     * -- MySQL V8
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id,
     *   FROM persons
     *  INNER JOIN (
     *      SELECT persons.id
     *        FROM persons
     *       INNER JOIN addresses ON addresses.id = persons.address_id
     *       WHERE addresses.city = 'Seoul'
     *  ) p2 ON  (persons.id = p2.id)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `covering index`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB }

        // 서울 사는 사람들을 id 를 조회
        withPersonsAndAddress(testDB) { persons, addrs ->
            // convering index 에 해당하는 subquery
            val indexQuery: QueryAlias = persons
                .innerJoin(addrs)
                .select(persons.id)
                .where { addrs.city eq "Seoul" }
                .alias("p2")

            // subquery 를 inner join 하여 해당 id의 사람들을 조회
            val rows = persons
                .innerJoin(indexQuery) { persons.id eq indexQuery[persons.id] }
                .select(persons.columns)
                .toList()

            rows.forEach {
                log.debug { it }
            }

            rows shouldHaveSize 2
        }
    }

    /**
     * Covering Index 를 사용하는 Subquery 와 Inner Join 하기
     *
     * 이를 위해 city, zip, id로 index를 구성했습니다.
     *
     * ```sql
     * -- Postgres
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *   INNER JOIN (
     *      SELECT persons.id
     *                 FROM persons INNER JOIN addresses ON addresses.id = persons.address_id
     *                WHERE addresses.city = 'Seoul'
     *   ) p2 ON (persons.id = p2.id)
     *  WHERE persons.id < 100
     *
     * -- MySQL V8
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id,
     *   FROM persons
     *  INNER JOIN (
     *      SELECT persons.id
     *        FROM persons
     *       INNER JOIN addresses ON addresses.id = persons.address_id
     *       WHERE addresses.city = 'Seoul'
     *  ) p2 ON  (persons.id = p2.id)
     *  WHERE persons.id < 100
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `subquery in join`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB }

        withPersonsAndAddress(testDB) { persons, addrs ->
            // Subquery 용 alias
            // convering index 에 해당하는 subquery
            val indexQuery: QueryAlias = persons
                .innerJoin(addrs)
                .select(persons.id)
                .where { addrs.city eq "Seoul" }
                .alias("p2")

            // subquery 를 inner join 하여 해당 id의 사람들을 조회
            val rows = persons
                .innerJoin(indexQuery) { persons.id eq indexQuery[persons.id] }
                .select(persons.columns)
                .where { persons.id less 100L }
                .toList()

            rows.forEach {
                log.debug { it }
            }
            rows shouldHaveSize 2
        }
    }
}
