package exposed.examples.connection

import java.io.PrintWriter
import java.sql.Connection
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * 테스트용 [DataSource] 스텁 구현체.
 *
 * [java.util.logging.Logger] import는 [javax.sql.DataSource] 인터페이스의
 * [getParentLogger] 메서드 반환 타입으로 요구되어 포함됩니다.
 */
internal open class DataSourceStub: DataSource {

    override fun getLogWriter(): PrintWriter {
        TODO("Not yet implemented")
    }

    override fun setLogWriter(out: PrintWriter?) {
        TODO("Not yet implemented")
    }

    override fun setLoginTimeout(seconds: Int) {
        TODO("Not yet implemented")
    }

    override fun getLoginTimeout(): Int {
        TODO("Not yet implemented")
    }

    override fun getParentLogger(): Logger {
        TODO("Not yet implemented")
    }

    override fun <T: Any?> unwrap(iface: Class<T>?): T {
        TODO("Not yet implemented")
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getConnection(): Connection {
        TODO("Not yet implemented")
    }

    override fun getConnection(username: String?, password: String?): Connection {
        TODO("Not yet implemented")
    }
}
