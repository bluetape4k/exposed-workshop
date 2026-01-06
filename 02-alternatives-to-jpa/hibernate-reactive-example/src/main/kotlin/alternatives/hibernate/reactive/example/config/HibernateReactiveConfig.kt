package alternatives.hibernate.reactive.example.config

import io.bluetape4k.hibernate.reactive.mutiny.asMutinySessionFactory
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HibernateReactiveConfig {

    companion object: KLoggingChannel()

    @Bean
    fun entityManagerFactory(): EntityManagerFactory {
        val props = mutableMapOf<String, Any?>()
        props["jakarta.persistence.jdbc.url"] = PostgreSQLServer.Launcher.postgres.getJdbcUrl()
        props["jakarta.persistence.jdbc.user"] = PostgreSQLServer.Launcher.postgres.getUsername()
        props["jakarta.persistence.jdbc.password"] = PostgreSQLServer.Launcher.postgres.getPassword()
        props["jakarta.persistence.jdbc.driver_class"] = PostgreSQLServer.Launcher.postgres.getDriverClassName()

        // 스캐닝 시 발생할 수 있는 파일 시스템 문제를 방지하기 위해 추가적인 힌트를 제공합니다.
        // 엔티티가 포함된 패키지를 명시적으로 지정하거나 스캔 범위를 조정합니다.
        props["hibernate.archive.autodetection"] = "class"

        log.info { "Create EntityManagerFactory. props=$props" }

        return Persistence.createEntityManagerFactory("default", props)
    }

    @Bean
    fun sessionFactory(emf: EntityManagerFactory): SessionFactory {
        return emf.asMutinySessionFactory()
    }
}
