package exposed.examples.spring.realtime.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import java.io.Closeable

internal class NotificationPersistence private constructor(
    private val dataSource: HikariDataSource,
) : Closeable {

    val database: Database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }

    companion object {
        fun inMemory(databaseName: String = "spring_outbox_realtime"): NotificationPersistence {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
                driverClassName = "org.h2.Driver"
                username = "sa"
                password = ""
                maximumPoolSize = 4
                poolName = "spring-outbox-realtime-$databaseName"
            }
            return NotificationPersistence(HikariDataSource(config))
        }
    }
}
