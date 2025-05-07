package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberDTO
import alternatives.hibernate.reactive.example.domain.dto.TeamDTO
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.returnResult

class TeamControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLogging()

    @Test
    fun `find team by id`() = runSuspendIO {
        val teamId = 1L

        val team = client.get()
            .uri("/teams/$teamId")
            .exchange()
            .expectStatus().isOk
            .returnResult<TeamDTO>().responseBody
            .awaitSingle()

        log.debug { "Team[1]: $team" }
        team.shouldNotBeNull()
        team.id shouldBeEqualTo teamId
        team.name shouldBeEqualTo "Team A"
    }

    @Test
    fun `find all teams`() = runSuspendIO {
        val teams = client.get()
            .uri("/teams")
            .exchange()
            .expectStatus().isOk
            .returnResult<TeamDTO>().responseBody
            .asFlow()
            .toList()

        teams.shouldNotBeNull()
        teams.size shouldBeGreaterOrEqualTo 2

        teams.forEach {
            log.debug { "Team: $it" }
        }
    }

    @Test
    fun `find team by name`() = runSuspendIO {
        val teamName = "Team A"

        val teams = client.get()
            .uri("/teams/name/$teamName")
            .exchange()
            .expectStatus().isOk
            .returnResult<TeamDTO>().responseBody
            .asFlow()
            .toList()

        teams.size shouldBeEqualTo 1
        teams.forEach {
            log.debug { "Team: $it" }
        }
    }

    @Test
    fun `find team by id with members`() = runSuspendIO {
        val teamId = 1L

        val team = client.get()
            .uri("/teams/$teamId/members")
            .exchange()
            .expectStatus().isOk
            .returnResult<TeamAndMemberDTO>().responseBody
            .awaitSingle()

        log.debug { "Team[1]: $team" }
        team.shouldNotBeNull()
        team.teamId shouldBeEqualTo teamId
        team.teamName shouldBeEqualTo "Team A"
        team.members.shouldNotBeEmpty()
    }

    @Test
    fun `find team by member name`() = runSuspendIO {
        val memberName = "Member 1"

        val teams = client.get()
            .uri("/teams/member/$memberName")
            .exchange()
            .expectStatus().isOk
            .returnResult<TeamAndMemberDTO>().responseBody
            .asFlow()
            .toList()

        teams shouldHaveSize 1
        val team = teams.first()
        log.debug { "Team: $team" }
        team.members.shouldNotBeEmpty()
    }
}
