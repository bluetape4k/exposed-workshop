package exposed.workshop.springmvc.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.TaskDecorator
import org.springframework.core.task.support.TaskExecutorAdapter
import org.springframework.scheduling.annotation.EnableAsync
import java.util.concurrent.Executors

/**
 * `@Async` 어노테이션이 적용된 메소드를 Virtual Threaed를 이용하여 비동기로 실행하기 위한 설정
 *
 * @see [org.springframework.scheduling.annotation.Async]
 */
@Configuration
@EnableAsync
@ConditionalOnProperty("app.virtualthread.enabled", havingValue = "true")
class AsyncVirtualThreadConfig {

    companion object: KLogging()

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    fun asyncTaskExecutor(): AsyncTaskExecutor {
        log.info { "Async Tasks 들을 Virtual Threads에서 수행되도록 설정합니다..." }

        val factory = Thread.ofVirtual().name("async-vt-exec-", 0).factory()
        return TaskExecutorAdapter(Executors.newThreadPerTaskExecutor(factory)).apply {
            setTaskDecorator(LoggingTaskDecorator())
        }
    }

    class LoggingTaskDecorator: TaskDecorator {
        override fun decorate(task: Runnable): Runnable {
            val callerThreadContext = MDC.getCopyOfContextMap()
            return Runnable {
                callerThreadContext?.let { MDC.setContextMap(it) }
                task.run()
            }
        }
    }
}
