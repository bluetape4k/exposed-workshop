package alternatives.hibernate.reactive.example.controller

import alternatives.hibernate.reactive.example.domain.dto.MemberAndTeamDTO
import alternatives.hibernate.reactive.example.domain.dto.MemberDTO
import alternatives.hibernate.reactive.example.domain.mapper.toDto
import alternatives.hibernate.reactive.example.domain.mapper.toMemberAndTeamDTO
import alternatives.hibernate.reactive.example.domain.model.Member
import alternatives.hibernate.reactive.example.domain.repository.MemberSessionRepository
import alternatives.hibernate.reactive.example.domain.repository.TeamSessionRepository
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withStatelessSessionSuspending
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/members")
class MemberController(
    private val sf: SessionFactory,
    private val memberRepository: MemberSessionRepository,
    private val teamRepository: TeamSessionRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    @RequestMapping
    suspend fun findAll(): List<MemberDTO> {
        return sf.withSessionSuspending { session ->
            memberRepository.findAll(session).map { it.toDto() }
        }
    }

    @RequestMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): MemberDTO? {
//        return sf.withSessionSuspending { session ->
//            memberRepository.findById(session, id)?.toDto()
//        }
        // NOTE: 성능이 중요한 경우, StatelessSession 을 사용하는 것이 좋습니다.
        return sf.withStatelessSessionSuspending { session ->
            session.get(Member::class.java, id).awaitSuspending().toDto()
        }
    }

    @RequestMapping("/{id}/team")
    suspend fun findByIdWithTeam(@PathVariable id: Long): MemberAndTeamDTO? {
        return sf.withSessionSuspending { session ->
            memberRepository.findById(session, id)?.let { member ->
                // Lazy Loading 시에는 이렇게 fetch 를 해줘야 합니다. member.team 은 Eager loading 이므로 fetch 를 할 필요가 없습니다.
                // session.fetch(member.team).awaitSuspending()
                member.toMemberAndTeamDTO()
            }
        }
    }
}
