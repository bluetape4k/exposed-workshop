package exposed.examples.jpa.ex02_subquery

import exposed.shared.mapping.PersonSchema.Person
import exposed.shared.mapping.withPersonsAndAddress
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.asLong
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.sql.CustomOperator
import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.IntegerColumnType
import org.jetbrains.exposed.sql.LongColumnType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.intLiteral
import org.jetbrains.exposed.sql.longLiteral
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.vendors.MariaDBDialect
import org.jetbrains.exposed.sql.vendors.currentDialect
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex01_SubQuery: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * `notEqSubQuery` 를 이용한 예제
     * ```sql
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  WHERE persons.id != (SELECT MAX(persons.id) FROM persons);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select notEqSubQuery`(testDB: TestDB) {
        withPersonsAndAddress(testDB) { persons, _ ->
            val query = persons
                .selectAll()
                .where {
                    persons.id notEqSubQuery persons.select(persons.id.max())
                }

            val rows = query.toList()
            rows shouldHaveSize 5

            // Query를 Entity로 만들기
            val personEntities = Person.wrapRows(query).toList()
            personEntities.forEach { person ->
                log.debug { "person: $person" }
            }
        }
    }

    /**
     * ```sql
     * -- Postgres:
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  WHERE persons.id = (SELECT MAX(persons.id) FROM persons)
     * ```
     * ```sql
     * -- MySQL V8:
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  WHERE persons.id = (SELECT MAX(persons.id) FROM persons)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select eqSubQuery`(testDB: TestDB) {
        withPersonsAndAddress(testDB) { persons, _ ->
            val query = persons
                .selectAll()
                .where {
                    persons.id eqSubQuery persons.select(persons.id.max())
                }

            val rows = query.toList()
            rows shouldHaveSize 1

            // Query를 Entity로 만들기
            val personEntities = Person.wrapRows(query).toList()
            personEntities.forEach { person ->
                log.debug { "person: $person" }
            }

            val expectedId = if (currentDialect is MariaDBDialect) 8L else 6L
            personEntities.single().id.value shouldBeEqualTo expectedId
        }
    }

    /**
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
     *  WHERE persons.id IN (SELECT persons.id
     *                         FROM persons
     *                        WHERE persons.last_name = 'Rubble')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select inSubQuery`(testDB: TestDB) {
        withPersonsAndAddress(testDB) { persons, _ ->
            val query = persons
                .selectAll()
                .where {
                    persons.id inSubQuery persons.select(persons.id).where { persons.lastName eq "Rubble" }
                }

            val rows = query.toList()
            rows shouldHaveSize 3

            // Query를 Entity로 만들기
            val entities = Person.wrapRows(query).toList()
            entities.forEach { person ->
                log.debug { "person: $person" }
            }
            entities.all { it.lastName == "Rubble" }.shouldBeTrue()

            val expectedIds = when (currentDialect) {
                is MariaDBDialect -> listOf(5L, 6L, 8L)
                else -> listOf(4L, 5L, 6L)
            }
            entities.map { it.id.value } shouldBeEqualTo expectedIds
        }
    }

    /**
     * ```sql
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  WHERE persons.id NOT IN (SELECT DISTINCT ON (persons.id) persons.id
     *                             FROM persons
     *                            WHERE persons.last_name = 'Rubble')
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `select notInSubQuery`(testDB: TestDB) {
        Assumptions.assumeTrue { testDB !in TestDB.ALL_MYSQL_MARIADB }

        withPersonsAndAddress(testDB) { persons, _ ->
            val query = persons.selectAll()
                .where {
                    persons.id notInSubQuery
                            persons.select(persons.id)
                                .withDistinctOn(persons.id)
                                .where { persons.lastName eq "Rubble" }
                }

            val entities = Person.wrapRows(query).toList()
            entities.forEach { person ->
                log.debug { "person: $person" }
            }
            entities shouldHaveSize 3
            entities.map { it.id.value } shouldBeEqualTo listOf(1L, 2L, 3L)
        }
    }

    //
    // NOTE: lessSubQuery, lessEqSubQuery, greaterSubQuery, greaterEqSubQuery 은 아직 지원하지 않는다
    //


    /**
     * Update Set in H2 (다른 DB에서는 지원하지 않는다)
     * ```sql
     * UPDATE PERSONS
     *    SET ADDRESS_ID=(SELECT (MAX(PERSONS.ADDRESS_ID) - 1) FROM PERSONS)
     *  WHERE PERSONS.ID = 3
     * ```
     */
    @Test
    fun `update set to subquery in H2`() {
        // implement a +/- operator using CustomOperator
        // NOTE: EntityID<ID> 는 CustomOperator를 사용할 수 없다
        withPersonsAndAddress(TestDB.H2) { persons, _ ->
            val affectedRows = persons.update({ persons.id eq 3L }) {
                it[persons.addressId] = persons.select(persons.addressId.max() - 1L)
            }
            affectedRows shouldBeEqualTo 1

            val person = Person.findById(3L)!!
            person.address.id.value shouldBeEqualTo 1L      // 2 - 1
        }
    }

    /**
     * MySQL 에서
     * ```sql
     * -- MySQL V8:
     * SELECT (persons.address_id - 1) addressId
     *   FROM persons
     *  GROUP BY persons.address_id
     *  ORDER BY persons.address_id DESC;
     *
     * UPDATE persons
     *    SET address_id=1
     *  WHERE persons.id = 5;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `update set to subquery in MYSQL`(testDB: TestDB) {
        Assumptions.assumeTrue(testDB in TestDB.ALL_MYSQL_LIKE)

        withPersonsAndAddress(testDB) { persons, _ ->
            val calcAddressId = (persons.addressId - 1L).alias("addressId")
            val query = persons.select(calcAddressId)
                .groupBy(persons.addressId)
                .orderBy(persons.addressId, SortOrder.DESC)

            val maxAddressId = query.firstOrNull()?.getOrNull(calcAddressId).asLong()
            log.debug { "maxAddressId=$maxAddressId" }

            val affectedRows = persons.update({ persons.id eq 5L }) {
                it[persons.addressId] = maxAddressId
            }
            affectedRows shouldBeEqualTo 1

            val person = Person.findById(5L)!!
            person.address.id.value shouldBeEqualTo 1L      // 2 - 1
        }
    }

    // implement a +/- operator using CustomOperator
    @JvmName("expressionPlusInt")
    infix operator fun Expression<*>.plus(operand: Int): CustomOperator<Int> =
        CustomOperator("+", IntegerColumnType(), this, intLiteral(operand))

    @JvmName("expressionMinusInt")
    infix operator fun Expression<*>.minus(operand: Int): CustomOperator<Int> =
        CustomOperator("-", IntegerColumnType(), this, intLiteral(operand))

    @JvmName("expressionPlusLong")
    infix operator fun Expression<*>.plus(operand: Long): CustomOperator<Long> =
        CustomOperator("+", LongColumnType(), this, longLiteral(operand))

    @JvmName("expressionMinusLong")
    infix operator fun Expression<*>.minus(operand: Long): CustomOperator<Long> =
        CustomOperator("-", LongColumnType(), this, longLiteral(operand))

}
