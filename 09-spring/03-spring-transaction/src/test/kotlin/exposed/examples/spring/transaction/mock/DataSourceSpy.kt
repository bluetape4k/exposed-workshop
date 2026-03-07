package exposed.examples.spring.transaction.mock

import io.bluetape4k.codec.Base58
import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * 테스트용 [DataSource] 스파이 구현체. 커넥션 획득 시 [connectionSpy] 람다로 래핑하여 동작을 검증한다.
 *
 * [java.util.logging.Logger] import는 [javax.sql.DataSource] 인터페이스의
 * [getParentLogger] 메서드 반환 타입으로 요구되어 포함됩니다.
 */
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
