package exposed.examples.suspendedcache.domain.repository

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

/** 캐시 없이 DB에서 직접 조회하는 `DefaultCountrySuspendedRepository`의 기본 CRUD 동작을 검증합니다. */
class DefaultCountrySuspendedRepositoryTest: AbstractCountrySuspendedRepositoryTest() {

    companion object: KLoggingChannel()

    @Autowired
    @Qualifier("defaultCountrySuspendedRepository")
    override val countrySuspendedRepository: CountrySuspendedRepository = uninitialized()
}
