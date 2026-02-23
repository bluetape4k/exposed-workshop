package exposed.example.springboot.autoconfig

import exposed.example.springboot.tables.TestEntity
import exposed.example.springboot.tables.TestTable
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Async
@Service
/**
 * Exposed 조회를 비동기 API(플랫폼 스레드/Virtual Thread)로 감싸 제공하는 테스트용 서비스입니다.
 */
class AsyncExposedService {

    companion object: KLoggingChannel()

    /**
     * 비동기 방식으로 [TestTable]의 모든 데이터를 조회합니다.
     */
    fun allTestDataAsync(): CompletableFuture<List<TestEntity>> = CompletableFuture.supplyAsync {
        transaction {
            val query = TestTable.selectAll()
            TestEntity.wrapRows(query).toList()
        }
    }

    /**
     * Virtual Threads 를 이용하여 비동기 방식으로 [TestTable]의 모든 데이터를 조회합니다.
     */
    fun allTestDataVirtualThreads() = virtualFuture {
        transaction {
            val query = TestTable.selectAll()
            TestEntity.wrapRows(query).toList()
        }
    }.toCompletableFuture()
}
