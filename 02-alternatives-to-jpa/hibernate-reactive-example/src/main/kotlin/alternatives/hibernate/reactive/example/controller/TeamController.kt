package alternatives.hibernate.reactive.example.controller

import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberDTO
import alternatives.hibernate.reactive.example.domain.dto.TeamDTO
import alternatives.hibernate.reactive.example.domain.mapper.toDto
import alternatives.hibernate.reactive.example.domain.mapper.toTeamAndMemberDTO
import alternatives.hibernate.reactive.example.domain.repository.TeamSessionRepository
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/teams")
class TeamController(
    private val sf: SessionFactory,
    private val teamRepository: TeamSessionRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    @GetMapping
    suspend fun findAll(): List<TeamDTO> {
        return sf.withSessionSuspending { session ->
            teamRepository.findAll(session).map { it.toDto() }
        }
    }

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): TeamDTO? {
        return sf.withSessionSuspending { session ->
            teamRepository.findById(session, id)?.toDto()
        }
    }

    @GetMapping("/name/{name}")
    suspend fun findByName(@PathVariable name: String): List<TeamDTO> {
        return sf.withSessionSuspending { session ->
            teamRepository.findAllByName(session, name).map { it.toDto() }
        }
    }

    @GetMapping("/{id}/members")
    suspend fun findMembers(@PathVariable id: Long): TeamAndMemberDTO? {
        return sf.withSessionSuspending { session ->
            val team = teamRepository.findById(session, id)
            team?.let {
                // Lazy Loading 시에는 이렇게 fetch 를 해줘야 합니다.
                session.fetch(team.members).awaitSuspending()
                team.toTeamAndMemberDTO()
            }
        }
    }

    @GetMapping("/member/{name}")
    suspend fun findTeamsByMemberName(@PathVariable name: String): List<TeamAndMemberDTO> {
        return sf.withSessionSuspending { session ->
            teamRepository.findAllByMemberName(session, name).map { it.toTeamAndMemberDTO() }
        }
    }
}
