package exposed.examples.jpa.ex02_entities

import exposed.examples.jpa.ex02_entities.PersonSchema.Address
import exposed.examples.jpa.ex02_entities.PersonSchema.Person
import exposed.examples.jpa.ex02_entities.PersonSchema.PersonRecord
import exposed.examples.jpa.ex02_entities.PersonSchema.withPersonAndAddress
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.plus
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class Ex02_Person: JdbcExposedTestBase() {

    companion object: KLogging()

    /**
     * ```sql
     * -- Postgres:
     * -- DSL
     * SELECT COUNT(*) FROM persons WHERE persons.id < 3;
     * SELECT COUNT(persons.id) FROM persons WHERE persons.id < 3;
     * -- DAO
     * SELECT COUNT(*) FROM persons WHERE persons.id < 3;
     * SELECT COUNT(persons.id) FROM persons WHERE persons.id < 3;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count with where clause`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            // DSL 사용한 방법
            persons.selectAll()
                .where { persons.id less 3L }
                .count() shouldBeEqualTo 2L

            persons.select(persons.id.count())
                .where { persons.id less 3L }
                .single()[persons.id.count()] shouldBeEqualTo 2L

            // DAO 사용한 방법
            Person.find { persons.id less 3L }.count() shouldBeEqualTo 2L
            Person.count(persons.id less 3L) shouldBeEqualTo 2L
        }
    }

    /**
     * ```sql
     * -- Postgres:
     * SELECT COUNT(*) FROM persons;
     *
     * SELECT COUNT(*) FROM persons;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count all records`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->

            // SQL DSL 이용
            persons.selectAll().count() shouldBeEqualTo 6L

            // DAO 이용
            Person.all().count() shouldBeEqualTo 6L
        }
    }

    /**
     * ```sql
     * SELECT COUNT(persons.last_name)
     *   FROM persons
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count lastName`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            persons
                .select(persons.lastName.count())
                .single()[persons.lastName.count()] shouldBeEqualTo 6L
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT COUNT(DISTINCT persons.last_name)
     *   FROM persons
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `count distinct lastName`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->

            // COUNT(DISTINCT persons.last_name)
            val counter = persons.lastName.countDistinct()

            persons
                .select(counter)
                .single()[counter] shouldBeEqualTo 2L

        }
    }

    private fun JdbcTransaction.insertPerson(): Long {
        return PersonSchema.PersonTable.insertAndGetId {
            it[firstName] = faker.name().firstName()
            it[lastName] = faker.name().lastName()
            it[birthDate] = java.time.LocalDate.now()
            it[addressId] = 1L
        }.value
    }

    /**
     * ```sql
     * -- Postgres
     * DELETE FROM persons WHERE persons.id = 7
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete by id`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val pId: Long = insertPerson()

            // DELETE FROM persons WHERE persons.id = 7
            // 삭제된 행의 갯수 반환
            persons.deleteWhere { persons.id eq pId } shouldBeEqualTo 1
        }
    }

    /**
     * ```sql
     * -- Postgre
     * DELETE
     *   FROM persons
     *  WHERE (persons.id >= 7)
     *    AND (persons.occupation IS NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete by where and`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val id1 = insertPerson()
            val id2 = insertPerson()
            val id3 = insertPerson()

            entityCache.clear()

            // DELETE FROM PERSONS WHERE (PERSONS.ID > $id1) AND (PERSONS.OCCUPATION IS NULL)
            persons
                .deleteWhere {
                    persons.id greaterEq id1 and persons.occupation.isNull()
                } shouldBeEqualTo 3
        }
    }

    /**
     * ```sql
     * DELETE
     *   FROM persons
     *  WHERE (persons.id >= 7) OR (persons.occupation IS NOT NULL)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete by where or`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val id1 = insertPerson()
            val id2 = insertPerson()
            val id3 = insertPerson()

            entityCache.clear()

            persons
                .deleteWhere {
                    persons.id greaterEq id1 or persons.occupation.isNotNull()
                } shouldBeEqualTo 7
        }
    }

    /**
     * ```sql
     * DELETE
     *   FROM persons
     *  WHERE ((persons.id >= 7) OR (persons.occupation IS NOT NULL))
     *    AND (persons.employeed = TRUE)
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete by where or and`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val id1 = insertPerson()
            val id2 = insertPerson()
            val id3 = insertPerson()

            // DELETE FROM PERSONS WHERE (PERSONS.ID > $id1) OR (PERSONS.OCCUPATION IS NOT NULL) AND (PERSONS.ID < $id3)
            persons
                .deleteWhere {
                    (persons.id greaterEq id1 or persons.occupation.isNotNull()) and (persons.employeed eq true)
                } shouldBeEqualTo 5
        }
    }

    /**
     * ```sql
     * DELETE
     *   FROM persons
     *  WHERE (persons.id >= 7)
     *     OR ((persons.occupation IS NOT NULL) AND (persons.employeed = TRUE))
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete by where and or`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val id1 = insertPerson()
            val id2 = insertPerson()
            val id3 = insertPerson()

            persons
                .deleteWhere {
                    (persons.id greaterEq id1) or ((persons.occupation.isNotNull()) and (persons.employeed eq true))
                } shouldBeEqualTo 5
        }
    }

    /**
     * PostgreSQL doesn't support LIMIT in DELETE clause
     *
     * ```sql
     * -- MySQL V8
     * DELETE FROM persons WHERE persons.id < 7 LIMIT 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `delete by where with limit`(testDB: TestDB) {
        // PostgreSQL doesn't support LIMIT in DELETE clause
        Assumptions.assumeTrue { testDB !in TestDB.ALL_POSTGRES_LIKE }

        withPersonAndAddress(testDB) { persons, _ ->
            val id1 = insertPerson()
            val id2 = insertPerson()
            val id3 = insertPerson()

            // DELETE FROM PERSONS WHERE PERSONS.ID < $id1 LIMIT 1
            persons
                .deleteWhere(limit = 1)
                {
                    persons.id less id1
                } shouldBeEqualTo 1
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
     *  WHERE persons.id = 100
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert entity`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val person = Person.new(100) {
                firstName = "John"
                lastName = "Doe"
                birthDate = java.time.LocalDate.now()
                employeed = true
                occupation = "Software Engineer"
                address = Address[1L]
            }

            entityCache.clear()

            val saved = Person.findById(person.id)!!
            saved shouldBeEqualTo person
        }
    }

    /**
     * ```sql
     * INSERT INTO persons (first_name, last_name, birth_date, employeed, occupation, address_id)
     * VALUES ('John', 'Doe', '2025-02-06', TRUE, 'Software Engineer', 1);
     *
     * SELECT persons.id,
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  WHERE persons.id = 7;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert record`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            val personId = persons.insertAndGetId {
                it[firstName] = "John"
                it[lastName] = "Doe"
                it[birthDate] = java.time.LocalDate.now()
                it[employeed] = true
                it[occupation] = "Software Engineer"
                it[addressId] = 1L
            }

            val saved = persons.selectAll().where { persons.id eq personId }.single()
            saved[persons.id] shouldBeEqualTo personId
        }
    }

    /**
     * ```sql
     * INSERT INTO persons (first_name, last_name, birth_date, employeed, occupation, address_id)
     * VALUES ('Joe', 'Jones', '2025-02-06', TRUE, 'Developer', 1);
     *
     * INSERT INTO persons (first_name, last_name, birth_date, employeed, occupation, address_id)
     * VALUES ('Sarah', 'Smith', '2025-02-06', TRUE, 'Architect', 2);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `batchInsert 01`(testDB: TestDB) {

        withPersonAndAddress(testDB) { persons, _ ->
            val record1 = PersonRecord(null, "Joe", "Jones", LocalDate.now(), true, "Developer", 1L)
            val record2 = PersonRecord(null, "Sarah", "Smith", LocalDate.now(), true, "Architect", 2L)

            val rows = persons.batchInsert(listOf(record1, record2)) { record ->
                this[persons.firstName] = record.firstName!!
                this[persons.lastName] = record.lastName!!
                this[persons.birthDate] = record.birthDate!!
                this[persons.employeed] = record.employeed!!
                this[persons.occupation] = record.occupation
                this[persons.addressId] = record.address!!
            }

            rows shouldHaveSize 2
            rows.all { it[persons.id].value > 0 }.shouldBeTrue()
        }
    }

    /**
     * ```sql
     * -- PostgreSQL
     * INSERT INTO persons (first_name, last_name, birth_date, employeed, occupation, address_id)
     * SELECT persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  WHERE TRUE
     *  ORDER BY persons.id ASC
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert select example 01`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            persons.selectAll().count() shouldBeEqualTo 6L

            val inserted = persons
                .insert(
                    persons
                        .select(
                            persons.firstName,
                            persons.lastName,
                            persons.birthDate,
                            persons.employeed,
                            persons.occupation,
                            persons.addressId
                        )
                        .where {
                            val occupation: String? = null
                            occupation?.let { persons.occupation.like("%$occupation%") } ?: Op.TRUE
                        }
                        .orderBy(persons.id)
                )

            inserted shouldBeEqualTo 6
            persons.selectAll().count() shouldBeEqualTo 12L
        }
    }

    /**
     * ```sql
     * INSERT INTO persons (id, first_name, last_name, birth_date, employeed, occupation, address_id)
     * SELECT (persons.id + 100),
     *        persons.first_name,
     *        persons.last_name,
     *        persons.birth_date,
     *        persons.employeed,
     *        persons.occupation,
     *        persons.address_id
     *   FROM persons
     *  ORDER BY persons.id ASC;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `insert select example 02`(testDB: TestDB) {
        withPersonAndAddress(testDB) { persons, _ ->
            persons.selectAll().count() shouldBeEqualTo 6L

            // PersonTaleDML 은 PersonTable과 물리적으로 같은 테이블을 사용하지만,
            // id에 AutoIncrement 를 지정하지 않아, 이렇게 id를 직접 지정할 수 있습니다. (INSERT INTO SELECT 구문 사용 시 필요)
            val personsDml = PersonSchema.PersonTableDML

            val inserted = personsDml.insert(
                personsDml
                    .select(
                        personsDml.id + 100L,
                        personsDml.firstName,
                        personsDml.lastName,
                        personsDml.birthDate,
                        personsDml.employeed,
                        personsDml.occupation,
                        personsDml.addressId
                    )
                    .orderBy(personsDml.id)
            )

            inserted shouldBeEqualTo 6
            persons.selectAll().count() shouldBeEqualTo 12L
        }
    }

}
