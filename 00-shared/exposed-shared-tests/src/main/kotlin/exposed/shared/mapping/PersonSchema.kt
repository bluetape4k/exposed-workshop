package exposed.shared.mapping

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.entityToStringBuilder
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.time.LocalDate

/**
 * 사람(Person)과 주소(Address) 엔티티와 테이블을 포함하는 스키마 정의 객체.
 *
 * DAO 패턴과 DSL 패턴 모두를 지원하며, 사람과 주소 간의 다대일(Many-to-One) 관계를 나타냅니다.
 */
object PersonSchema {

    val allPersonTables = arrayOf(AddressTable, PersonTable)

    /**
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS addresses (
     *      id BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      street VARCHAR(255) NOT NULL,
     *      city VARCHAR(255) NOT NULL,
     *      `state` VARCHAR(32) NOT NULL,
     *      zip VARCHAR(10) NULL
     * );
     *
     * CREATE INDEX idx_address_city_zip ON addresses (city, zip, id)
     *
     * ```
     */
    object AddressTable: LongIdTable("addresses") {
        val street = varchar("street", 255)
        val city = varchar("city", 255)
        val state = varchar("state", 32)
        val zip = varchar("zip", 10).nullable()

        init {
            index("idx_address_city_zip", false, city, zip, id)
        }
    }

    /**
     * ```sql
     * -- MySQL V8
     * CREATE TABLE IF NOT EXISTS persons (
     *      id BIGINT AUTO_INCREMENT PRIMARY KEY,
     *      first_name VARCHAR(50) NOT NULL,
     *      last_name VARCHAR(50) NOT NULL,
     *      birth_date DATE NOT NULL,
     *      employed BOOLEAN DEFAULT TRUE NOT NULL,
     *      occupation VARCHAR(255) NULL,
     *      address_id BIGINT NOT NULL,
     *
     *      CONSTRAINT fk_persons_address_id__id FOREIGN KEY (address_id)
     *      REFERENCES addresses(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE persons ADD CONSTRAINT idx_person_addr UNIQUE (id, address_id)
     * ```
     */
    object PersonTable: LongIdTable("persons") {
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val birthDate = date("birth_date")
        val employed = bool("employed").default(true)
        val occupation = varchar("occupation", 255).nullable()
        val addressId = reference("address_id", AddressTable)  // many to one

        init {
            uniqueIndex("idx_person_addr", id, addressId)
        }
    }

    /**
     * INSERT SELECT 등 SQL 만을 위해서 사용하기 위한 테이블 정의.
     *
     * `PersonTable`은 `Person` 엔티티를 위한 테이블이다. 하지만 같은 테이블을 바라보고 있다.
     */
    object PersonTableDML: Table("persons") {
        val id = long("id")   // autoIncrement() 를 지정하면 insert select 같은 id 에 값을 지정하는 것이 불가능하다.
        val firstName = varchar("first_name", 50)
        val lastName = varchar("last_name", 50)
        val birthDate = date("birth_date")
        val employed = bool("employed").default(true)
        val occupation = varchar("occupation", 255).nullable()
        val addressId = long("address_id")  // many to one

        override val primaryKey = PrimaryKey(id)
    }

    /** 주소 엔티티. [AddressTable]과 매핑됩니다. */
    class Address(id: EntityID<Long>): LongEntity(id), java.io.Serializable {
        companion object: LongEntityClass<Address>(AddressTable)

        var street by AddressTable.street
        var city by AddressTable.city
        var state by AddressTable.state
        var zip by AddressTable.zip

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("street", street)
            .add("city", city)
            .add("state", state)
            .add("zip", zip)
            .toString()
    }

    /** 사람 엔티티. [PersonTable]과 매핑됩니다. */
    class Person(id: EntityID<Long>): LongEntity(id), java.io.Serializable {
        companion object: LongEntityClass<Person>(PersonTable)

        var firstName by PersonTable.firstName
        var lastName by PersonTable.lastName
        var birthDate by PersonTable.birthDate
        var employed by PersonTable.employed
        var occupation by PersonTable.occupation
        var address by Address referencedOn PersonTable.addressId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = entityToStringBuilder()
            .add("firstName", firstName)
            .add("lastName", lastName)
            .add("birthDate", birthDate)
            .add("employed", employed)
            .add("occupation", occupation)
            .add("address", address)
            .toString()
    }

    /**
     * 사람 조회 결과를 담는 데이터 클래스.
     *
     * @property id 사람 ID
     * @property firstName 이름
     * @property lastName 성
     * @property birthDate 생년월일
     * @property employed 재직 여부
     * @property occupation 직업
     * @property address 주소 ID
     */
    data class PersonRecord(
        val id: Long? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val birthDate: java.time.LocalDate? = null,
        val employed: Boolean? = null,
        val occupation: String? = null,
        val address: Long? = null,
    ): java.io.Serializable

    /**
     * 주소 정보를 포함한 사람 조회 결과를 담는 데이터 클래스.
     *
     * @property id 사람 ID
     * @property firstName 이름
     * @property lastName 성
     * @property birthDate 생년월일
     * @property employed 재직 여부
     * @property occupation 직업
     * @property address 주소 엔티티
     */
    data class PersonWithAddress(
        var id: Long? = null,
        var firstName: String? = null,
        var lastName: String? = null,
        var birthDate: java.time.LocalDate? = null,
        var employed: Boolean? = null,
        var occupation: String? = null,
        var address: Address? = null,
    ): java.io.Serializable

    /**
     * 사람과 주소 테이블을 생성한 후 [block]을 실행합니다.
     *
     * @param testDB 사용할 테스트 데이터베이스
     * @param block 테이블이 준비된 상태에서 실행할 트랜잭션 블록
     */
    fun withPersons(
        testDB: TestDB,
        block: JdbcTransaction.(PersonTable, AddressTable) -> Unit,
    ) {
        withTables(testDB, *allPersonTables) {
            block(PersonTable, AddressTable)
        }
    }

    /**
     * 사람과 주소 테이블을 생성하고 샘플 데이터를 삽입한 후 [statement]를 실행합니다.
     *
     * @param testDB 사용할 테스트 데이터베이스
     * @param statement 테이블과 샘플 데이터가 준비된 상태에서 실행할 트랜잭션 블록
     */
    @Suppress("UnusedReceiverParameter")
    fun AbstractExposedTest.withPersonsAndAddress(
        testDB: TestDB,
        statement: JdbcTransaction.(
            persons: PersonSchema.PersonTable,
            addresses: PersonSchema.AddressTable,
        ) -> Unit,
    ) {
        val persons = PersonSchema.PersonTable
        val addresses = PersonSchema.AddressTable

        withTables(testDB, *PersonSchema.allPersonTables) {

            val addr1 = PersonSchema.Address.new {
                street = "123 Main St"
                city = "Bedrock"
                state = "IN"
                zip = "12345"
            }
            val addr2 = PersonSchema.Address.new {
                street = "456 Elm St"
                city = "Bedrock"
                state = "IN"
                zip = "12345"
            }

            val addr3 = PersonSchema.Address.new {
                street = "165 Kangnam-daero"
                city = "Seoul"
                state = "Seoul"
                zip = "11111"
            }
            flushCache()

            PersonSchema.Person.new {
                firstName = "Fred"
                lastName = "Flintstone"
                birthDate = LocalDate.of(1935, 2, 1)
                employed = true
                occupation = "Brontosaurus Operator"
                address = addr1
            }
            PersonSchema.Person.new {
                firstName = "Wilma"
                lastName = "Flintstone"
                birthDate = LocalDate.of(1940, 2, 1)
                employed = false
                occupation = "Accountant"
                address = addr1
            }
            PersonSchema.Person.new {
                firstName = "Pebbles"
                lastName = "Flintstone"
                birthDate = LocalDate.of(1960, 5, 6)
                employed = false
                address = addr1
            }
            flushCache()
            PersonSchema.Person.new {
                firstName = "Barney"
                lastName = "Rubble"
                birthDate = LocalDate.of(1937, 2, 1)
                employed = true
                occupation = "Brontosaurus Operator"
                address = addr2
            }
            PersonSchema.Person.new {
                firstName = "Betty"
                lastName = "Rubble"
                birthDate = LocalDate.of(1943, 2, 1)
                employed = false
                occupation = "Engineer"
                address = addr2
            }
            PersonSchema.Person.new {
                firstName = "Bamm Bamm"
                lastName = "Rubble"
                birthDate = LocalDate.of(1963, 7, 8)
                employed = false
                address = addr2
            }

            PersonSchema.Person.new {
                firstName = "Sunghyouk"
                lastName = "Bae"
                birthDate = LocalDate.of(1968, 10, 14)
                employed = false
                address = addr3
            }

            PersonSchema.Person.new {
                firstName = "Jehyoung"
                lastName = "Bae"
                birthDate = LocalDate.of(1996, 5, 22)
                employed = false
                address = addr3
            }

            flushCache()

            statement(persons, addresses)
        }
    }
}
