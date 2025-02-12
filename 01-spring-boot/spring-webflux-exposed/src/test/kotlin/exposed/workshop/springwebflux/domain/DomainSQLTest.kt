package exposed.workshop.springwebflux.domain

import exposed.workshop.springwebflux.AbstractSpringWebfluxTest
import exposed.workshop.springwebflux.domain.MovieSchema.ActorTable
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

class DomainSQLTest: AbstractSpringWebfluxTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Nested
    open inner class Coroutines {

        @Transactional(readOnly = true)
        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() = runSuspendIO {
            newSuspendedTransaction(readOnly = true) {
                val actors = ActorTable.selectAll().map { it.toActorDTO() }
                actors.shouldNotBeEmpty()
            }
        }

        @Test
        fun `get all actors in multiple platform threads`() = runSuspendIO {
            MultijobTester()
                .numThreads(Runtime.getRuntime().availableProcessors() * 2)
                .roundsPerJob(4)
                .add {
                    newSuspendedTransaction(readOnly = true) {
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
