package exposed.examples.benchmark.cache

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.CurrentTimestamp
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * 캐시 벤치마크용 Exposed 테이블 정의입니다.
 */
object PayloadTable: LongIdTable("cache_payload") {
    val payload = text("payload")
    val updatedAt = timestamp("updated_at").defaultExpression(CurrentTimestamp)
}

/**
 * 캐시 전략 비교 벤치마크를 위한 PostgreSQL + HikariCP 인프라 설정입니다.
 */
object CacheBenchmarkSetup: KLogging() {

    /**
     * 벤치마크 전체에서 공유하는 PostgreSQL Testcontainer 인스턴스.
     */
    val postgres: PostgreSQLServer by lazy {
        PostgreSQLServer.Launcher.postgres
    }

    /**
     * PostgreSQL Testcontainer 기반 HikariCP DataSource를 생성합니다.
     */
    fun createDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            driverClassName = postgres.driverClassName
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 50
            isAutoCommit = false
        }
        return HikariDataSource(config)
    }

    /**
     * Exposed Database 연결을 설정합니다.
     */
    fun setupDatabase(dataSource: HikariDataSource): Database {
        return Database.connect(dataSource)
    }

    /**
     * PayloadTable을 생성합니다.
     */
    fun createTables(db: Database) {
        transaction(db) {
            SchemaUtils.create(PayloadTable)
        }
    }

    /**
     * 지정된 크기의 payload 문자열로 초기 데이터를 시드합니다.
     */
    fun seedData(db: Database, rowCount: Int, payloadBytes: Int) {
        transaction(db) {
            for (i in 1L..rowCount.toLong()) {
                PayloadTable.insert {
                    it[PayloadTable.id] = i
                    it[PayloadTable.payload] = generatePayload(i, payloadBytes)
                }
            }
        }
    }

    /**
     * 결정론적 payload 문자열을 생성합니다.
     */
    fun generatePayload(id: Long, size: Int): String {
        val base = "payload_${id}_"
        return buildString(size) {
            while (length < size) {
                append(base)
            }
        }.take(size)
    }
}
