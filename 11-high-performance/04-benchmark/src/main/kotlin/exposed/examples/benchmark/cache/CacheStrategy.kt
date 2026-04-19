package exposed.examples.benchmark.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

/**
 * 캐시 전략 비교 벤치마크에서 사용하는 payload 데이터 클래스입니다.
 */
data class CachePayload(
    val id: Long,
    val payload: String,
): java.io.Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }
}

/**
 * 캐시 전략 인터페이스 — NoCache, ReadThrough, WriteThrough 구현을 통합합니다.
 */
interface CacheStrategy {

    /**
     * 주어진 ID로 데이터를 읽습니다.
     */
    fun read(id: Long): CachePayload?

    /**
     * 주어진 ID에 데이터를 씁니다.
     */
    fun write(id: Long, payload: CachePayload)
}

/**
 * 캐시 없이 직접 DB를 조회하는 전략입니다.
 */
class NoCacheStrategy(private val db: Database): CacheStrategy {

    companion object: KLogging()

    override fun read(id: Long): CachePayload? {
        return transaction(db) {
            PayloadTable.selectAll()
                .where { PayloadTable.id eq id }
                .firstOrNull()
                ?.let { row ->
                    CachePayload(
                        id = row[PayloadTable.id].value,
                        payload = row[PayloadTable.payload],
                    )
                }
        }
    }

    override fun write(id: Long, payload: CachePayload) {
        transaction(db) {
            val updated = PayloadTable.update({ PayloadTable.id eq id }) {
                it[PayloadTable.payload] = payload.payload
            }
            if (updated == 0) {
                PayloadTable.insert {
                    it[PayloadTable.id] = id
                    it[PayloadTable.payload] = payload.payload
                }
            }
        }
    }
}

/**
 * Read-Through 캐시 전략: 읽기 시 캐시 우선 조회, 쓰기 시 DB 기록 후 캐시 무효화.
 */
class ReadThroughStrategy(private val db: Database): CacheStrategy {

    companion object: KLogging()

    private val cache: Cache<Long, CachePayload> = Caffeine.newBuilder()
        .maximumSize(4096)
        .build()

    override fun read(id: Long): CachePayload? {
        return cache.getIfPresent(id) ?: run {
            val loaded = dbRead(id)
            if (loaded != null) {
                cache.put(id, loaded)
            }
            loaded
        }
    }

    override fun write(id: Long, payload: CachePayload) {
        dbWrite(id, payload)
        cache.invalidate(id)
    }

    private fun dbRead(id: Long): CachePayload? {
        return transaction(db) {
            PayloadTable.selectAll()
                .where { PayloadTable.id eq id }
                .firstOrNull()
                ?.let { row ->
                    CachePayload(
                        id = row[PayloadTable.id].value,
                        payload = row[PayloadTable.payload],
                    )
                }
        }
    }

    private fun dbWrite(id: Long, payload: CachePayload) {
        transaction(db) {
            val updated = PayloadTable.update({ PayloadTable.id eq id }) {
                it[PayloadTable.payload] = payload.payload
            }
            if (updated == 0) {
                PayloadTable.insert {
                    it[PayloadTable.id] = id
                    it[PayloadTable.payload] = payload.payload
                }
            }
        }
    }
}

/**
 * Write-Through 캐시 전략: 쓰기 시 캐시와 DB에 동기적으로 기록, 읽기 시 캐시 우선.
 */
class WriteThroughStrategy(private val db: Database): CacheStrategy {

    companion object: KLogging()

    private val cache: Cache<Long, CachePayload> = Caffeine.newBuilder()
        .maximumSize(4096)
        .build()

    override fun read(id: Long): CachePayload? {
        return cache.getIfPresent(id) ?: run {
            val loaded = dbRead(id)
            if (loaded != null) {
                cache.put(id, loaded)
            }
            loaded
        }
    }

    override fun write(id: Long, payload: CachePayload) {
        cache.put(id, payload)
        dbWrite(id, payload)
    }

    private fun dbRead(id: Long): CachePayload? {
        return transaction(db) {
            PayloadTable.selectAll()
                .where { PayloadTable.id eq id }
                .firstOrNull()
                ?.let { row ->
                    CachePayload(
                        id = row[PayloadTable.id].value,
                        payload = row[PayloadTable.payload],
                    )
                }
        }
    }

    private fun dbWrite(id: Long, payload: CachePayload) {
        transaction(db) {
            val updated = PayloadTable.update({ PayloadTable.id eq id }) {
                it[PayloadTable.payload] = payload.payload
            }
            if (updated == 0) {
                PayloadTable.insert {
                    it[PayloadTable.id] = id
                    it[PayloadTable.payload] = payload.payload
                }
            }
        }
    }
}
