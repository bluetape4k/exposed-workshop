package exposed.workshop.springmvc.controller

import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.info.BuildProperties
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 루트 경로(`/`)에 대한 인덱스 컨트롤러.
 *
 * 빌드 정보를 반환하여 애플리케이션 상태를 확인할 수 있습니다.
 */
@RestController
@RequestMapping("/")
class IndexController {

    @Autowired
    private val buildProperties: BuildProperties = uninitialized()

    /**
     * 애플리케이션 빌드 정보를 반환합니다.
     *
     * @return 빌드 버전, 시간 등의 정보가 담긴 [BuildProperties]
     */
    @GetMapping
    fun index(): BuildProperties {
        return buildProperties
    }
}
