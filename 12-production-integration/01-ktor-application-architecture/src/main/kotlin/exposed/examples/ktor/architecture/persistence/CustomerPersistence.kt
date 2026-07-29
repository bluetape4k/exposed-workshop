package exposed.examples.ktor.architecture.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.codec.Base58
import org.jetbrains.exposed.v1.jdbc.Database

private const val HIKARI_MAX_POOL_SIZE = 4

/**
 * Ktor 애플리케이션 인스턴스가 소유하는 JDBC 자원을 보관한다.
 */
internal class CustomerPersistence private constructor(
    private val dataSource: HikariDataSource,
) : AutoCloseable {

    val database: Database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }

    companion object {
        fun inMemory(name: String = "ktor_architecture_${Base58.randomString(8)}"): CustomerPersistence {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"
                username = "sa"
                password = ""
                driverClassName = "org.h2.Driver"
                maximumPoolSize = HIKARI_MAX_POOL_SIZE
                poolName = "ktor-architecture-$name"
            }
            return CustomerPersistence(HikariDataSource(config))
        }
    }
}
