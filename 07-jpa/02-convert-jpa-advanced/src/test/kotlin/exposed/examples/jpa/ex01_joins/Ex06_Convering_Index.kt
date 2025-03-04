package exposed.examples.jpa.ex01_joins

import exposed.shared.mapping.withPersonsAndAddress
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex06_Convering_Index: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * ```sql
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id,
     *        p2.id
     *   FROM persons
     *      INNER JOIN (SELECT persons.id
     *                    FROM persons
     *                   WHERE persons.address_id = 2
     *      ) p2 ON  (persons.id = p2.id)
     *  WHERE persons.id < 5
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `covering index`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB }
        
        withPersonsAndAddress(testDB) { persons, _ ->
            // convering index 에 해당하는 subquery
            val p2 = persons
                .select(persons.id)
                .where { persons.addressId eq 2L }
                .alias("p2")

            val rows = persons
                .innerJoin(p2) { persons.id eq p2[persons.id] }
                .selectAll()
                .where { persons.id less 5L }
                .toList()

            rows shouldHaveSize 1
            rows.single()[persons.id].value shouldBeEqualTo 4L
        }
    }

    /**
     * Subquery 와 Inner Join 하기 (Covering Index)
     *
     * ```sql
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id,
     *        p2.id
     *   FROM persons
     *      INNER JOIN (SELECT persons.id
     *                    FROM persons
     *                   WHERE persons.address_id = 2
     *                   ORDER BY persons.id ASC
     *      ) p2 ON (persons.id = p2.id)
     *   WHERE persons.id < 5
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `subquery in join`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MARIADB }
        
        withPersonsAndAddress(testDB) { persons, _ ->
            // Subquery 용 alias
            val p2 = persons.select(persons.id)
                .where { persons.addressId eq 2L }
                .orderBy(persons.id)
                .alias("p2")

            val rows = persons
                .innerJoin(p2) { persons.id eq p2[persons.id] }
                .selectAll()
                .where { persons.id less 5L }
                .toList()

            rows.forEach {
                log.debug { it }
            }
            rows shouldHaveSize 1
            rows.single()[persons.id].value shouldBeEqualTo 4L
        }
    }
}
