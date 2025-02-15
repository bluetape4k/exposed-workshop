package alternative.r2dbc.example.controller

import alternative.r2dbc.example.AbstractR2dbcTest
import alternative.r2dbc.example.domain.model.Post
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList

class PostControllerTest(
    @Autowired private val client: WebTestClient,
): AbstractR2dbcTest() {

    companion object: KLogging()

    @Test
    fun `find all posts`() {
        val posts = client.get().uri("/posts")
            .exchange()
            .expectStatus().isOk
            .expectBodyList<Post>()
            .returnResult().responseBody!!

        posts.shouldNotBeEmpty()
        posts.forEach { post ->
            log.debug { "post=$post" }
        }
    }

    @Test
    fun `find one post by id`() {
        val post = client.get().uri("/posts/1")
            .exchange()
            .expectStatus().isOk
            .expectBody<Post>()
            .returnResult().responseBody!!

        log.debug { "Post[1]=$post" }
        post.id shouldBeEqualTo 1L
    }

    @Test
    fun `find one post by non-existing id`() {
        client.get().uri("/posts/9999")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `save new post`() {
        val newPost = createPost()

        val savedPost = client.post().uri("/posts")
            .bodyValue(newPost)
            .exchange()
            .expectStatus().is2xxSuccessful
            .expectBody<Post>()
            .returnResult().responseBody!!

        savedPost.id.shouldNotBeNull()
        savedPost shouldBeEqualTo newPost.copy(id = savedPost.id)
    }

    @Test
    fun `count of comments by post id`() {
        val commentCount1 = countOfCommentByPostId(1L)
        val commentCount2 = countOfCommentByPostId(2L)

        commentCount1 shouldBeGreaterThan 0
        commentCount2 shouldBeGreaterThan 0
    }

    @Test
    fun `count of comments by non-existing post id`() {
        countOfCommentByPostId(9999L) shouldBeEqualTo 0L
    }

    private fun countOfCommentByPostId(postId: Long): Long {
        return client.get().uri("/posts/$postId/comments/count")
            .exchange()
            .expectStatus().isOk
            .expectBody<Long>()
            .returnResult().responseBody!!
    }
}
