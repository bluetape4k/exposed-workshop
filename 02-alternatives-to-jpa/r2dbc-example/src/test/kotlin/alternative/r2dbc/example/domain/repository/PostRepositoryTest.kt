package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.AbstractR2dbcTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Spring Data R2DBC [PostRepository]의 CRUD 동작을 코루틴 환경에서 검증하는 테스트입니다.
 * 조회, 저장, 삭제 등 기본 리포지토리 메서드의 비동기 동작을 확인합니다.
 */
class PostRepositoryTest(
    @param:Autowired private val postRepository: PostRepository,
): AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find all posts`() = runSuspendIO {
        val posts = postRepository.findAll().toList()
        posts.forEach { post ->
            log.debug { "post=$post" }
        }
        posts.shouldNotBeEmpty()
    }

    @Test
    fun `find one post by id`() = runSuspendIO {
        val post = postRepository.findById(1L)
        post.id shouldBeEqualTo 1L
        log.debug { "post=$post" }
    }

    @Test
    fun `find one post by id - not exists`() = runSuspendIO {
        postRepository.findByIdOrNull(-1L).shouldBeNull()
    }

    @Test
    fun `find first by id`() = runSuspendIO {
        val post = postRepository.findFirstById(1L)
        post.id shouldBeEqualTo 1L
        log.debug { "post=$post" }
    }

    @Test
    fun `find first by id - not exists`() = runSuspendIO {
        postRepository.findFirstByIdOrNull(-1L).shouldBeNull()
    }

    @Test
    fun `insert new post`() = runSuspendIO {
        val oldCount = postRepository.count()

        val newPost = createPost()
        postRepository.save(newPost)
        postRepository.count() shouldBeEqualTo oldCount + 1
    }

    @Test
    fun `delete post by id`() = runSuspendIO {
        val oldCount = postRepository.count()

        val newPost = createPost()
        val savedPost = postRepository.save(newPost)
        postRepository.count() shouldBeEqualTo oldCount + 1

        val deletedCount = postRepository.deleteById(savedPost.id!!)
        deletedCount shouldBeEqualTo 1L
    }
}
