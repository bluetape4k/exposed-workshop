package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.domain.model.Post
import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.r2dbc.coroutines.coCountAll
import io.bluetape4k.spring.r2dbc.coroutines.coDelete
import io.bluetape4k.spring.r2dbc.coroutines.coDeleteAll
import io.bluetape4k.spring.r2dbc.coroutines.coFindFirstById
import io.bluetape4k.spring.r2dbc.coroutines.coFindFirstByIdOrNull
import io.bluetape4k.spring.r2dbc.coroutines.coFindOneById
import io.bluetape4k.spring.r2dbc.coroutines.coFindOneByIdOrNull
import io.bluetape4k.spring.r2dbc.coroutines.coInsert
import io.bluetape4k.spring.r2dbc.coroutines.coSelectAll
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
    companion object: KLogging()

    suspend fun count(): Long = operations.coCountAll<Post>()

    fun findAll(): Flow<Post> = operations.coSelectAll()

    suspend fun findById(id: Long): Post = operations.coFindOneById(id)

    suspend fun findByIdOrNull(id: Long): Post? = operations.coFindOneByIdOrNull(id)

    suspend fun findFirstById(id: Long): Post = operations.coFindFirstById(id)

    suspend fun findFirstByIdOrNull(id: Long): Post? = operations.coFindFirstByIdOrNull(id)

    @Transactional
    suspend fun deleteAll(): Long = operations.coDeleteAll<Post>()

    @Transactional
    suspend fun deleteById(id: Long): Long {
        val query = query(Criteria.where(Post::id.name).isEqual(id))
        return operations.coDelete<Post>(query)
    }

    @Transactional
    suspend fun save(post: Post): Post = operations.coInsert(post)

    @Transactional
    suspend fun init() {
        save(Post(title = "My first post title", content = "Content of my first post"))
        save(Post(title = "My second post title", content = "Content of my second post"))
    }

}
