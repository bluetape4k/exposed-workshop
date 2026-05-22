package exposed.examples.ktor.auth.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.Closeable
import org.jetbrains.exposed.v1.jdbc.Database

internal class AuthPersistence private constructor(
    private val dataSource: HikariDataSource,
) : Closeable {

    val database: Database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }

    companion object {
        fun inMemory(databaseName: String = "ktor_auth_session"): AuthPersistence {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 4
                poolName = "ktor-auth-session-$databaseName"
            }
            return AuthPersistence(HikariDataSource(config))
        }
    }
}
