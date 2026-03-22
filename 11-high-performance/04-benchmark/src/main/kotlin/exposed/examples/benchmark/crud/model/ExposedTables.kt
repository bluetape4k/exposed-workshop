package exposed.examples.benchmark.crud.model

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp

// ── DSL Tables ─────────────────────────────────────────

/**
 * Single Entity: Person (10 컬럼)
 */
object PersonTable: LongIdTable("persons") {
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).uniqueIndex("idx_persons_email")
    val phone = varchar("phone", 30).nullable()
    val age = integer("age")
    val address = varchar("address", 500).nullable()
    val zipcode = varchar("zipcode", 20).nullable()
    val bio = text("bio").nullable()
    val picture = blob("picture").nullable()
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    init {
        index("idx_persons_name", false, firstName, lastName)
        index("idx_persons_zipcode", false, zipcode)
    }
}

/**
 * One-to-Many: Department (8 컬럼)
 */
object DepartmentTable: LongIdTable("departments") {
    val name = varchar("name", 200).uniqueIndex("idx_dept_name")
    val code = varchar("code", 20).uniqueIndex("idx_dept_code")
    val description = text("description").nullable()
    val budget = decimal("budget", 15, 2)
    val headCount = integer("head_count").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()
}

/**
 * One-to-Many: Employee (12 컬럼)
 */
object EmployeeTable: LongIdTable("employees") {
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).uniqueIndex("idx_emp_email")
    val phone = varchar("phone", 30).nullable()
    val position = varchar("position", 100)
    val salary = decimal("salary", 12, 2)
    val hireDate = timestamp("hire_date")
    val isActive = bool("is_active").default(true)
    val bio = text("bio").nullable()
    val picture = blob("picture").nullable()
    val departmentId = reference("department_id", DepartmentTable, onDelete = ReferenceOption.CASCADE)
        .index("idx_emp_dept_id")
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)
    val updatedAt = timestamp("updated_at").nullable()

    init {
        index("idx_emp_name", false, firstName, lastName)
        index("idx_emp_position", false, position)
        index("idx_emp_salary", false, salary)
    }
}

// ── DAO Entities ───────────────────────────────────────

class PersonEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<PersonEntity>(PersonTable)

    var firstName by PersonTable.firstName
    var lastName by PersonTable.lastName
    var email by PersonTable.email
    var phone by PersonTable.phone
    var age by PersonTable.age
    var address by PersonTable.address
    var zipcode by PersonTable.zipcode
    var bio by PersonTable.bio
    var picture by PersonTable.picture
    var createdAt by PersonTable.createdAt
    var updatedAt by PersonTable.updatedAt
}

class DepartmentEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<DepartmentEntity>(DepartmentTable)

    var name by DepartmentTable.name
    var code by DepartmentTable.code
    var description by DepartmentTable.description
    var budget by DepartmentTable.budget
    var headCount by DepartmentTable.headCount
    var isActive by DepartmentTable.isActive
    var createdAt by DepartmentTable.createdAt
    var updatedAt by DepartmentTable.updatedAt

    val employees by EmployeeEntity referrersOn EmployeeTable.departmentId
}

class EmployeeEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<EmployeeEntity>(EmployeeTable)

    var firstName by EmployeeTable.firstName
    var lastName by EmployeeTable.lastName
    var email by EmployeeTable.email
    var phone by EmployeeTable.phone
    var position by EmployeeTable.position
    var salary by EmployeeTable.salary
    var hireDate by EmployeeTable.hireDate
    var isActive by EmployeeTable.isActive
    var bio by EmployeeTable.bio
    var picture by EmployeeTable.picture
    var department by DepartmentEntity referencedOn EmployeeTable.departmentId
    var createdAt by EmployeeTable.createdAt
    var updatedAt by EmployeeTable.updatedAt
}
