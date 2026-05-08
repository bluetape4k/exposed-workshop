package alternative.hibernate.reactive.example.domain.repository

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.model.memberOf
import alternatives.hibernate.reactive.example.domain.repository.MemberSessionRepository
import alternatives.hibernate.reactive.example.domain.repository.TeamSessionRepository
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MemerRepositoryTest @Autowired constructor(
    private val sf: SessionFactory,
    private val memberRepository: MemberSessionRepository,
    private val teamRepository: TeamSessionRepository,
): AbstractHibernateReactiveTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find member by id`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            val memberId = 1L

            val member = memberRepository.findById(session, memberId)

            member.shouldNotBeNull()
            member.id shouldBeEqualTo memberId
        }
    }

    @Test
    fun `find all members`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            val members = memberRepository.findAll(session)

            members.shouldNotBeEmpty()
            members.size shouldBeGreaterOrEqualTo 100
        }
    }


    @Test
    fun `create new member`() = runSuspendIO {
        sf.withTransactionSuspending { session ->
            val teamA = teamRepository.findById(session, 1L)
            teamA.shouldNotBeNull()

            val member = memberOf("Member 101", 101, teamA)
            memberRepository.save(session, member)
            member.id shouldBeGreaterThan 0L
        }
    }

    @Test
    fun `delete member by id`() = runSuspendIO {
        sf.withTransactionSuspending { session ->
            val teamA = teamRepository.findById(session, 1L)!!
            teamA.members.shouldNotBeNull()

            val member = memberOf("Member 3", 40, teamA)
            memberRepository.save(session, member)

            memberRepository.deleteById(session, member.id).shouldBeTrue()

            memberRepository.findById(session, member.id).shouldBeNull()
        }
    }
}
