package alternative.r2dbc.example.utils

import alternative.r2dbc.example.domain.repository.CommentRepository
import alternative.r2dbc.example.domain.repository.PostRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import kotlinx.coroutines.runBlocking
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class DatabaseInitializer(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
): ApplicationRunner {

    companion object: KLogging()


    override fun run(args: ApplicationArguments) {
        log.info { "Initialize sample data..." }

        runBlocking {
            postRepository.init()
            commentRepository.init()
        }

        log.info { "Done initialize sample data..." }
    }

}
