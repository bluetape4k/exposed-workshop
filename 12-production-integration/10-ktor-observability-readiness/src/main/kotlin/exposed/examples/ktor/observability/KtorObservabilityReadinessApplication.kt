package exposed.examples.ktor.observability

import exposed.examples.ktor.observability.config.installKtorPlugins
import exposed.examples.ktor.observability.model.IndexResponse
import exposed.examples.ktor.observability.persistence.DiagnosticsPersistence
import exposed.examples.ktor.observability.repository.ExposedDiagnosticsRepository
import exposed.examples.ktor.observability.routes.diagnosticsRoutes
import exposed.examples.ktor.observability.routes.readinessRoutes
import exposed.examples.ktor.observability.service.DiagnosticsService
import exposed.examples.ktor.observability.service.ReadinessState
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

private const val DEFAULT_PORT = 8081

/**
 * 인메모리 H2로 Ktor 관측성 및 readiness 예제를 시작한다.
 *
 * ## 계약
 * - `/readyz`는 데이터베이스 기반 readiness 검사를 수행한다.
 * - 요청 상관관계는 `X-Request-ID`와 구조화된 오류 응답을 사용한다.
 * - 느린 작업 진단 정보는 Exposed JDBC로 저장한다.
 */
fun main() {
    embeddedServer(CIO, port = DEFAULT_PORT) {
        ktorObservabilityReadinessModule()
    }.start(wait = true)
}

internal fun Application.ktorObservabilityReadinessModule(
    persistence: DiagnosticsPersistence = DiagnosticsPersistence.inMemory(),
    readinessState: ReadinessState = ReadinessState(),
) {
    monitor.subscribe(ApplicationStopped) {
        persistence.close()
    }

    installKtorPlugins()

    val repository = ExposedDiagnosticsRepository(persistence.database)
    val service = DiagnosticsService(repository)

    routing {
        get("/") {
            call.respond(IndexResponse())
        }
        readinessRoutes(repository, readinessState)
        diagnosticsRoutes(service)
    }
}
