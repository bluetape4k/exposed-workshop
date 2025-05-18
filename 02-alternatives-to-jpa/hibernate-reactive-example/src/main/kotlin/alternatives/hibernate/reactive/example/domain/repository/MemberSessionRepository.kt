package alternatives.hibernate.reactive.example.domain.repository

import alternatives.hibernate.reactive.example.domain.model.Member
import alternatives.hibernate.reactive.example.domain.model.Member_
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny.Session
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.stereotype.Repository

@Repository
class MemberSessionRepository(
    sf: SessionFactory,
): AbstractMutinySessionRepository<Member, Long>(sf) {

    companion object: KLoggingChannel()

    suspend fun findById(session: Session, id: Long): Member? {
        return findById(session, Member::class, id)
    }

    suspend fun findAll(session: Session): List<Member> {
        return findAll(session, Member::class)
    }

    suspend fun findAllByName(session: Session, name: String): List<Member> {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(Member::class.java)
        val root = criteria.from(Member::class.java)

        criteria.select(root)
            .where(cb.equal(root.get(Member_.name), name))

        return session.createQuery(criteria).resultList.awaitSuspending()
    }


    suspend fun deleteById(session: Session, id: Long): Boolean {
        return deleteById(session, Member::class, id)
    }
}
