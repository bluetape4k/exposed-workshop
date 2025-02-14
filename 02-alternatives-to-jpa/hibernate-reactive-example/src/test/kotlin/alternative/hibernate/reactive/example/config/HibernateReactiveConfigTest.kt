package alternative.hibernate.reactive.example.config

import alternative.hibernate.reactive.example.AbstractHibernateReactiveTest
import io.bluetape4k.support.uninitialized
import jakarta.persistence.EntityManagerFactory
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class HibernateReactiveConfigTest: AbstractHibernateReactiveTest() {

    @Autowired
    private val entityManagerFactory: EntityManagerFactory = uninitialized()

    @Test
    fun `context loading`() {
        entityManagerFactory.shouldNotBeNull()
    }
}
