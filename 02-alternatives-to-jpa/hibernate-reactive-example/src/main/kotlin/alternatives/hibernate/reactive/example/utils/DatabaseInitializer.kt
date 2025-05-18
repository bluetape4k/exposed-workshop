package alternatives.hibernate.reactive.example.utils

import alternatives.hibernate.reactive.example.domain.model.memberOf
import alternatives.hibernate.reactive.example.domain.model.teamOf
import alternatives.hibernate.reactive.example.domain.repository.MemberSessionRepository
import alternatives.hibernate.reactive.example.domain.repository.TeamSessionRepository
import io.bluetape4k.hibernate.reactive.mutiny.withTransactionSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.uninitialized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DatabaseInitializer: ApplicationRunner {

    companion object: KLoggingChannel()

    @Autowired
    private val sf: SessionFactory = uninitialized()

    @Autowired
    private val teamRepository: TeamSessionRepository = uninitialized()

    @Autowired
    private val memberRepository: MemberSessionRepository = uninitialized()

    override fun run(args: ApplicationArguments?) {
        log.debug { "Initialize sample data..." }

        runBlocking(Dispatchers.IO) {
            sf.withTransactionSuspending { session ->
                val teamA = teamOf("Team A")
                val teamB = teamOf("Team B")
                teamRepository.saveAll(session, teamA, teamB)

                val members = List(100) {
                    val selectedTeam = if (it % 2 == 0) teamA else teamB
                    memberOf("Member $it", it, selectedTeam)
                }
                memberRepository.saveAll(session, *members.toTypedArray())
            }
        }

    }
}
