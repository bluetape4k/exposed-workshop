package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.dto.MemberAndTeamDTO
import alternatives.hibernate.reactive.example.domain.dto.MemberDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class MemberControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find all members`() = runSuspendIO {
        val members = client.get()
            .uri("/members")
            .exchange()
            .expectStatus().isOk
            .returnResult<MemberDTO>().responseBody
            .asFlow()
            .toList()

        members.shouldNotBeEmpty()
        members.forEach {
            log.debug { "Member: $it" }
        }
    }

    @Test
    fun `find member by id`() = runSuspendIO {
        val memberId = 1L

        val member = client.get()
            .uri("/members/$memberId")
            .exchange()
            .expectStatus().isOk
            .returnResult<MemberDTO>().responseBody
            .awaitSingle()

        log.debug { "Member[1]: $member" }
        member.shouldNotBeNull()
        member.id shouldBeEqualTo memberId
    }

    @Test
    fun `find member by id with team`() = runSuspendIO {
        val memberId = 1L

        val member = client.get()
            .uri("/members/$memberId/team")
            .exchange()
            .expectStatus().isOk
            .returnResult<MemberAndTeamDTO>().responseBody
            .awaitSingle()

        log.debug { "Member[1]: $member" }
        member.shouldNotBeNull()
        member.id shouldBeEqualTo memberId
        member.team.shouldNotBeNull()
        member.team.name shouldBeEqualTo "Team A"
    }
}
