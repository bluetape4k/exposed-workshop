package alternative.vertx.sqlclient.example

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.vertx.core.Vertx
import io.vertx.jdbcclient.JDBCConnectOptions
import io.vertx.jdbcclient.JDBCPool
import io.vertx.junit5.VertxExtension
import io.vertx.kotlin.jdbcclient.jdbcConnectOptionsOf
import io.vertx.kotlin.mysqlclient.mySQLConnectOptionsOf
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.kotlin.sqlclient.poolOptionsOf
import io.vertx.mysqlclient.MySQLBuilder
import io.vertx.mysqlclient.MySQLConnectOptions
import io.vertx.pgclient.PgBuilder
import io.vertx.pgclient.PgConnectOptions
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.PoolOptions
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)  // VertxTestContext 를 사용하기 위해서 추가합니다.
abstract class AbstractSqlClientTest {

    companion object: KLoggingChannel() {

        @JvmStatic
        val faker = Fakers.faker

        private val defaultPoolOptions: PoolOptions = poolOptionsOf(maxSize = 20)


        private val h2ConnectionOptions: JDBCConnectOptions by lazy {
            jdbcConnectOptionsOf(
                jdbcUrl = "jdbc:h2:mem:test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;DATABASE_TO_UPPER=FALSE;",
                user = "sa"
            )
        }

        fun Vertx.getH2Pool(
            connectOptions: JDBCConnectOptions = h2ConnectionOptions,
            poolOptions: PoolOptions = defaultPoolOptions,
        ): JDBCPool = JDBCPool.pool(this, connectOptions, poolOptions)

        private val mysql by lazy { MySQL8Server.Launcher.mysql }

        fun MySQL8Server.connectOptions(): MySQLConnectOptions =
            mySQLConnectOptionsOf(
                host = host,
                port = port,
                database = databaseName,
                user = username,
                password = password
            )

        fun Vertx.getMySQLPool(
            connectOptions: MySQLConnectOptions = mysql.connectOptions(),
            poolOptions: PoolOptions = defaultPoolOptions,
        ): Pool {
            connectOptions.host.requireNotBlank("host")

            return MySQLBuilder
                .pool().with(poolOptions)
                .connectingTo(connectOptions)
                .using(this@getMySQLPool)
                .build()

        }

        private val postgres by lazy { PostgreSQLServer.Launcher.postgres }

        fun PostgreSQLServer.connectOptions(): PgConnectOptions =
            pgConnectOptionsOf(
                host = host,
                port = port,
                database = databaseName,
                user = username,
                password = password
            )

        fun Vertx.getPostgresPool(
            connectOptions: PgConnectOptions = postgres.connectOptions(),
            poolOptions: PoolOptions = defaultPoolOptions,
        ): Pool {
            connectOptions.host.requireNotBlank("host")

            return PgBuilder
                .pool().with(poolOptions)
                .connectingTo(connectOptions)
                .using(this@getPostgresPool)
                .build()
        }
    }
}
