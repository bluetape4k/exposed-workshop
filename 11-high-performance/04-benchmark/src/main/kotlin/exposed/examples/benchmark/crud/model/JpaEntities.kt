package exposed.examples.benchmark.crud.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

/**
 * Single Entity: Person (10 컬럼)
 */
@Entity
@Table(
    name = "persons",
    indexes = [
        Index(name = "idx_persons_name", columnList = "first_name, last_name"),
        Index(name = "idx_persons_email", columnList = "email", unique = true),
        Index(name = "idx_persons_zipcode", columnList = "zipcode"),
    ]
)
class PersonJpa(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String = "",

    @Column(nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(length = 30)
    var phone: String? = null,

    @Column(nullable = false)
    var age: Int = 0,

    @Column(length = 500)
    var address: String? = null,

    @Column(length = 20)
    var zipcode: String? = null,

    @Lob
    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Lob
    var picture: ByteArray? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
)

/**
 * One-to-Many: Department (8 컬럼)
 */
@Entity
@Table(
    name = "departments",
    indexes = [
        Index(name = "idx_dept_name", columnList = "name", unique = true),
        Index(name = "idx_dept_code", columnList = "code", unique = true),
    ]
)
class DepartmentJpa(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(nullable = false, unique = true, length = 200)
    var name: String = "",

    @Column(nullable = false, unique = true, length = 20)
    var code: String = "",

    @Lob
    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, precision = 15, scale = 2)
    var budget: BigDecimal = BigDecimal.ZERO,

    @Column(name = "head_count", nullable = false)
    var headCount: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    @OneToMany(mappedBy = "department", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var employees: MutableList<EmployeeJpa> = mutableListOf(),
)

/**
 * One-to-Many: Employee (12 컬럼)
 */
@Entity
@Table(
    name = "employees",
    indexes = [
        Index(name = "idx_emp_email", columnList = "email", unique = true),
        Index(name = "idx_emp_name", columnList = "first_name, last_name"),
        Index(name = "idx_emp_dept_id", columnList = "department_id"),
        Index(name = "idx_emp_position", columnList = "position"),
        Index(name = "idx_emp_salary", columnList = "salary"),
    ]
)
class EmployeeJpa(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String = "",

    @Column(nullable = false, unique = true, length = 255)
    var email: String = "",

    @Column(length = 30)
    var phone: String? = null,

    @Column(nullable = false, length = 100)
    var position: String = "",

    @Column(nullable = false, precision = 12, scale = 2)
    var salary: BigDecimal = BigDecimal.ZERO,

    @Column(name = "hire_date", nullable = false)
    var hireDate: Instant = Instant.now(),

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Lob
    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Lob
    var picture: ByteArray? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    var department: DepartmentJpa? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,
)
