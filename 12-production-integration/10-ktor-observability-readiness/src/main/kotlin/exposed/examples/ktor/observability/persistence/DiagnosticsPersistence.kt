package exposed.examples.ktor.observability.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.codec.Base58
import org.jetbrains.exposed.v1.jdbc.Database

private const val HIKARI_MAX_POOL_SIZE = 4

internal class DiagnosticsPersistence private constructor(
    private val dataSource: HikariDataSource,
) : AutoCloseable {

    val database: Database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }

    companion object {
        fun inMemory(name: String = "ktor_observability_${Base58.randomString(8)}"): DiagnosticsPersistence {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                username = "sa"
                password = ""
                driverClassName = "org.h2.Driver"
                maximumPoolSize = HIKARI_MAX_POOL_SIZE
                poolName = "ktor-observability-$name"
            }
            return DiagnosticsPersistence(HikariDataSource(config))
        }
    }
}
