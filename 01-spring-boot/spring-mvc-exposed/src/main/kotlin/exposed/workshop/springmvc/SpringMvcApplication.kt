package exposed.workshop.springmvc

import exposed.workshop.springmvc.domain.DatabaseInitializer
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringMvcApplication: ApplicationRunner {

    companion object: KLogging()

    @Autowired
    private val databaseInitializer: DatabaseInitializer = uninitialized()

    override fun run(args: ApplicationArguments?) {
        log.debug { "데이터베이스 초기화 및 샘플 데이터 추가" }
        databaseInitializer.createSchemaAndPopulateData()
    }

}

fun main(vararg args: String) {
    runApplication<SpringMvcApplication>(*args) {
        webApplicationType = WebApplicationType.SERVLET
    }
}
