package exposed.shared.tests

import io.bluetape4k.logging.KLogging
import io.bluetape4k.testcontainers.database.CockroachServer
import io.bluetape4k.testcontainers.database.MariaDBServer
import io.bluetape4k.testcontainers.database.MySQL5Server
import io.bluetape4k.testcontainers.database.MySQL8Server
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import io.bluetape4k.utils.ShutdownQueue

/**
 * 테스트를 위한 DB 컨테이너를 제공합니다.
 */
object Containers: KLogging() {

    val MariaDB: MariaDBServer by lazy {
        MariaDBServer()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }
    val MySql5: MySQL5Server by lazy {
        MySQL5Server()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }
    val MySql8: MySQL8Server by lazy {
        MySQL8Server()
            .withCommand(
                "--character-set-server=utf8mb4",
                "--collation-server=utf8mb4_bin"
            )
            .apply {
                start()
                ShutdownQueue.register(this)
            }
    }

    val Postgres: PostgreSQLServer by lazy { PostgreSQLServer.Launcher.postgres }

    val Cockroach: CockroachServer by lazy { CockroachServer.Launcher.cockroach }

}
