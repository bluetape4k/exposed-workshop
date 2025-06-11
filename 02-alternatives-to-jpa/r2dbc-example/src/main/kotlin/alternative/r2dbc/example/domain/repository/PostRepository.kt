package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.domain.model.Post
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.suspendCountAll
import io.bluetape4k.spring.r2dbc.coroutines.suspendDelete
import io.bluetape4k.spring.r2dbc.coroutines.suspendDeleteAll
import io.bluetape4k.spring.r2dbc.coroutines.suspendFindFirstById
import io.bluetape4k.spring.r2dbc.coroutines.suspendFindFirstByIdOrNull
import io.bluetape4k.spring.r2dbc.coroutines.suspendFindOneById
import io.bluetape4k.spring.r2dbc.coroutines.suspendFindOneByIdOrNull
import io.bluetape4k.spring.r2dbc.coroutines.suspendInsert
import io.bluetape4k.spring.r2dbc.coroutines.suspendSelectAll
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

    suspend fun count(): Long = operations.suspendCountAll<Post>()

    fun findAll(): Flow<Post> = operations.suspendSelectAll()

    suspend fun findById(id: Long): Post = operations.suspendFindOneById(id)

    suspend fun findByIdOrNull(id: Long): Post? = operations.suspendFindOneByIdOrNull(id)

    suspend fun findFirstById(id: Long): Post = operations.suspendFindFirstById(id)

    suspend fun findFirstByIdOrNull(id: Long): Post? = operations.suspendFindFirstByIdOrNull(id)

    @Transactional
    suspend fun deleteAll(): Long = operations.suspendDeleteAll<Post>()

    @Transactional
    suspend fun deleteById(id: Long): Long {
        val query = query(Criteria.where(Post::id.name).isEqual(id))
        return operations.suspendDelete<Post>(query)
    }

    @Transactional
    suspend fun save(post: Post): Post = operations.suspendInsert(post)

    @Transactional
    suspend fun init() {
        save(Post(title = "My first post title", content = "Content of my first post"))
        save(Post(title = "My second post title", content = "Content of my second post"))
    }
}
