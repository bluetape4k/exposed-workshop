package alternatives.hibernate.reactive.example.domain.repository

import io.smallrye.mutiny.coroutines.awaitSuspending
import org.hibernate.reactive.mutiny.Mutiny.Session
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import java.io.Serializable
import kotlin.reflect.KClass

abstract class AbstractMutinySessionRepository<T: Any, ID: Serializable>(
    val sf: SessionFactory,
) {
    suspend fun findById(session: Session, clazz: KClass<T>, id: ID): T? {
        return session.find(clazz.java, id).awaitSuspending()
    }

    suspend fun findAll(session: Session, clazz: KClass<T>): List<T> {
        val cb = sf.criteriaBuilder
        val criteria = cb.createQuery(clazz.java)
        val root = criteria.from(clazz.java)

        criteria.select(root)

        return session.createQuery(criteria).resultList.awaitSuspending()
    }

    suspend fun save(session: Session, entity: T): T {
        session.persist(entity).map { session.flush() }.awaitSuspending()
        return entity
    }

    suspend fun saveAll(session: Session, vararg entities: T): List<T> {
        session.persistAll(*entities).awaitSuspending()
        session.flush().awaitSuspending()
        return entities.toList()
    }

    suspend fun deleteById(session: Session, clazz: KClass<T>, id: ID): Boolean {
        val entity = session.getReference(clazz.java, id)
        return try {
            session.remove(entity).awaitSuspending()
            session.flush().awaitSuspending()
            true
        } catch (e: Exception) {
            false
        }
    }
}
