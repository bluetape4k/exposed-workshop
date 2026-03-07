package exposed.workshop.springmvc.controller

import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 인덱스(루트) 경로 REST 컨트롤러.
 *
 * `/` 경로로 애플리케이션 빌드 정보를 제공합니다.
 */
@RestController
@RequestMapping("/")
class IndexController {

    @Autowired
    private val buildProperties: BuildProperties = uninitialized()

    /**
     * 애플리케이션 빌드 정보를 반환합니다.
     *
     * HTTP GET `/`
     *
     * @return 버전, 빌드 시간 등의 메타정보가 담긴 [BuildProperties]
     */
    @GetMapping
    fun index(): BuildProperties {
        return buildProperties
    }
}
