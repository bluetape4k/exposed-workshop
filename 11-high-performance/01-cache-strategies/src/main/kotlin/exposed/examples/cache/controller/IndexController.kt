package exposed.examples.cache.controller

import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 헬스 체크 및 기본 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping
class IndexController(private val buildProps: BuildProperties) {

    companion object: KLoggingChannel()

    @GetMapping("/")
    fun index(): BuildProperties {
        return buildProps
    }
}
