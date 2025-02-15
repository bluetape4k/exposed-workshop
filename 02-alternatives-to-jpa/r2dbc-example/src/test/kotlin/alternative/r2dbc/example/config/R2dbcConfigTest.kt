package alternative.r2dbc.example.config

import alternative.r2dbc.example.AbstractR2dbcTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import io.r2dbc.spi.ConnectionFactory
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class R2dbcConfigTest: AbstractR2dbcTest() {

    companion object: KLogging()

    @Autowired
    private val cf: ConnectionFactory = uninitialized()

    @Test
    fun `context loads`() {
        log.debug { "ConnectionFactory: $cf" }
        cf.shouldNotBeNull()
    }
}
