package alternative.r2dbc.example.controller

import alternative.r2dbc.example.AbstractR2dbcTest
import alternative.r2dbc.example.domain.model.Post
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.tests.httpGet
import io.bluetape4k.spring.tests.httpPost
import kotlinx.coroutines.reactive.awaitSingle
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import org.springframework.test.web.reactive.server.returnResult

class PostControllerTest(
    @param:Autowired private val client: WebTestClient,
): AbstractR2dbcTest() {

    companion object: KLoggingChannel()

    @Test
    fun `find all posts`() = runSuspendIO {
        val posts = client
            .httpGet("/posts")
            .expectStatus().is2xxSuccessful
            .expectBodyList<Post>()
            .returnResult().responseBody
            .shouldNotBeNull()

        posts.shouldNotBeEmpty()
        posts.forEach { post ->
            log.debug { "post=$post" }
        }
    }

    @Test
    fun `find one post by id`() = runSuspendIO {
        val post = client
            .httpGet("/posts/1")
            .expectStatus().is2xxSuccessful
            .returnResult<Post>().responseBody
            .awaitSingle()

        log.debug { "Post[1]=$post" }
        post.id shouldBeEqualTo 1L
    }

    @Test
    fun `find one post by non-existing id`() {
        client
            .httpGet("/posts/9999")
            .expectStatus().isNotFound
    }

    @Test
    fun `save new post`() = runSuspendIO {
        val newPost = createPost()

        val savedPost = client
            .httpPost("/posts", newPost)
            .expectStatus().is2xxSuccessful
            .returnResult<Post>().responseBody
            .awaitSingle()

        savedPost.id.shouldNotBeNull()
        savedPost shouldBeEqualTo newPost.copy(id = savedPost.id)
    }

    @Test
    fun `count of comments by post id`() = runSuspendIO {
        val commentCount1 = countOfCommentByPostId(1L)
        val commentCount2 = countOfCommentByPostId(2L)

        commentCount1 shouldBeGreaterThan 0
        commentCount2 shouldBeGreaterThan 0
    }

    @Test
    fun `count of comments by non-existing post id`() = runSuspendIO {
        countOfCommentByPostId(9999L) shouldBeEqualTo 0L
    }

    private suspend fun countOfCommentByPostId(postId: Long): Long {
        return client
            .httpGet("/posts/$postId/comments/count")
            .expectStatus().is2xxSuccessful
            .returnResult<Long>().responseBody
            .awaitSingle()
    }
}
