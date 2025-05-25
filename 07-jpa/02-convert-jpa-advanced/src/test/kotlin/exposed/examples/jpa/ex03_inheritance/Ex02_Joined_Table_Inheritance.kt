package exposed.examples.jpa.ex03_inheritance

import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ReferenceOption.CASCADE
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * JPA의 Joined Inheritance 는 Exposed 에서는 Table 간의 Join 으로 구현할 수 있다.
 *
 * 단 CRUD 를 위해서는 따로 Repository 를 만들어야 한다.
 */
class Ex02_Joined_Table_Inheritance: AbstractExposedTest() {

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
    object PersonTable: IntIdTable("joined_person") {
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
     *      id INT PRIMARY KEY,
     *      emp_no VARCHAR(128) NOT NULL,
     *      emp_title VARCHAR(128) NOT NULL,
     *      manager_id INT NULL
     * );
     *
     * ALTER TABLE joined_employee
     *  ADD CONSTRAINT joined_employee_emp_no_manager_id_unique UNIQUE (emp_no, manager_id);
     *
     * ALTER TABLE joined_employee
     *   ADD CONSTRAINT fk_joined_employee_id__id FOREIGN KEY (id)
     *       REFERENCES joined_person(id) ON DELETE CASCADE ON UPDATE CASCADE;
     *
     * ALTER TABLE joined_employee
     *   ADD CONSTRAINT fk_joined_employee_manager_id__id FOREIGN KEY (manager_id)
     *      REFERENCES joined_employee(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
     * ```
     */
    object EmployeeTable: IdTable<Int>("joined_employee") {
        override val id = reference("id", PersonTable, onDelete = CASCADE, onUpdate = CASCADE)

        val empNo = varchar("emp_no", 128)
        val empTitle = varchar("emp_title", 128)
        val managerId = optReference("manager_id", EmployeeTable.id)

        override val primaryKey = PrimaryKey(id)

        init {
            uniqueIndex(empNo, managerId)
        }
    }

    /**
     * Joined Customer Table
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS joined_customer (
     *      id INT PRIMARY KEY,
     *      mobile VARCHAR(16) NOT NULL,
     *      customer_grade VARCHAR(32) NOT NULL,
     *      contact_employee_id INT NULL
     * );
     *
     * CREATE INDEX idx_customer_mobile ON joined_customer (mobile, customer_grade);
     * ALTER TABLE joined_customer
     *   ADD CONSTRAINT fk_joined_customer_id__id FOREIGN KEY (id)
     *      REFERENCES joined_person(id) ON DELETE CASCADE ON UPDATE CASCADE;
     * ALTER TABLE joined_customer
     *   ADD CONSTRAINT fk_joined_customer_contact_employee_id__id FOREIGN KEY (contact_employee_id)
     *      REFERENCES joined_employee(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
     * ```
     */
    object CustomerTable: IdTable<Int>("joined_customer") {
        override val id: Column<EntityID<Int>> = reference("id", PersonTable, onDelete = CASCADE, onUpdate = CASCADE)

        val mobile = varchar("mobile", 16)
        val grade = varchar("customer_grade", 32)
        val contactEmployeeId = optReference("contact_employee_id", EmployeeTable)

        override val primaryKey = PrimaryKey(id)

        init {
            index("idx_customer_mobile", false, mobile, grade)
        }
    }

    open class Person(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Person>(PersonTable)

        open var name by PersonTable.name
        open var ssn by PersonTable.ssn

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("ssn", ssn)
            .toString()
    }

    class Employee(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Employee>(EmployeeTable) {
            fun create(person: Person, init: Employee.() -> Unit): Employee {
                return Employee.new(person.id.value, init)
            }

            fun create(name: String, ssn: String, init: Employee.() -> Unit): Employee {
                val person = Person.new {
                    this.name = name
                    this.ssn = ssn
                }
                return Employee.new(person.id.value, init)
            }
        }

        // Exposed 에서는 이렇게 one-to-one 참조로 표현해야 합니다.
        val person: Person by Person referencedOn EmployeeTable.id

        // JPA 와 유사한 인터페이스를 유지하기 위해 name, ssn 필드를 재정의합니다.
        var name
            get() = person.name
            set(value) {
                person.name = value
            }
        var ssn
            get() = person.ssn
            set(value) {
                person.ssn = value
            }

        var empNo: String by EmployeeTable.empNo
        var empTitle: String by EmployeeTable.empTitle
        var manager: Employee? by Employee optionalReferencedOn EmployeeTable.managerId
        val members: SizedIterable<Employee> by Employee optionalReferrersOn EmployeeTable.managerId

        // one-to-one 관계에서 child 를 삭제한다고, owner 가 삭제되지는 않는다 -> delete 함수를 재정의해야 한다
        override fun delete() {
            super.delete()
            person.delete()
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("ssn", ssn)
            .add("empNo", empNo)
            .add("empTitle", empTitle)
            .toString()
    }

    class Customer(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Customer>(CustomerTable) {
            fun create(person: Person, init: Customer.() -> Unit): Customer {
                return Customer.new(person.id.value, init)
            }

            fun create(name: String, ssn: String, init: Customer.() -> Unit): Customer {
                val person = Person.new {
                    this.name = name
                    this.ssn = ssn
                }
                return Customer.new(person.id.value, init)
            }
        }

        // Exposed 에서는 이렇게 one-to-one 참조로 표현해야 합니다.
        val person: Person by Person referencedOn CustomerTable.id

        // JPA 와 유사한 인터페이스를 유지하기 위해 name, ssn 필드를 재정의합니다.
        var name
            get() = person.name
            set(value) {
                person.name = value
            }
        var ssn
            get() = person.ssn
            set(value) {
                person.ssn = value
            }

        var mobile: String by CustomerTable.mobile
        var grade: String by CustomerTable.grade

        var contactEmployee: Employee? by Employee optionalReferencedOn CustomerTable.contactEmployeeId

        // one-to-one 관계에서 child 를 삭제한다고, owner 가 삭제되지는 않는다 -> delete 함수를 재정의해야 한다
        override fun delete() {
            super.delete()
            person.delete()
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("ssn", ssn)
            .add("mobile", mobile)
            .add("grade", grade)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `DAO with joined table strategy`(testDB: TestDB) {
        withTables(testDB, PersonTable, EmployeeTable, CustomerTable) {
            val emp1 = Employee.create("Debop", "111111-111111") {
                empNo = "EMP-001"
                empTitle = "Manager"
            }
            val emp2 = Employee.create("Kally", "222222-222222") {
                empNo = "EMP-002"
                empTitle = "Staff"
                manager = emp1
            }

            val customer = Customer.create("Black", "333333-333333") {
                mobile = "010-5555-5555"
                grade = "VIP"
                contactEmployee = emp2
            }

            entityCache.clear()

            /**
             * Eager loading with `with()`
             *
             * ```sql
             * -- Postgres
             * SELECT joined_employee.id, joined_employee.emp_no, joined_employee.emp_title, joined_employee.manager_id FROM joined_employee
             * SELECT joined_person.id, joined_person.person_name, joined_person.ssn FROM joined_person WHERE joined_person.id IN (1, 2)
             * ```
             */
            val emps = Employee.all().with(Employee::person).toList()
            emps.forEach {
                log.debug { it }
            }

            /**
             * Lazy loading with `load()`
             *
             * ```sql
             * -- Postgres
             * SELECT joined_customer.id, joined_customer.mobile, joined_customer.customer_grade, joined_customer.contact_employee_id FROM joined_customer WHERE joined_customer.id = 3
             * SELECT joined_employee.id, joined_employee.emp_no, joined_employee.emp_title, joined_employee.manager_id FROM joined_employee WHERE joined_employee.id = 2
             * ```
             */
            val loadedCustomer = Customer.findById(customer.id)!!.load(Customer::person, Customer::contactEmployee)
            loadedCustomer.ssn shouldBeEqualTo customer.ssn
            loadedCustomer.contactEmployee shouldBeEqualTo emp2

            // 엔티티 삭제 (one-to-one 관계에서 child 를 삭제한다고, owner 가 삭제되지는 않는다 -> delete 함수를 재정의해야 한다)
            loadedCustomer.delete()
            emp2.delete()
            emp1.delete()
        }
    }

}
