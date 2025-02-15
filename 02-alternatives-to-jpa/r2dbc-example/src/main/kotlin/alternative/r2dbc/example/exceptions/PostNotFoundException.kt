package alternative.r2dbc.example.exceptions

import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.R2dbcException

open class PostNotFoundException: R2dbcException {

    companion object: KLogging() {
        private fun getMessage(postId: Long): String = "Post[$postId] is not found."
    }

    val postId: Long

    constructor(postId: Long): super(getMessage(postId)) {
        this.postId = postId
    }

    constructor(postId: Long, cause: Throwable?): super(getMessage(postId), cause) {
        this.postId = postId
    }
}
