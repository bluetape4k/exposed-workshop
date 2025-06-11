package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.domain.model.Comment
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.r2dbc.coroutines.suspendCount
import io.bluetape4k.spring.r2dbc.coroutines.suspendCountAll
import io.bluetape4k.spring.r2dbc.coroutines.suspendInsert
import io.bluetape4k.spring.r2dbc.coroutines.suspendSelect
import io.bluetape4k.spring.r2dbc.coroutines.suspendSelectAll
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.isEqual
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class CommentRepository(
    private val client: DatabaseClient,
    private val operations: R2dbcEntityOperations,
) {

    companion object: KLoggingChannel()

    suspend fun count(): Long = operations.suspendCountAll<Comment>()

    fun findAll(): Flow<Comment> = operations.suspendSelectAll<Comment>()

    @Transactional
    suspend fun save(comment: Comment): Comment {
        return operations.suspendInsert(comment)
    }

    suspend fun countByPostId(postId: Long): Long {
        val query = Query.query(Criteria.where(Comment::postId.name).isEqual(postId))
        return operations.suspendCount<Comment>(query)
    }

    fun findAllByPostId(postId: Long): Flow<Comment> {
        val query = Query.query(Criteria.where(Comment::postId.name).isEqual(postId))
        return operations.suspendSelect(query)
    }

    @Transactional
    suspend fun init() {
        save(Comment(postId = 1, content = "Content 1 of post 1"))
        save(Comment(postId = 1, content = "Content 2 of post 1"))
        save(Comment(postId = 2, content = "Content 1 of post 2"))
        save(Comment(postId = 2, content = "Content 2 of post 1"))
    }
}
