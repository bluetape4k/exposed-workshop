package exposed.workshop.springmvc.domain

import exposed.workshop.springmvc.AbstractSpringMvcTest
import exposed.workshop.springmvc.domain.model.MovieSchema.ActorTable
import exposed.workshop.springmvc.domain.model.toActorRecord
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import org.springframework.transaction.annotation.Transactional

class DomainSQLTest: AbstractSpringMvcTest() {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @Nested
    open inner class PlatformThread {

        @Transactional(readOnly = true)
        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() {
            val actors = ActorTable.selectAll().map { it.toActorRecord() }
            actors.shouldNotBeEmpty()
        }

        @Test
        fun `get all actors in multiple platform threads`() {
            MultithreadingTester()
                .workers(Runtime.getRuntime().availableProcessors() * 2)
                .rounds(4)
                .add {
                    transaction {
                        val actors = ActorTable.selectAll().map { it.toActorRecord() }
                        actors.shouldNotBeEmpty()
                    }
                }
                .run()
        }
    }

    @EnabledOnJre(JRE.JAVA_21, JRE.JAVA_25)
    @Nested
    open inner class VirtualThread {

        /**
         * Retrieves and validates actors within virtual thread
         */
        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() {
            virtualFuture {
                transaction {
                    val actors = ActorTable.selectAll().map { it.toActorRecord() }
                    actors.shouldNotBeEmpty()
                }
            }.await()
        }

        /**
         * Tests actor retrieval across multiple virtual threads
         */
        @Test
        open fun `get all actors in multiple virtual threads`() {
            StructuredTaskScopeTester()
                .rounds(Runtime.getRuntime().availableProcessors() * 8)
                .add {
                    transaction {
                        val actors = ActorTable.selectAll().map { it.toActorRecord() }
                        actors.shouldNotBeEmpty()
                    }
                }
                .run()
        }
    }
}
