package exposed.example.springboot.autoconfig

import exposed.example.springboot.tables.TestEntity
import exposed.example.springboot.tables.TestTable
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Async
@Service
class AsyncExposedService {

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
    fun allTestDataVirtualThreads(): CompletableFuture<List<TestEntity>> = virtualFuture {
        transaction {
            val query = TestTable.selectAll()
            TestEntity.wrapRows(query).toList()
        }
    }.toCompletableFuture()
}
