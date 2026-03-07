package exposed.examples.springmvc.controller

import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 애플리케이션 루트 경로를 처리하는 REST 컨트롤러.
 *
 * 빌드 정보를 반환하는 인덱스 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/")
class IndexController {

    @Autowired
    private val buildProperties: BuildProperties = uninitialized()

    /**
     * 애플리케이션 빌드 정보를 반환합니다.
     *
     * @return 빌드 버전, 시간 등의 정보를 담은 [BuildProperties]
     */
    @GetMapping
    fun index(): BuildProperties {
        return buildProperties
    }

}
