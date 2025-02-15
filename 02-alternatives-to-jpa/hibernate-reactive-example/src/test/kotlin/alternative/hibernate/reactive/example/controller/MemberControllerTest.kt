package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.dto.MemberAndTeamDTO
import alternatives.hibernate.reactive.example.domain.dto.MemberDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class MemberControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLogging()

    @Test
    fun `find all members`() {
        val members = client.get()
            .uri("/members")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<MemberDTO>()
            .returnResult().responseBody!!

        members.shouldNotBeEmpty()
        members.forEach {
            log.debug { "Member: $it" }
        }
    }

    @Test
    fun `find member by id`() {
        val memberId = 1L

        val member = client.get()
            .uri("/members/$memberId")
            .exchange()
            .expectStatus().isOk
            .expectBody<MemberDTO>()
            .returnResult().responseBody!!

        log.debug { "Member[1]: $member" }
        member.shouldNotBeNull()
        member.id shouldBeEqualTo memberId
    }

    @Test
    fun `find member by id with team`() {
        val memberId = 1L

        val member = client.get()
            .uri("/members/$memberId/team")
            .exchange()
            .expectStatus().isOk
            .expectBody<MemberAndTeamDTO>()
            .returnResult().responseBody!!

        log.debug { "Member[1]: $member" }
        member.shouldNotBeNull()
        member.id shouldBeEqualTo memberId
        member.team.shouldNotBeNull()
        member.team.name shouldBeEqualTo "Team A"
    }
}
