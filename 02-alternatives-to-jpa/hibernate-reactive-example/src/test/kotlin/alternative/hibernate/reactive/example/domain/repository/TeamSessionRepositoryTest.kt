package alternative.hibernate.reactive.example.domain.repository

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import alternatives.hibernate.reactive.example.domain.model.teamOf
import alternatives.hibernate.reactive.example.domain.repository.TeamSessionRepository
import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired

class TeamSessionRepositoryTest(
    @Autowired private val sf: SessionFactory,
    @Autowired private val teamRepository: TeamSessionRepository,
): AbstractHibernateReactiveTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find by id`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            val teamId = 1L
            val teamA = teamRepository.findById(session, teamId)

            log.debug { "Team[$teamId]: $teamA" }

            teamA.shouldNotBeNull()
            teamA.id shouldBeEqualTo teamId
        }
    }

    @Test
    fun `find all teams`() = runSuspendIO {
        sf.withSessionSuspending { session ->
            val teams = teamRepository.findAll(session)

            teams.forEach {
                log.debug { "Team: $it" }
            }
            teams.shouldNotBeEmpty()
            teams.size shouldBeGreaterOrEqualTo 2
        }
    }

    @ParameterizedTest(name = "team name - {0}")
    @ValueSource(strings = ["Team A", "Team B"])
    fun `find all by name`(teamName: String) = runSuspendIO {
        sf.withSessionSuspending { session ->
            val teams = teamRepository.findAllByName(session, teamName)

            teams.forEach {
                log.debug { "Team: $it" }
            }
            teams shouldHaveSize 1

            val team = teamRepository.findById(session, teams.first().id)
            team shouldBeEqualTo teams.first()
        }
    }

    @Test
    fun `create new team`() = runSuspendIO {
        sf.withTransactionSuspending { session ->
            val newTeam = teamOf(faker.team().name())

            val savedTeam = teamRepository.save(session, newTeam)

            log.debug { "New Team: $savedTeam" }

            savedTeam.shouldNotBeNull()
            savedTeam.id.shouldNotBeNull()
        }
    }

    @Test
    fun `delete team by id`() = runSuspendIO {
        sf.withTransactionSuspending { session ->
            val newTeam = teamOf(faker.team().name())
            val savedTeam = teamRepository.save(session, newTeam)
            savedTeam.id shouldBeGreaterThan 0

            val deleted = teamRepository.deleteById(session, savedTeam.id)
            deleted.shouldBeTrue()

            teamRepository.findById(session, savedTeam.id).shouldBeNull()
        }
    }
}
