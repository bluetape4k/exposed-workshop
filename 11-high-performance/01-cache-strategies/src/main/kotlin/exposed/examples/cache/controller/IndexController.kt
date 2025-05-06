package exposed.examples.cache.controller

import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class IndexController(private val buildProps: BuildProperties) {

    @GetMapping("/")
    fun index(): BuildProperties {
        return buildProps
    }
}
