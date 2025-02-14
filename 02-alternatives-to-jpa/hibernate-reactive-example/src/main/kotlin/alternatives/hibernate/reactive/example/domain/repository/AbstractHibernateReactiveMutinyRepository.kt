package alternatives.hibernate.reactive.example.domain.repository

import io.bluetape4k.hibernate.reactive.mutiny.withSessionSuspending
import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.io.Serializable
import kotlin.reflect.KClass

abstract class AbstractHibernateReactiveMutinyRepository<T: Any, ID: Serializable>(
    val sf: SessionFactory,
) {
    suspend fun findById(clazz: KClass<T>, id: ID): T? {
        return sf.withSessionSuspending { session ->
            session.find(clazz.java, id).awaitSuspending()
        }
    }

    suspend fun findAll(clazz: KClass<T>): List<T> {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(clazz.java)
        // val root = criteria.from(clazz.java)

        return sf.withSessionSuspending { session ->
            session.createQuery(criteria).resultList.awaitSuspending()
        }
    }

    suspend fun save(entity: T): T {
        return sf.withSessionSuspending { session ->
            session.persist(entity).awaitSuspending()
            entity
        }
    }

    suspend fun deleteById(clazz: KClass<T>, id: ID): Boolean {
        return sf.withSessionSuspending { session ->
            val entity = session.find(clazz.java, id).awaitSuspending()
            if (entity != null) {
                session.remove(entity).awaitSuspending()
                true
            } else {
                false
            }
        }
    }
}
