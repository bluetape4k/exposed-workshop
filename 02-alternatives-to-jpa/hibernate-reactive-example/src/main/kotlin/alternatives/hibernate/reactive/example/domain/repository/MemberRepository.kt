package alternatives.hibernate.reactive.example.domain.repository

import alternatives.hibernate.reactive.example.domain.model.Member
import io.bluetape4k.logging.KLogging
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.stereotype.Repository

@Repository
class MemberRepository(sf: SessionFactory): AbstractHibernateReactiveMutinyRepository<Member, Long>(sf) {

    companion object: KLogging()

    suspend fun findById(id: Long): Member? {
        return findById(Member::class, id)
    }

    suspend fun findAll(): List<Member> {
        return findAll(Member::class)
    }

    suspend fun deleteById(id: Long): Boolean {
        return deleteById(Member::class, id)
    }
}
