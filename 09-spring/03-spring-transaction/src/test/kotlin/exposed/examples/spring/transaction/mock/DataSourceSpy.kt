package exposed.examples.spring.transaction.mock

import io.bluetape4k.codec.Base58
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Logger
import javax.sql.DataSource

class DataSourceSpy(connectionSpy: (Connection) -> Connection): DataSource {

    var con: Connection = connectionSpy(
        DriverManager.getConnection(
            "jdbc:h2:mem:spy-${Base58.randomString(4)}",
            "sa",
            ""
        )
    )

    override fun getConnection() = con
    override fun getLogWriter(): PrintWriter = throw NotImplementedError()
    override fun setLogWriter(out: PrintWriter?) = throw NotImplementedError()
    override fun setLoginTimeout(seconds: Int) = throw NotImplementedError()
    override fun getLoginTimeout(): Int = throw NotImplementedError()
    override fun getParentLogger(): Logger = throw NotImplementedError()
    override fun <T: Any?> unwrap(iface: Class<T>?): T = throw NotImplementedError()
    override fun isWrapperFor(iface: Class<*>?): Boolean = throw NotImplementedError()
    override fun getConnection(username: String?, password: String?): Connection = throw NotImplementedError()
}
