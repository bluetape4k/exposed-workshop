package alternatives.hibernate.reactive.example.config

import io.bluetape4k.hibernate.reactive.mutiny.asMutinySessionFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.testcontainers.database.PostgreSQLServer
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HibernateReactiveConfig {

    companion object: KLogging()

    @Bean
    fun entityManagerFactory(): EntityManagerFactory {
        val props = mutableMapOf<String, Any?>()
        props["jakarta.persistence.jdbc.url"] = PostgreSQLServer.Launcher.postgres.getJdbcUrl()
        props["jakarta.persistence.jdbc.user"] = PostgreSQLServer.Launcher.postgres.getUsername()
        props["jakarta.persistence.jdbc.password"] = PostgreSQLServer.Launcher.postgres.getPassword()
        props["jakarta.persistence.jdbc.driver_class"] = PostgreSQLServer.Launcher.postgres.getDriverClassName()

        log.info { "Create EntityManagerFactory. props=$props" }

        return Persistence.createEntityManagerFactory("default", props)
    }

    @Bean
    fun sessionFactory(emf: EntityManagerFactory): SessionFactory {
        return emf.asMutinySessionFactory()
    }
}
