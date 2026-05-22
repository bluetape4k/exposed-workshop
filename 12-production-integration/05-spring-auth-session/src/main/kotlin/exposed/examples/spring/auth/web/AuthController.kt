package exposed.examples.spring.auth.web

import exposed.examples.spring.auth.model.ProfileResponse
import exposed.examples.spring.auth.model.SessionsResponse
import exposed.examples.spring.auth.model.toProfileResponse
import exposed.examples.spring.auth.model.toResponse
import exposed.examples.spring.auth.repository.AuthRepository
import java.security.Principal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
internal class AuthController(
    private val repository: AuthRepository,
) {

    @GetMapping("/public")
    fun publicEndpoint(): Map<String, String> =
        mapOf("status" to "anonymous")

    @GetMapping("/profile")
    fun profile(principal: Principal): ProfileResponse =
        repository.findUser(principal.name)?.toProfileResponse()
            ?: error("Authenticated user ${principal.name} was not found")

    @GetMapping("/admin")
    fun admin(principal: Principal): Map<String, String> =
        mapOf("status" to "admin", "username" to principal.name)

    @PostMapping("/sessions")
    fun createSession(principal: Principal) =
        repository.createSession(principal.name).toResponse()

    @GetMapping("/sessions")
    fun sessions(principal: Principal): SessionsResponse =
        SessionsResponse(
            sessions = repository.findSessions(principal.name).map { it.toResponse() }
        )
}
