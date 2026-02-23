package exposed.examples.spring.transaction.domain

import exposed.examples.spring.transaction.AbstractSpringTransactionTest
import exposed.examples.spring.transaction.service.OrderService
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.Commit
import org.springframework.transaction.TransactionSystemException
import kotlin.test.assertFailsWith

class SpringTransactionEntityTest(
    @param:Autowired private val orderService: OrderService,
): AbstractSpringTransactionTest() {

    companion object: KLogging()

    @BeforeEach
    fun beforeEach() {
        orderService.init()
    }

    @AfterEach
    fun afterEach() {
        orderService.cleanUp()
    }

    @Test
    @Commit
    fun `customer와 order 생성`() {
        val customer = orderService.createCustomer("Alice1")

        orderService.createOrder(customer, "Product1")

        val order = orderService.findOrderByProductName("Product1")
        order.shouldNotBeNull()

        orderService.transaction {
            // lazy loading
            log.debug { "order's customer=${order.customer}" }
            order.customer.name shouldBeEqualTo "Alice1"
        }
    }

    @Test
    @Commit
    fun `customer와 order를 동시에 생성`() {
        orderService.doBoth("Bob", "Product2")

        val order = orderService.findOrderByProductName("Product2")
        order.shouldNotBeNull()

        orderService.transaction {
            // lazy loading
            log.debug { "order's customer=${order.customer}" }
            order.customer.name shouldBeEqualTo "Bob"
        }
    }

    @Test
    @Commit
    fun `존재하지 않는 상품명으로 주문 조회 시 null을 반환한다`() {
        orderService.findOrderByProductName("NO_SUCH_PRODUCT").shouldBeNull()
    }

    @Test
    @Commit
    fun `중복된 고객 이름으로 생성 시 예외가 발생한다`() {
        orderService.createCustomer("DuplicatedCustomer")
        assertFailsWith<TransactionSystemException> {
            orderService.createCustomer("DuplicatedCustomer")
        }
    }
}
