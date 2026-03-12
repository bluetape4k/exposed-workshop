package alternative.r2dbc.example

import alternative.r2dbc.example.domain.model.Comment
import alternative.r2dbc.example.domain.model.Post
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("h2")  // "h2" | "postgres"
@SpringBootTest(
    classes = [R2dbcApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
/**
 * R2DBC 기반 Spring Data 테스트의 공통 기반 클래스입니다.
 * H2 인메모리 데이터베이스를 기본으로 사용하며, `@ActiveProfiles("postgres")` 로 PostgreSQL 전환이 가능합니다.
 * Faker를 이용한 테스트 데이터 생성 헬퍼 메서드를 제공합니다.
 */
abstract class AbstractR2dbcTest {

    companion object: KLoggingChannel() {
        @JvmStatic
        val faker = Fakers.faker
    }

    protected fun createPost(): Post =
        Post(
            title = faker.book().title(),
            content = Fakers.fixedString(128)
        )

    protected fun createComment(postId: Long): Comment =
        Comment(
            postId = postId,
            content = Fakers.fixedString(128)
        )
}
