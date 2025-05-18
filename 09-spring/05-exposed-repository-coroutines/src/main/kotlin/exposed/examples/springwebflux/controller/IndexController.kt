package exposed.examples.springwebflux.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/")
class IndexController {

    companion object: KLoggingChannel()

    @Autowired
    private val buildProperties: BuildProperties = uninitialized()


    @GetMapping
    suspend fun index(): BuildProperties {
        return buildProperties
    }
}
