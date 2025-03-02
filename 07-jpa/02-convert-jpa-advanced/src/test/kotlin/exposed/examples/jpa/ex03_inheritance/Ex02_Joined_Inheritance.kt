package exposed.examples.jpa.ex03_inheritance

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable

/**
 * JPA의 Joined Inheritance 는 Exposed 에서는 Table 간의 Join 으로 구현할 수 있다.
 *
 * 단 CRUD 를 위해서는 따로 Repository 를 만들어야 한다.
 */
class Ex02_Joined_Inheritance: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * Joined Person Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS joined_person (
     *      id SERIAL PRIMARY KEY,
     *      person_name VARCHAR(128) NOT NULL,
     *      ssn VARCHAR(128) NOT NULL
     * );
     *
     * ALTER TABLE joined_person
     *   ADD CONSTRAINT joined_person_person_name_ssn_unique UNIQUE (person_name, ssn);
     * ```
     */
    object JoinedPersonTable: IntIdTable("joined_person") {
        val name = varchar("person_name", 128)
        val ssn = varchar("ssn", 128)

        init {
            uniqueIndex(name, ssn)
        }
    }

    /**
     * Joined Employee Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS joined_employee (
     *      id INT NOT NULL,
     *      emp_no VARCHAR(128) NOT NULL,
     *      manager_id INT NULL,
     *
     *      CONSTRAINT fk_joined_employee_id__id FOREIGN KEY (id)
     *      REFERENCES joined_person(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     * ```
     */
    object JoinedEmployeeTable: IdTable<Int>("joined_employee") {
        override val id: Column<EntityID<Int>> =
            reference("id", JoinedPersonTable.id, onDelete = CASCADE, onUpdate = CASCADE)

        val empNo = varchar("emp_no", 128)
        val managerId = integer("manager_id").nullable()
    }

    /**
     * Joined Customer Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS joined_customer (
     *      id INT NOT NULL,
     *      mobile VARCHAR(16) NOT NULL,
     *      contact_employee_id INT NULL,
     *
     *      CONSTRAINT fk_joined_customer_id__id FOREIGN KEY (id)
     *      REFERENCES joined_person(id) ON DELETE CASCADE ON UPDATE CASCADE
     * );
     * ```
     */
    object JoinedCustomerTable: IdTable<Int>("joined_customer") {
        override val id: Column<EntityID<Int>> =
            reference("id", JoinedPersonTable.id, onDelete = CASCADE, onUpdate = CASCADE)

        val mobile = varchar("mobile", 16)
        val contactEmployeeId = integer("contact_employee_id").nullable()
    }

    abstract class AbstractJoinedPerson: Serializable {
        abstract val id: Int
        abstract val name: String
        abstract val ssn: String
    }

    data class JoinedEmployee(
        override val id: Int,
        override val name: String,
        override val ssn: String,
        val empNo: String,
        val managerId: Int? = null,
    ): AbstractJoinedPerson()

    data class JoinedCustomer(
        override val id: Int,
        override val name: String,
        override val ssn: String,
        val mobile: String,
        val contactEmployeeId: Int? = null,
    ): AbstractJoinedPerson()

    fun ResultRow.toJoinedEmployee(): JoinedEmployee = JoinedEmployee(
        id = this[JoinedPersonTable.id].value,
        name = this[JoinedPersonTable.name],
        ssn = this[JoinedPersonTable.ssn],
        empNo = this[JoinedEmployeeTable.empNo],
        managerId = this[JoinedEmployeeTable.managerId]
    )

    fun ResultRow.toJoinedCustomer(): JoinedCustomer = JoinedCustomer(
        id = this[JoinedPersonTable.id].value,
        name = this[JoinedPersonTable.name],
        ssn = this[JoinedPersonTable.ssn],
        mobile = this[JoinedCustomerTable.mobile],
        contactEmployeeId = this[JoinedCustomerTable.contactEmployeeId]
    )


    val joinedEmployees = JoinedPersonTable.innerJoin(JoinedEmployeeTable)
    val joinedCustomers = JoinedPersonTable.innerJoin(JoinedCustomerTable)


    @Suppress("UnusedReceiverParameter")
    fun Transaction.newEmployee(name: String, ssn: String, empNo: String): JoinedEmployee {

        /**
         * ```sql
         * INSERT INTO joined_person (person_name, ssn) VALUES ('Debop', '111111-111111');
         * ```
         */
        val personId1 = JoinedPersonTable.insertAndGetId {
            it[JoinedPersonTable.name] = name
            it[JoinedPersonTable.ssn] = ssn
        }
        /**
         * ```sql
         * INSERT INTO joined_employee (id, emp_no) VALUES (1, 'EMP-001')
         * ```
         */
        JoinedEmployeeTable.insert {
            it[id] = personId1
            it[JoinedEmployeeTable.empNo] = empNo
        }

        flushCache()

        /**
         * ```sql
         * -- Postgres
         * SELECT joined_person.id,
         *        joined_person.person_name,
         *        joined_person.ssn,
         *        joined_employee.id,
         *        joined_employee.emp_no,
         *        joined_employee.manager_id
         *   FROM joined_person
         *      INNER JOIN joined_employee ON joined_person.id = joined_employee.id
         *  WHERE joined_person.id = 1
         * ```
         */
        return joinedEmployees
            .selectAll()
            .where { JoinedPersonTable.id eq personId1 }
            .map { it.toJoinedEmployee() }
            .single()
    }

    @Suppress("UnusedReceiverParameter")
    fun Transaction.newCustomer(name: String, ssn: String, mobile: String, contactEmployeeId: Int?): JoinedCustomer {
        /**
         * ```sql
         * INSERT INTO joined_person (person_name, ssn)
         * VALUES ('Black', '333333-333333')
         * ```
         */
        val personId1 = JoinedPersonTable.insertAndGetId {
            it[JoinedPersonTable.name] = name
            it[JoinedPersonTable.ssn] = ssn
        }
        /**
         * ```sql
         * INSERT INTO joined_customer (id, mobile, contact_employee_id)
         * VALUES (3, '010-5555-5555', 2)
         * ```
         */
        JoinedCustomerTable.insert {
            it[id] = personId1
            it[JoinedCustomerTable.mobile] = mobile
            it[JoinedCustomerTable.contactEmployeeId] = contactEmployeeId
        }

        /**
         * ```sql
         * SELECT joined_person.id,
         *        joined_person.person_name,
         *        joined_person.ssn,
         *        joined_customer.id,
         *        joined_customer.mobile,
         *        joined_customer.contact_employee_id
         *   FROM joined_person
         *      INNER JOIN joined_customer ON joined_person.id = joined_customer.id
         *  WHERE joined_person.id = 3
         * ```
         */
        return joinedCustomers.selectAll()
            .where { JoinedPersonTable.id eq personId1 }
            .map { it.toJoinedCustomer() }
            .single()
    }


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `inheritance with joined table`(testDB: TestDB) {
        withTables(testDB, JoinedPersonTable, JoinedEmployeeTable, JoinedCustomerTable) {
            val emp1 = newEmployee("Debop", "111111-111111", "EMP-001")
            log.debug { "emp1=$emp1" }

            val emp2 = newEmployee("Kally", "222222-222222", "EMP-002")
            log.debug { "emp2=$emp2" }

            val customer = newCustomer("Black", "333333-333333", "010-5555-5555", emp2.id)
            log.debug { "customer=$customer" }

            // TODO: 삭제 등 추가적인 테스트 코드를 작성하세요.
        }
    }

}
