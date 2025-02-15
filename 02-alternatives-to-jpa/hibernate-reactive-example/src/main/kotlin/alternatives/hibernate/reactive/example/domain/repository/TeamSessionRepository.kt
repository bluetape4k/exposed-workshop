package alternatives.hibernate.reactive.example.domain.repository

import alternatives.hibernate.reactive.example.domain.model.Member_
import alternatives.hibernate.reactive.example.domain.model.Team
import alternatives.hibernate.reactive.example.domain.model.Team_
import io.bluetape4k.logging.KLogging
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import org.hibernate.reactive.mutiny.Mutiny.Session
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.stereotype.Repository

@Repository
class TeamSessionRepository(
    sf: SessionFactory,
): AbstractMutinySessionRepository<Team, Long>(sf) {

    companion object: KLogging()

    suspend fun findById(session: Session, id: Long): Team? {
        return findById(session, Team::class, id)
    }

    suspend fun findAllByName(session: Session, name: String): List<Team> {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(Team::class.java)
        val root = criteria.from(Team::class.java)

        criteria.select(root)
            .where(cb.equal(root.get(Team_.name), name))

        return session.createQuery(criteria).resultList.awaitSuspending()
    }

    suspend fun findAll(session: Session): List<Team> {
        return findAll(session, Team::class)
    }

    suspend fun deleteById(session: Session, id: Long): Boolean {
        return deleteById(session, Team::class, id)
    }

    suspend fun findAllByMemberName(session: Session, name: String): List<Team> {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(Team::class.java)
        val root = criteria.from(Team::class.java)
        val members = root.join(Team_.members)

        criteria.select(root)
            .where(cb.equal(members.get(Member_.name), name))

        return session.createQuery(criteria).resultList.awaitSuspending().apply {
            // 팀이 여러 개일 때, 동시에 진행할 수 있도록 한다.
            asFlow()
                .flatMapMerge { team ->
                    session.fetch(team.members).awaitSuspending().asFlow()
                }
                .collect()
        }
    }
}
