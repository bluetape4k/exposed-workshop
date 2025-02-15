package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberDTO
import alternatives.hibernate.reactive.example.domain.dto.TeamDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class TeamControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLogging()

    @Test
    fun `find team by id`() {
        val teamId = 1L

        val team = client.get()
            .uri("/teams/$teamId")
            .exchange()
            .expectStatus().isOk
            .expectBody<TeamDTO>()
            .returnResult().responseBody

        log.debug { "Team[1]: $team" }
        team.shouldNotBeNull()
        team.id shouldBeEqualTo teamId
        team.name shouldBeEqualTo "Team A"
    }

    @Test
    fun `find all teams`() {
        val teams = client.get()
            .uri("/teams")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<TeamDTO>()
            .returnResult().responseBody

        teams.shouldNotBeNull()
        teams.size shouldBeGreaterOrEqualTo 2

        teams.forEach {
            log.debug { "Team: $it" }
        }
    }

    @Test
    fun `find team by name`() {
        val teamName = "Team A"

        val teams = client.get()
            .uri("/teams/name/$teamName")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<TeamDTO>()
            .returnResult().responseBody!!

        teams.size shouldBeEqualTo 1
        teams.forEach {
            log.debug { "Team: $it" }
        }
    }

    @Test
    fun `find team by id with members`() {
        val teamId = 1L

        val team = client.get()
            .uri("/teams/$teamId/members")
            .exchange()
            .expectStatus().isOk
            .expectBody<TeamAndMemberDTO>()
            .returnResult().responseBody

        log.debug { "Team[1]: $team" }
        team.shouldNotBeNull()
        team.teamId shouldBeEqualTo teamId
        team.teamName shouldBeEqualTo "Team A"
        team.members.shouldNotBeEmpty()
    }

    @Test
    fun `find team by member name`() {
        val memberName = "Member 1"

        val teams = client.get()
            .uri("/teams/member/$memberName")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<TeamAndMemberDTO>()
            .returnResult().responseBody!!

        teams shouldHaveSize 1
        val team = teams.first()
        log.debug { "Team: $team" }
        team.members.shouldNotBeEmpty()
    }
}
