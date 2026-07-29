package exposed.examples.spring.architecture.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.bluetape4k.codec.Base58
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Spring 예제에서 사용하는 Hikari 데이터소스와 Exposed 데이터베이스 핸들을 소유한다.
 */
internal class CustomerPersistence(
    private val dataSource: HikariDataSource,
) : AutoCloseable {

    val database: Database = Database.connect(dataSource)

    override fun close() {
        dataSource.close()
    }

    companion object {
        private const val HIKARI_MAX_POOL_SIZE = 4

        fun inMemory(name: String = Base58.randomString(8)): CustomerPersistence {
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:h2:mem:spring_architecture_$name;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
                username = "sa"
                password = ""
                driverClassName = "org.h2.Driver"
                maximumPoolSize = HIKARI_MAX_POOL_SIZE
                poolName = "spring-architecture-$name"
            }
            return CustomerPersistence(HikariDataSource(config))
        }
    }
}

