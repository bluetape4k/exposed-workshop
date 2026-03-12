package alternative.r2dbc.example.config

import alternative.r2dbc.example.AbstractR2dbcTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import io.r2dbc.spi.ConnectionFactory
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * R2DBC 설정이 정상적으로 로드되는지 검증하는 테스트입니다.
 * [ConnectionFactory] 빈이 컨텍스트에 올바르게 등록되었는지 확인합니다.
 */
class R2dbcConfigTest: AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    @Autowired
    private val cf: ConnectionFactory = uninitialized()

    @Test
    fun `context loads`() {
        log.debug { "ConnectionFactory: $cf" }
        cf.shouldNotBeNull()
    }
}
