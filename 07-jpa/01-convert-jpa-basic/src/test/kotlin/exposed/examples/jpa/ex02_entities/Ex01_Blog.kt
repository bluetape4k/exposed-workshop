package exposed.examples.jpa.ex02_entities

import exposed.examples.jpa.ex02_entities.BlogSchema.Post
import exposed.examples.jpa.ex02_entities.BlogSchema.PostDetail
import exposed.examples.jpa.ex02_entities.BlogSchema.blogTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withDb
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.flushCache
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.exists
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class Ex01_Blog: AbstractExposedTest() {

    companion object: KLogging()

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create blog entities`(testDB: TestDB) {
        withDb(testDB) {
            SchemaUtils.create(*blogTables)
            try {
                blogTables.all { it.exists() }.shouldBeTrue()
            } finally {
                SchemaUtils.drop(*blogTables)
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * INSERT INTO posts (title) VALUES ('Post 1');
     *
     * INSERT INTO post_details (id, created_on, created_by)
     * VALUES (1, '2025-02-06', 'admin');
     * ```
     *
     * ```sql
     * -- Postgres
     * SELECT posts.id, posts.title
     *   FROM posts
     *  WHERE posts.id = 1;
     *
     * -- lazy loading for PostDetail of Post
     * SELECT post_details.id,
     *        post_details.created_on,
     *        post_details.created_by
     *   FROM post_details
     *  WHERE post_details.id = 1
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `create post by DAO`(testDB: TestDB) {
        withTables(testDB, *blogTables) {

            val post = Post.new { title = "Post 1" }
            log.debug { "Post=$post" }

            // one-to-one 관계에서 ownership 을 가진 Post의 id 값을 지정합니다.
            val postDetail = PostDetail.new(post.id.value) {
                createdOn = LocalDate.now()
                createdBy = "admin"
            }
            log.debug { "PostDetail=$postDetail" }

            flushCache()
            entityCache.clear()

            val loadedPost = Post.findById(post.id)!!

            loadedPost shouldBeEqualTo post
            loadedPost.details shouldBeEqualTo postDetail
        }
    }
}
