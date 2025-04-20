package exposed.examples.suspendedcache.domain.repository

import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

class DefaultCountrySuspendedRepositoryTest: AbstractCountrySuspendedRepositoryTest() {

    @Autowired
    @Qualifier("defaultCountrySuspendedRepository")
    override val countrySuspendedRepository: CountrySuspendedRepository = uninitialized()
}
