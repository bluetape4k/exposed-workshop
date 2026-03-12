package exposed.examples.springmvc.domain

import exposed.examples.springmvc.AbstractExposedRepositoryTest
import exposed.examples.springmvc.domain.model.MovieSchema.ActorTable
import exposed.examples.springmvc.domain.model.toActorRecord
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

/**
 * Spring MVC 환경에서 플랫폼 스레드와 가상 스레드(Java 21)를 이용한 Exposed DSL 쿼리의 동시성 동작을 테스트합니다.
 */
class DomainSQLTest: AbstractExposedRepositoryTest() {

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

    @EnabledOnJre(JRE.JAVA_21)
    @Nested
    open inner class VirtualThread {

        @RepeatedTest(REPEAT_SIZE)
        open fun `get all actors`() {
            virtualFuture {
                transaction {
                    val actors = ActorTable.selectAll().map { it.toActorRecord() }
                    actors.shouldNotBeEmpty()
                }
            }.await()
        }

        @Test
        open fun `get all actors in multiple virtual threads`() {
            StructuredTaskScopeTester()
                .rounds(Runtime.getRuntime().availableProcessors() * 2 * 4)
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
