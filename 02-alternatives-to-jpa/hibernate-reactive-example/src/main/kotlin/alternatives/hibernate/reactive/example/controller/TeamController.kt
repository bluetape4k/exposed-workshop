package alternatives.hibernate.reactive.example.controller

import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberRecord
import alternatives.hibernate.reactive.example.domain.dto.TeamRecord
import alternatives.hibernate.reactive.example.domain.mapper.toRecord
import alternatives.hibernate.reactive.example.domain.mapper.toTeamAndMemberRecord
import alternatives.hibernate.reactive.example.domain.repository.TeamSessionRepository
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
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

    companion object: KLoggingChannel()

    @GetMapping
    suspend fun findAll(): List<TeamRecord> {
        return sf.withSessionSuspending { session ->
            teamRepository.findAll(session).map { it.toRecord() }
        }
    }

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): TeamRecord? {
        return sf.withSessionSuspending { session ->
            teamRepository.findById(session, id)?.toRecord()
        }
    }

    @GetMapping("/name/{name}")
    suspend fun findByName(@PathVariable name: String): List<TeamRecord> {
        return sf.withSessionSuspending { session ->
            teamRepository.findAllByName(session, name).map { it.toRecord() }
        }
    }

    @GetMapping("/{id}/members")
    suspend fun findMembers(@PathVariable id: Long): TeamAndMemberRecord? {
        return sf.withSessionSuspending { session ->
            val team = teamRepository.findById(session, id)
            team?.let {
                // Lazy Loading 시에는 이렇게 fetch 를 해줘야 합니다.
                session.fetch(team.members).awaitSuspending()
                team.toTeamAndMemberRecord()
            }
        }
    }

    @GetMapping("/member/{name}")
    suspend fun findTeamsByMemberName(@PathVariable name: String): List<TeamAndMemberRecord> {
        return sf.withSessionSuspending { session ->
            teamRepository.findAllByMemberName(session, name).map { it.toTeamAndMemberRecord() }
        }
    }
}
