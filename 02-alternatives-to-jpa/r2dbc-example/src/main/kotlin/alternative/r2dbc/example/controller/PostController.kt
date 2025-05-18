package alternative.r2dbc.example.controller

import alternative.r2dbc.example.domain.model.Comment
import alternative.r2dbc.example.domain.model.Post
import alternative.r2dbc.example.domain.repository.CommentRepository
import alternative.r2dbc.example.domain.repository.PostRepository
import alternative.r2dbc.example.exceptions.PostNotFoundException
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/posts")
class PostController(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
): CoroutineScope by CoroutineScope(Dispatchers.IO + SupervisorJob()) {

    companion object: KLoggingChannel()

    @GetMapping
    fun findAll(): Flow<Post> = postRepository.findAll()

    @GetMapping("/{id}")
    suspend fun findById(@PathVariable id: Long): Post? =
        postRepository.findByIdOrNull(id) ?: throw PostNotFoundException(id)

    @PostMapping
    suspend fun save(@RequestBody post: Post): Post {
        return postRepository.save(post)
    }

    @GetMapping("/{postId}/comments")
    fun findCommentsByPostId(@PathVariable postId: Long): Flow<Comment> =
        commentRepository.findAllByPostId(postId)

    @GetMapping("/{postId}/comments/count")
    suspend fun countCommentsByPostId(@PathVariable postId: Long): Long =
        commentRepository.countByPostId(postId)

    @PostMapping("/{postId}/comments")
    suspend fun saveComment(@PathVariable postId: Long, @RequestBody comment: Comment): Comment {
        return commentRepository.save(comment.copy(postId = postId, content = comment.content))
    }
}
