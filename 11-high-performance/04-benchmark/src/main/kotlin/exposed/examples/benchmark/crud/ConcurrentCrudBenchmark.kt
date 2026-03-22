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
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * Platform Threads vs Virtual Threads 동시성 벤치마크.
 *
 * 동일한 워크로드를 Platform Thread Pool과 Virtual Thread Executor로
 * 동시에 실행하여 처리량 차이를 비교합니다.
 *
 * - PLATFORM_EXPOSED / VIRTUAL_EXPOSED: Exposed DSL로 동시 CRUD
 * - PLATFORM_JPA / VIRTUAL_JPA: JPA로 동시 CRUD
 */
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class ConcurrentCrudBenchmark {

    @Param("PLATFORM_EXPOSED", "VIRTUAL_EXPOSED", "PLATFORM_JPA", "VIRTUAL_JPA")
    var mode: String = ""

    private lateinit var dataSource: HikariDataSource
    private var exposedDb: Database? = null
    private var emf: EntityManagerFactory? = null
    private lateinit var executor: ExecutorService

    private val idSeq = AtomicLong(0)
    private val concurrency = 50
    private val pictureBytes = ByteArray(4096) { (it % 256).toByte() }
    private val bioText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ".repeat(9)

    private val isExposed: Boolean get() = mode.endsWith("EXPOSED")
    private val isVirtual: Boolean get() = mode.startsWith("VIRTUAL")

    @Setup(Level.Trial)
    fun setup() {
        dataSource = createDataSource("concurrent_$mode")

        if (isExposed) {
            exposedDb = setupExposedDatabase(dataSource)
            createExposedTables(exposedDb!!)
        } else {
            emf = createEntityManagerFactory(dataSource)
        }

        executor = if (isVirtual) {
            Executors.newVirtualThreadPerTaskExecutor()
        } else {
            Executors.newFixedThreadPool(concurrency)
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        executor.shutdown()
        executor.awaitTermination(30, TimeUnit.SECONDS)
        emf?.close()
        dataSource.close()
    }

    // ── CONCURRENT CREATE (50건 동시 INSERT) ───────────

    @Benchmark
    fun concurrentCreate(): Int {
        val tasks = (1..concurrency).map {
            val seq = idSeq.incrementAndGet()
            Callable {
                if (isExposed) exposedInsert(seq) else jpaInsert(seq)
            }
        }
        val futures = executor.invokeAll(tasks)
        return futures.sumOf { it.get().toInt() }
    }

    // ── CONCURRENT READ (50건 동시 SELECT) ─────────────

    @Benchmark
    fun concurrentRead(): Int {
        // 먼저 50건 삽입 (순차)
        val ids = (1..concurrency).map {
            val seq = idSeq.incrementAndGet()
            if (isExposed) exposedInsert(seq) else jpaInsert(seq)
        }

        // 동시에 50건 읽기
        val tasks = ids.map { id ->
            Callable {
                if (isExposed) exposedSelect(id) else jpaSelect(id)
            }
        }
        val futures = executor.invokeAll(tasks)
        return futures.count { it.get() != null }
    }

    // ── CONCURRENT MIXED (읽기 70% + 쓰기 30%) ────────

    @Benchmark
    fun concurrentMixed(): Int {
        // 사전 데이터 30건
        val existingIds = (1..30).map {
            val seq = idSeq.incrementAndGet()
            if (isExposed) exposedInsert(seq) else jpaInsert(seq)
        }

        val tasks = (1..concurrency).map { i ->
            Callable {
                if (i % 10 < 7) {
                    // 70% 읽기
                    val id = existingIds[i % existingIds.size]
                    if (isExposed) exposedSelect(id) else jpaSelect(id)
                    1L
                } else {
                    // 30% 쓰기
                    val seq = idSeq.incrementAndGet()
                    if (isExposed) exposedInsert(seq) else jpaInsert(seq)
                }
            }
        }
        val futures = executor.invokeAll(tasks)
        return futures.sumOf { it.get().toInt() }
    }

    // ── Exposed 구현 ───────────────────────────────────

    private fun exposedInsert(seq: Long): Long {
        val now = Instant.now()
        return transaction(exposedDb!!) {
            PersonTable.insert {
                it[firstName] = "Concurrent-$seq"
                it[lastName] = "User-$seq"
                it[email] = "concurrent.$seq@example.com"
                it[phone] = "+82-10-${(seq % 9000 + 1000)}-${(seq % 9000 + 1000)}"
                it[age] = (seq % 60 + 18).toInt()
                it[address] = "$seq Concurrent St, Apt ${seq % 50}"
                it[zipcode] = "${(seq % 90000 + 10000)}"
                it[bio] = bioText
                it[picture] = ExposedBlob(pictureBytes)
                it[createdAt] = now
                it[updatedAt] = now
            }[PersonTable.id].value
        }
    }

    private fun exposedSelect(id: Long): Any? {
        return transaction(exposedDb!!) {
            PersonEntity.findById(id)
        }
    }

    // ── JPA 구현 ───────────────────────────────────────

    private fun jpaInsert(seq: Long): Long {
        val em = emf!!.createEntityManager()
        val now = Instant.now()
        return try {
            em.transaction.begin()
            val person = PersonJpa(
                firstName = "Concurrent-$seq",
                lastName = "User-$seq",
                email = "concurrent.$seq@example.com",
                phone = "+82-10-${(seq % 9000 + 1000)}-${(seq % 9000 + 1000)}",
                age = (seq % 60 + 18).toInt(),
                address = "$seq Concurrent St, Apt ${seq % 50}",
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

    private fun jpaSelect(id: Long): Any? {
        val em = emf!!.createEntityManager()
        return try {
            em.find(PersonJpa::class.java, id)
        } finally {
            em.close()
        }
    }
}
