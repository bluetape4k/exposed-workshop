package exposed.examples.benchmark.crud

import com.zaxxer.hikari.HikariDataSource
import exposed.examples.benchmark.crud.model.DepartmentEntity
import io.bluetape4k.logging.KLogging
import exposed.examples.benchmark.crud.model.DepartmentJpa
import exposed.examples.benchmark.crud.model.DepartmentTable
import exposed.examples.benchmark.crud.model.EmployeeJpa
import exposed.examples.benchmark.crud.model.EmployeeTable
import exposed.examples.benchmark.crud.setup.createDataSource
import exposed.examples.benchmark.crud.setup.createEntityManagerFactory
import exposed.examples.benchmark.crud.setup.createExposedTables
import exposed.examples.benchmark.crud.setup.setupExposedDatabase
import jakarta.persistence.EntityManagerFactory
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Warmup
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

private val POSITIONS = arrayOf(
    "Software Engineer", "Senior Engineer", "Staff Engineer",
    "Engineering Manager", "Product Manager", "Designer",
    "Data Scientist", "DevOps Engineer", "QA Engineer", "Tech Lead",
    "VP Engineering", "CTO", "Intern", "Architect", "SRE",
    "Frontend Engineer", "Backend Engineer", "Mobile Engineer",
    "ML Engineer", "Security Engineer"
)

/**
 * One-to-Many (Department → 20 Employees) CRUD 벤치마크.
 *
 * Department: 8컬럼, Employee: 12컬럼 (bio, picture(8KB) 포함)
 * readAll: Exposed DAO eager loading vs JPA JOIN FETCH (동일 엔티티 그래프 로드)
 *
 * Exposed JDBC vs JPA 를 비교합니다.
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class OneToManyCrudBenchmark {

    companion object: KLogging()

    @Param("EXPOSED", "JPA")
    var framework: String = ""

    private lateinit var exposedDs: HikariDataSource
    private lateinit var exposedDb: Database
    private lateinit var jpaDs: HikariDataSource
    private lateinit var emf: EntityManagerFactory

    private val idSeq = AtomicLong(0)
    private val employeesPerDept = 20

    /** 8KB 프로필 사진 바이너리 */
    private val pictureBytes = ByteArray(8192) { (it % 256).toByte() }

    /** 직원 바이오 텍스트 */
    private val bioText = "Experienced professional with extensive background in software development. ".repeat(6)

    /** 부서 설명 텍스트 */
    private val deptDescription =
        "This department is responsible for building and maintaining critical infrastructure. ".repeat(5)

    @Setup(Level.Trial)
    fun setup() {
        when (framework) {
            "EXPOSED" -> {
                exposedDs = createDataSource("exposed_otm")
                exposedDb = setupExposedDatabase(exposedDs)
                createExposedTables(exposedDb)
            }

            "JPA"     -> {
                jpaDs = createDataSource("jpa_otm")
                emf = createEntityManagerFactory(jpaDs)
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        when (framework) {
            "EXPOSED" -> runCatching { exposedDs.close() }
            "JPA"     -> {
                runCatching { emf.close() }
                runCatching { jpaDs.close() }
            }
        }
    }

    // ── CREATE (Department + 20 Employees) ─────────────

    @Benchmark
    fun create(): Long {
        val seq = idSeq.incrementAndGet()
        return when (framework) {
            "EXPOSED" -> exposedCreate(seq)
            "JPA"     -> jpaCreate(seq)
            else      -> error("Unknown framework: $framework")
        }
    }

    private fun exposedCreate(seq: Long): Long {
        val now = Instant.now()
        return transaction(exposedDb) {
            val deptId = DepartmentTable.insert {
                it[name] = "Engineering Division $seq"
                it[code] = "ENG-$seq"
                it[description] = deptDescription
                it[budget] = BigDecimal("5000000.00").add(BigDecimal(seq * 100000))
                it[headCount] = employeesPerDept
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }[DepartmentTable.id]

            for (i in 1..employeesPerDept) {
                val empSeq = seq * 100 + i
                EmployeeTable.insert {
                    it[firstName] = "Employee-$empSeq"
                    it[lastName] = "Smith-$i"
                    it[email] = "emp.$empSeq@company.com"
                    it[phone] = "+82-10-${(empSeq % 9000 + 1000)}-${(empSeq % 9000 + 1000)}"
                    it[position] = POSITIONS[i % POSITIONS.size]
                    it[salary] = BigDecimal("50000.00").add(BigDecimal(i * 5000))
                    it[hireDate] = now
                    it[isActive] = true
                    it[bio] = bioText
                    it[picture] = ExposedBlob(pictureBytes)
                    it[departmentId] = deptId
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            deptId.value
        }
    }

    private fun jpaCreate(seq: Long): Long {
        val em = emf.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            val dept = DepartmentJpa(
                name = "Engineering Division $seq",
                code = "ENG-$seq",
                description = deptDescription,
                budget = BigDecimal("5000000.00").add(BigDecimal(seq * 100000)),
                headCount = employeesPerDept,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            )
            for (i in 1..employeesPerDept) {
                val empSeq = seq * 100 + i
                val emp = EmployeeJpa(
                    firstName = "Employee-$empSeq",
                    lastName = "Smith-$i",
                    email = "emp.$empSeq@company.com",
                    phone = "+82-10-${(empSeq % 9000 + 1000)}-${(empSeq % 9000 + 1000)}",
                    position = POSITIONS[i % POSITIONS.size],
                    salary = BigDecimal("50000.00").add(BigDecimal(i * 5000)),
                    hireDate = now,
                    isActive = true,
                    bio = bioText,
                    picture = pictureBytes.copyOf(),
                    department = dept,
                    createdAt = now,
                    updatedAt = now,
                )
                dept.employees.add(emp)
            }
            em.persist(dept)
            em.transaction.commit()
            dept.id
        } finally {
            em.close()
        }
    }

    // ── READ (Department + Employees eager) ─────────────

    @Benchmark
    fun read(): Any? {
        val seq = idSeq.incrementAndGet()
        val id = when (framework) {
            "EXPOSED" -> exposedCreate(seq)
            "JPA"     -> jpaCreate(seq)
            else      -> error("Unknown framework: $framework")
        }
        return when (framework) {
            "EXPOSED" -> exposedRead(id)
            "JPA"     -> jpaRead(id)
            else      -> error("Unknown framework: $framework")
        }
    }

    private fun exposedRead(deptId: Long): Any? {
        return transaction(exposedDb) {
            DepartmentEntity.findById(deptId)?.apply {
                employees.toList() // eager load employees
            }
        }
    }

    private fun jpaRead(deptId: Long): Any? {
        val em = emf.createEntityManager()
        return try {
            em.createQuery(
                "SELECT d FROM DepartmentJpa d JOIN FETCH d.employees WHERE d.id = :id",
                DepartmentJpa::class.java
            )
                .setParameter("id", deptId)
                .singleResult
        } finally {
            em.close()
        }
    }

    // ── UPDATE ──────────────────────────────────────────

    @Benchmark
    fun update(): Int {
        val seq = idSeq.incrementAndGet()
        val id = when (framework) {
            "EXPOSED" -> exposedCreate(seq)
            "JPA"     -> jpaCreate(seq)
            else      -> error("Unknown framework: $framework")
        }
        return when (framework) {
            "EXPOSED" -> exposedUpdate(id)
            "JPA"     -> jpaUpdate(id)
            else      -> error("Unknown framework: $framework")
        }
    }

    private fun exposedUpdate(deptId: Long): Int {
        val now = Instant.now()
        return transaction(exposedDb) {
            DepartmentTable.update({ DepartmentTable.id eq deptId }) {
                it[name] = "Updated Division $deptId"
                it[budget] = BigDecimal("9999999.99")
                it[headCount] = 999
                it[updatedAt] = now
            }
            EmployeeTable.update({ EmployeeTable.departmentId eq deptId }) {
                it[salary] = BigDecimal("99999.99")
                it[position] = "Senior Staff Engineer"
                it[isActive] = false
                it[updatedAt] = now
            }
        }
    }

    private fun jpaUpdate(deptId: Long): Int {
        val em = emf.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            val dept = em.createQuery(
                "SELECT d FROM DepartmentJpa d JOIN FETCH d.employees WHERE d.id = :id",
                DepartmentJpa::class.java
            )
                .setParameter("id", deptId)
                .singleResult
            dept.name = "Updated Division $deptId"
            dept.budget = BigDecimal("9999999.99")
            dept.headCount = 999
            dept.updatedAt = now
            dept.employees.forEach {
                it.salary = BigDecimal("99999.99")
                it.position = "Senior Staff Engineer"
                it.isActive = false
                it.updatedAt = now
            }
            em.merge(dept)
            em.transaction.commit()
            1 + dept.employees.size
        } finally {
            em.close()
        }
    }

    // ── DELETE (CASCADE) ────────────────────────────────

    @Benchmark
    fun delete(): Int {
        val seq = idSeq.incrementAndGet()
        val id = when (framework) {
            "EXPOSED" -> exposedCreate(seq)
            "JPA"     -> jpaCreate(seq)
            else      -> error("Unknown framework: $framework")
        }
        return when (framework) {
            "EXPOSED" -> exposedDelete(id)
            "JPA"     -> jpaDelete(id)
            else      -> error("Unknown framework: $framework")
        }
    }

    private fun exposedDelete(deptId: Long): Int {
        return transaction(exposedDb) {
            val empCount = EmployeeTable.deleteWhere { EmployeeTable.departmentId eq deptId }
            val deptCount = DepartmentTable.deleteWhere { DepartmentTable.id eq deptId }
            empCount + deptCount
        }
    }

    private fun jpaDelete(deptId: Long): Int {
        val em = emf.createEntityManager()
        return try {
            em.transaction.begin()
            val dept = em.createQuery(
                "SELECT d FROM DepartmentJpa d JOIN FETCH d.employees WHERE d.id = :id",
                DepartmentJpa::class.java
            )
                .setParameter("id", deptId)
                .singleResult
            val count = 1 + dept.employees.size
            em.remove(dept)
            em.transaction.commit()
            count
        } finally {
            em.close()
        }
    }

    // ── BATCH CREATE ───────────────────────────────────

    @Benchmark
    fun batchCreate(): Int {
        val base = idSeq.addAndGet(10)
        return when (framework) {
            "EXPOSED" -> exposedBatchCreate(base)
            "JPA"     -> jpaBatchCreate(base)
            else      -> error("Unknown framework: $framework")
        }
    }

    private fun exposedBatchCreate(base: Long): Int {
        val now = Instant.now()
        return transaction(exposedDb) {
            var count = 0
            for (d in 0 until 10) {
                val deptSeq = base + d
                val deptId = DepartmentTable.insert {
                    it[name] = "BatchDept $deptSeq"
                    it[code] = "BD-$deptSeq"
                    it[description] = deptDescription
                    it[budget] = BigDecimal("3000000.00")
                    it[headCount] = employeesPerDept
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }[DepartmentTable.id]
                count++

                for (e in 1..employeesPerDept) {
                    val empSeq = deptSeq * 100 + e
                    EmployeeTable.insert {
                        it[firstName] = "BatchEmp-$empSeq"
                        it[lastName] = "Worker-$e"
                        it[email] = "batch.emp.$empSeq@company.com"
                        it[phone] = "+82-10-${(empSeq % 9000 + 1000)}-${(empSeq % 9000 + 1000)}"
                        it[position] = POSITIONS[e % POSITIONS.size]
                        it[salary] = BigDecimal("60000.00").add(BigDecimal(e * 2000))
                        it[hireDate] = now
                        it[isActive] = true
                        it[bio] = bioText
                        it[picture] = ExposedBlob(pictureBytes)
                        it[departmentId] = deptId
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    count++
                }
            }
            count
        }
    }

    private fun jpaBatchCreate(base: Long): Int {
        val em = emf.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            var count = 0
            for (d in 0 until 10) {
                val deptSeq = base + d
                val dept = DepartmentJpa(
                    name = "BatchDept $deptSeq",
                    code = "BD-$deptSeq",
                    description = deptDescription,
                    budget = BigDecimal("3000000.00"),
                    headCount = employeesPerDept,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now,
                )
                for (e in 1..employeesPerDept) {
                    val empSeq = deptSeq * 100 + e
                    val emp = EmployeeJpa(
                        firstName = "BatchEmp-$empSeq",
                        lastName = "Worker-$e",
                        email = "batch.emp.$empSeq@company.com",
                        phone = "+82-10-${(empSeq % 9000 + 1000)}-${(empSeq % 9000 + 1000)}",
                        position = POSITIONS[e % POSITIONS.size],
                        salary = BigDecimal("60000.00").add(BigDecimal(e * 2000)),
                        hireDate = now,
                        isActive = true,
                        bio = bioText,
                        picture = pictureBytes.copyOf(),
                        department = dept,
                        createdAt = now,
                        updatedAt = now,
                    )
                    dept.employees.add(emp)
                }
                em.persist(dept)
                count += 1 + employeesPerDept
                if (d % 5 == 4) {
                    em.flush()
                    em.clear()
                }
            }
            em.transaction.commit()
            count
        } finally {
            em.close()
        }
    }

    // ── READ ALL (DAO eager loading / JOIN FETCH) ──────

    @Benchmark
    fun readAll(): Int {
        return when (framework) {
            "EXPOSED" -> exposedReadAll()
            "JPA"     -> jpaReadAll()
            else      -> error("Unknown framework: $framework")
        }
    }

    private fun exposedReadAll(): Int {
        return transaction(exposedDb) {
            DepartmentEntity.all()
                .with(DepartmentEntity::employees)
                .toList()
                .sumOf { 1 + it.employees.count().toInt() }
        }
    }

    private fun jpaReadAll(): Int {
        val em = emf.createEntityManager()
        return try {
            em.createQuery(
                "SELECT DISTINCT d FROM DepartmentJpa d JOIN FETCH d.employees",
                DepartmentJpa::class.java
            )
                .resultList
                .sumOf { 1 + it.employees.size }
        } finally {
            em.close()
        }
    }
}
