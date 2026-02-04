package alternative.hibernate.reactive.example.controller

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.dto.TeamAndMemberRecord
import alternatives.hibernate.reactive.example.domain.dto.TeamRecord
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class TeamControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractHibernateReactiveTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find team by id`() = runSuspendIO {
        val teamId = 1L

        val team = client
            .httpGet("/teams/$teamId")
            .expectStatus().is2xxSuccessful
            .returnResult<TeamRecord>().responseBody
            .awaitSingle()

        log.debug { "Team[1]: $team" }
        team.shouldNotBeNull()
        team.id shouldBeEqualTo teamId
        team.name shouldBeEqualTo "Team A"
    }

    @Test
    fun `find all teams`() = runSuspendIO {
        val teams = client
            .httpGet("/teams")
            .expectStatus().is2xxSuccessful
            .expectBodyList<TeamRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        teams.size shouldBeGreaterOrEqualTo 2

        teams.forEach {
            log.debug { "Team: $it" }
        }
    }

    @Test
    fun `find team by name`() = runSuspendIO {
        val teamName = "Team A"

        val teams = client
            .httpGet("/teams/name/$teamName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<TeamRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        teams.size shouldBeEqualTo 1
        teams.forEach {
            log.debug { "Team: $it" }
        }
    }

    @Test
    fun `find team by id with members`() = runSuspendIO {
        val teamId = 1L

        val team = client
            .httpGet("/teams/$teamId/members")
            .expectStatus().is2xxSuccessful
            .returnResult<TeamAndMemberRecord>().responseBody
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

        val teams = client
            .httpGet("/teams/member/$memberName")
            .expectStatus().is2xxSuccessful
            .expectBodyList<TeamAndMemberRecord>()
            .returnResult().responseBody
            .shouldNotBeNull()

        teams shouldHaveSize 1
        val team = teams.first()
        log.debug { "Team: $team" }
        team.members.shouldNotBeEmpty()
    }
}
