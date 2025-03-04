package exposed.examples.springwebflux.domain

import exposed.examples.springwebflux.AbstractCoroutineExposedRepositoryTest
import exposed.examples.springwebflux.domain.model.MovieSchema.ActorTable
import exposed.examples.springwebflux.domain.model.toActorDTO
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test

class DomainSQLTest: AbstractCoroutineExposedRepositoryTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Nested
    open inner class Coroutines {

        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() = runSuspendIO {
            newSuspendedTransaction(readOnly = true) {
                val actors = ActorTable.selectAll().map { it.toActorDTO() }
                actors.shouldNotBeEmpty()
            }
        }

        @Test
        fun `get all actors in multiple platform threads`() = runSuspendIO {
            newSuspendedTransaction(readOnly = true) {
                MultijobTester()
                    .numThreads(Runtime.getRuntime().availableProcessors() * 2)
                    .roundsPerJob(4)
                    .add {
                        suspendedTransactionAsync {
                            val actors = ActorTable.selectAll().map { it.toActorDTO() }
                            actors.shouldNotBeEmpty()
                        }.await()
                    }
                    .run()
            }
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
