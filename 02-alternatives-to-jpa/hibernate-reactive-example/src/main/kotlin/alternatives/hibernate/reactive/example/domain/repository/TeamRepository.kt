package alternatives.hibernate.reactive.example.domain.repository

import alternatives.hibernate.reactive.example.domain.model.Team
import io.bluetape4k.logging.KLogging
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.stereotype.Repository

@Repository
class TeamRepository(sf: SessionFactory): AbstractHibernateReactiveMutinyRepository<Team, Long>(sf) {

    companion object: KLogging()

    suspend fun findById(id: Long): Team? {
        return findById(Team::class, id)
    }

    suspend fun findAll(): List<Team> {
        return findAll(Team::class)
    }

    suspend fun deleteById(id: Long): Boolean {
        return deleteById(Team::class, id)
    }
}
