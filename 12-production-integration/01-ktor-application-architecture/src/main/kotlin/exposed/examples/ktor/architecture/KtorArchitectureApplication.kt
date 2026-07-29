package exposed.examples.ktor.architecture

import exposed.examples.ktor.architecture.config.installKtorPlugins
import exposed.examples.ktor.architecture.model.HealthResponse
import exposed.examples.ktor.architecture.model.IndexResponse
import exposed.examples.ktor.architecture.persistence.CustomerPersistence
import exposed.examples.ktor.architecture.repository.ExposedCustomerRepository
import exposed.examples.ktor.architecture.routes.customerRoutes
import exposed.examples.ktor.architecture.service.CustomerService
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8080

/**
 * 인메모리 H2 데이터베이스로 Ktor 아키텍처 예제를 시작한다.
 *
 * ## 계약
 * - 경로 계층은 얇게 유지하고 서비스에 처리를 위임한다.
 * - 저장소는 모든 블로킹 Exposed JDBC 트랜잭션을
 *   `Dispatchers.IO` 뒤에서 책임진다.
 * - JSON 오류는 `StatusPages`를 통해 정제된 응답으로 매핑한다.
 */
fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorArchitectureModule()
    }.start(wait = true)
}

/**
 * Ktor + Exposed 아키텍처 예제를 구성한다.
 *
 * 기본 영속성 구성은 로컬 실행을 위해 인메모리 H2 데이터베이스를 사용한다.
 * 테스트는 테스트별 고유 JDBC URL을 가진 전용 영속성 인스턴스를 전달한다.
 */
internal fun Application.ktorArchitectureModule(
    persistence: CustomerPersistence = CustomerPersistence.inMemory(),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    installKtorPlugins()

    val repository = ExposedCustomerRepository(persistence.database)
    val service = CustomerService(repository)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        get("/health") {
            call.respond(HealthResponse(status = "UP"))
        }
        customerRoutes(service)
    }
}
