package exposed.example.springboot.autoconfig

import exposed.example.springboot.tables.TestTable
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import org.jetbrains.exposed.sql.ResultRow
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
     *
     * 여기서는 Java 21의 Virtual Thread를 사용하여 비동기 처리를 구현합니다.
     */
    fun allTestDataAsync(): CompletableFuture<List<ResultRow>> = virtualFuture {
        transaction {
            TestTable.selectAll().toList()
        }
    }.toCompletableFuture()

}
