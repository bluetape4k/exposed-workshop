package exposed.examples.benchmark.crud

import com.zaxxer.hikari.HikariDataSource
import exposed.examples.benchmark.crud.model.PersonEntity
import exposed.examples.benchmark.crud.model.PersonJpa
import exposed.examples.benchmark.crud.model.PersonTable
import exposed.examples.benchmark.crud.setup.createDataSource
import exposed.examples.benchmark.crud.setup.createEntityManagerFactory
import exposed.examples.benchmark.crud.setup.createExposedTables
import exposed.examples.benchmark.crud.setup.setupExposedDatabase
import jakarta.persistence.EntityManagerFactory
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
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
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Single Entity (Person) CRUD 벤치마크.
 *
 * Person: 10컬럼 (firstName, lastName, email, phone, age, address, zipcode, bio, picture, timestamps)
 * 인덱스: email(unique), (firstName,lastName), zipcode
 *
 * Exposed JDBC vs JPA 를 비교합니다.
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class SingleEntityCrudBenchmark {

    @Param("EXPOSED", "JPA")
    var framework: String = ""

    private lateinit var exposedDs: HikariDataSource
    private lateinit var exposedDb: Database
    private lateinit var jpaDs: HikariDataSource
    private lateinit var emf: EntityManagerFactory

    private val idSeq = AtomicLong(0)

    /** 4KB 프로필 사진 바이너리 */
    private val pictureBytes = ByteArray(4096) { (it % 256).toByte() }

    /** 500자 바이오 텍스트 */
    private val bioText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(9)

    @Setup(Level.Trial)
    fun setup() {
        when (framework) {
            "EXPOSED" -> {
                exposedDs = createDataSource("exposed_single")
                exposedDb = setupExposedDatabase(exposedDs)
                createExposedTables(exposedDb)
            }

            "JPA"     -> {
                jpaDs = createDataSource("jpa_single")
                emf = createEntityManagerFactory(jpaDs)
            }
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        when (framework) {
            "EXPOSED" -> exposedDs.close()
            "JPA"     -> {
                emf.close()
                jpaDs.close()
            }
        }
    }

    // ── CREATE ──────────────────────────────────────────

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
            PersonTable.insert {
                it[firstName] = "John-$seq"
                it[lastName] = "Doe-$seq"
                it[email] = "john.doe.$seq@example.com"
                it[phone] = "+82-10-${(seq % 9000 + 1000)}-${(seq % 9000 + 1000)}"
                it[age] = (seq % 60 + 18).toInt()
                it[address] = "$seq Main Street, Suite ${seq % 100}, Springfield"
                it[zipcode] = "${(seq % 90000 + 10000)}"
                it[bio] = bioText
                it[picture] = ExposedBlob(pictureBytes)
                it[createdAt] = now
                it[updatedAt] = now
            }[PersonTable.id].value
        }
    }

    private fun jpaCreate(seq: Long): Long {
        val em = emf.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            val person = PersonJpa(
                firstName = "John-$seq",
                lastName = "Doe-$seq",
                email = "john.doe.$seq@example.com",
                phone = "+82-10-${(seq % 9000 + 1000)}-${(seq % 9000 + 1000)}",
                age = (seq % 60 + 18).toInt(),
                address = "$seq Main Street, Suite ${seq % 100}, Springfield",
                zipcode = "${(seq % 90000 + 10000)}",
                bio = bioText,
                picture = pictureBytes.copyOf(),
                createdAt = now,
                updatedAt = now,
            )
            em.persist(person)
            em.transaction.commit()
            person.id
        } finally {
            em.close()
        }
    }

    // ── READ ────────────────────────────────────────────

    @Benchmark
    fun read(): Any? {
        val seq = idSeq.incrementAndGet()
        // WARNING: 이 벤치마크는 측정 대상 연산 전에 create를 포함하여 측정 편향이 있습니다.
        // 순수 read 성능만 측정하려면 @Setup(Level.Invocation)에서 데이터를 사전 준비해야 합니다.
        // 교육 목적으로 JMH 사용법을 보여주는 예제입니다.
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

    private fun exposedRead(id: Long): Any? {
        return transaction(exposedDb) {
            PersonEntity.findById(id)
        }
    }

    private fun jpaRead(id: Long): Any? {
        val em = emf.createEntityManager()
        return try {
            em.find(PersonJpa::class.java, id)
        } finally {
            em.close()
        }
    }

    // ── UPDATE ──────────────────────────────────────────

    @Benchmark
    fun update(): Int {
        val seq = idSeq.incrementAndGet()
        // WARNING: 이 벤치마크는 측정 대상 연산 전에 create를 포함하여 측정 편향이 있습니다.
        // 순수 update 성능만 측정하려면 @Setup(Level.Invocation)에서 데이터를 사전 준비해야 합니다.
        // 교육 목적으로 JMH 사용법을 보여주는 예제입니다.
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

    private fun exposedUpdate(id: Long): Int {
        val now = Instant.now()
        return transaction(exposedDb) {
            PersonTable.update({ PersonTable.id eq id }) {
                it[firstName] = "Updated-John"
                it[lastName] = "Updated-Doe"
                it[age] = 99
                it[address] = "999 Updated Avenue, Floor 42"
                it[zipcode] = "99999"
                it[bio] = "Updated bio: $bioText"
                it[updatedAt] = now
            }
        }
    }

    private fun jpaUpdate(id: Long): Int {
        val em = emf.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            val person = em.find(PersonJpa::class.java, id)
            person.firstName = "Updated-John"
            person.lastName = "Updated-Doe"
            person.age = 99
            person.address = "999 Updated Avenue, Floor 42"
            person.zipcode = "99999"
            person.bio = "Updated bio: $bioText"
            person.updatedAt = now
            em.merge(person)
            em.transaction.commit()
            1
        } finally {
            em.close()
        }
    }

    // ── DELETE ──────────────────────────────────────────

    @Benchmark
    fun delete(): Int {
        val seq = idSeq.incrementAndGet()
        // WARNING: 이 벤치마크는 측정 대상 연산 전에 create를 포함하여 측정 편향이 있습니다.
        // 순수 delete 성능만 측정하려면 @Setup(Level.Invocation)에서 데이터를 사전 준비해야 합니다.
        // 교육 목적으로 JMH 사용법을 보여주는 예제입니다.
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

    private fun exposedDelete(id: Long): Int {
        return transaction(exposedDb) {
            PersonTable.deleteWhere { PersonTable.id eq id }
        }
    }

    private fun jpaDelete(id: Long): Int {
        val em = emf.createEntityManager()
        return try {
            em.transaction.begin()
            val person = em.find(PersonJpa::class.java, id)
            if (person != null) {
                em.remove(person)
                em.transaction.commit()
                1
            } else {
                em.transaction.rollback()
                0
            }
        } finally {
            em.close()
        }
    }

    // ── BATCH CREATE ───────────────────────────────────

    @Benchmark
    fun batchCreate(): Int {
        val base = idSeq.addAndGet(100)
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
            for (i in 0 until 100) {
                val seq = base + i
                PersonTable.insert {
                    it[firstName] = "Batch-$seq"
                    it[lastName] = "User-$seq"
                    it[email] = "batch.user.$seq@example.com"
                    it[phone] = "+82-10-${(seq % 9000 + 1000)}-${(seq % 9000 + 1000)}"
                    it[age] = (i % 60 + 18)
                    it[address] = "$seq Batch Road, Apt ${i % 50}"
                    it[zipcode] = "${(seq % 90000 + 10000)}"
                    it[bio] = bioText
                    it[picture] = ExposedBlob(pictureBytes)
                    it[createdAt] = now
                    it[updatedAt] = now
                }
                count++
            }
            count
        }
    }

    private fun jpaBatchCreate(base: Long): Int {
        val em = emf.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            for (i in 0 until 100) {
                val seq = base + i
                val person = PersonJpa(
                    firstName = "Batch-$seq",
                    lastName = "User-$seq",
                    email = "batch.user.$seq@example.com",
                    phone = "+82-10-${(seq % 9000 + 1000)}-${(seq % 9000 + 1000)}",
                    age = (i % 60 + 18),
                    address = "$seq Batch Road, Apt ${i % 50}",
                    zipcode = "${(seq % 90000 + 10000)}",
                    bio = bioText,
                    picture = pictureBytes.copyOf(),
                    createdAt = now,
                    updatedAt = now,
                )
                em.persist(person)
                if (i % 50 == 49) {
                    em.flush()
                    em.clear()
                }
            }
            em.transaction.commit()
            100
        } finally {
            em.close()
        }
    }

    // ── READ ALL (DAO / JPQL) ──────────────────────────

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
            PersonEntity.all().toList().size
        }
    }

    private fun jpaReadAll(): Int {
        val em = emf.createEntityManager()
        return try {
            em.createQuery("SELECT p FROM PersonJpa p", PersonJpa::class.java)
                .resultList.size
        } finally {
            em.close()
        }
    }
}
