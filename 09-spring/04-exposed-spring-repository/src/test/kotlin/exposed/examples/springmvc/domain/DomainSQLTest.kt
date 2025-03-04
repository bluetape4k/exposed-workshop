package exposed.examples.springmvc.domain

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.dtos.toActorDTO
import exposed.examples.springmvc.domain.model.MovieSchema.ActorTable
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

class DomainSQLTest: AbstractExposedRepositoryTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Nested
    open inner class PlatformThread {

        @Transactional(readOnly = true)
        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() {
            val actors = ActorTable.selectAll().map { it.toActorDTO() }
            actors.shouldNotBeEmpty()
        }

        @Test
        fun `get all actors in multiple platform threads`() {
            MultithreadingTester()
                .numThreads(Runtime.getRuntime().availableProcessors() * 2)
                .roundsPerThread(4)
                .add {
                    transaction {
                        val actors = ActorTable.selectAll().map { it.toActorDTO() }
                        actors.shouldNotBeEmpty()
                    }
                }
                .run()
        }
    }

    @Nested
    open inner class VirtualThread {

        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() {
            virtualFuture {
                transaction {
                    val actors = ActorTable.selectAll().map { it.toActorDTO() }
                    actors.shouldNotBeEmpty()
                }
            }.await()
        }

        @Test
        open fun `get all actors in multiple virtual threads`() {
            VirtualthreadTester()
                .numThreads(Runtime.getRuntime().availableProcessors() * 2)
                .roundsPerThread(4)
                .add {
                    transaction {
                        val actors = ActorTable.selectAll().map { it.toActorDTO() }
                        actors.shouldNotBeEmpty()
                    }
                }
                .run()
        }
    }
}
