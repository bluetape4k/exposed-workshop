package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.domain.model.Post
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.countAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.deleteAllSuspending
import io.bluetape4k.spring.r2dbc.coroutines.deleteSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findFirstByIdOrNullSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findFirstByIdSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdOrNullSuspending
import io.bluetape4k.spring.r2dbc.coroutines.findOneByIdSuspending
import io.bluetape4k.spring.r2dbc.coroutines.insertSuspending
import io.bluetape4k.spring.r2dbc.coroutines.selectAllSuspending
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.isEqual
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class PostRepository(
    private val client: DatabaseClient,
    private val operations: R2dbcEntityOperations,
    private val mappingR2dbcConverter: MappingR2dbcConverter,
) {
    companion object: KLoggingChannel()

    suspend fun count(): Long = operations.countAllSuspending<Post>()

    fun findAll(): Flow<Post> = operations.selectAllSuspending<Post>()

    suspend fun findById(id: Long): Post = operations.findOneByIdSuspending(id)

    suspend fun findByIdOrNull(id: Long): Post? = operations.findOneByIdOrNullSuspending(id)

    suspend fun findFirstById(id: Long): Post = operations.findFirstByIdSuspending(id)

    suspend fun findFirstByIdOrNull(id: Long): Post? = operations.findFirstByIdOrNullSuspending(id)

    @Transactional
    suspend fun deleteAll(): Long = operations.deleteAllSuspending<Post>()

    @Transactional
    suspend fun deleteById(id: Long): Long {
        val query = query(Criteria.where(Post::id.name).isEqual(id))
        return operations.deleteSuspending<Post>(query)
    }

    @Transactional
    suspend fun save(post: Post): Post = operations.insertSuspending(post)

    @Transactional
    suspend fun init() {
        save(Post(title = "My first post title", content = "Content of my first post"))
        save(Post(title = "My second post title", content = "Content of my second post"))
    }
}
