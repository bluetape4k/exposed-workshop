package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.AbstractR2dbcTest
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * Spring Data R2DBC [CommentRepository]의 동작을 검증하는 테스트입니다.
 * Post ID 기반 댓글 조회, 집계, 신규 삽입 시나리오를 코루틴으로 테스트합니다.
 */
class CommentRespositoryTest(
    @param:Autowired private val commentRepository: CommentRepository,
): AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find comments by post id`() = runSuspendIO {
        val comments = commentRepository.findAllByPostId(1L).toList()
        comments.shouldNotBeEmpty()
        comments.size shouldBeGreaterOrEqualTo 2
    }

    @Test
    fun `find comments by non-existing post id`() = runSuspendIO {
        val comments = commentRepository.findAllByPostId(-1L).toList()
        comments.shouldBeEmpty()
    }

    @Test
    fun `count of comments by post id`() = runSuspendIO {
        val count = commentRepository.countByPostId(1L)
        count shouldBeGreaterOrEqualTo 2L
    }

    @Test
    fun `count of comments by non-existing post id`() = runSuspendIO {
        commentRepository.countByPostId(-1L) shouldBeEqualTo 0L
    }

    @Test
    fun `insert new comment`() = runSuspendIO {
        val oldCommentSize = commentRepository.countByPostId(2L)

        val newComment = createComment(2L)
        val savedComment = commentRepository.save(newComment)
        savedComment.shouldNotBeNull()
        savedComment.id.shouldNotBeNull()

        val newCommentSize = commentRepository.countByPostId(2L)
        newCommentSize shouldBeEqualTo oldCommentSize + 1
    }
}
