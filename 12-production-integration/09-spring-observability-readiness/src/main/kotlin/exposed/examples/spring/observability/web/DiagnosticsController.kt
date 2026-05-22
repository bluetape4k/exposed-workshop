package exposed.examples.spring.observability.web

import exposed.examples.spring.observability.config.REQUEST_ID_ATTRIBUTE
import exposed.examples.spring.observability.model.OperationsResponse
import exposed.examples.spring.observability.model.toResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/diagnostics")
internal class DiagnosticsController(
    private val service: DiagnosticsService,
) {

    @GetMapping("/operations/{name}")
    fun runOperation(
        @PathVariable name: String,
        @RequestParam(defaultValue = "0") delayMs: Long,
        request: HttpServletRequest,
    ) = service.runOperation(
        name = name,
        delayMs = delayMs,
        requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE)?.toString().orEmpty(),
    ).toResponse()

    @GetMapping("/operations")
    fun findOperations(): OperationsResponse =
        OperationsResponse(
            operations = service.findOperations().map { it.toResponse() }
        )
}
