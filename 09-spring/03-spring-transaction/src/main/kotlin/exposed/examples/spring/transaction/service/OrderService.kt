package exposed.examples.spring.transaction.service

import exposed.examples.spring.transaction.domain.OrderSchema.CustomerEntity
import exposed.examples.spring.transaction.domain.OrderSchema.CustomerTable
import exposed.examples.spring.transaction.domain.OrderSchema.OrderEntity
import exposed.examples.spring.transaction.domain.OrderSchema.OrderTable
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
/**
 * 고객/주문 엔티티 생성과 조회를 트랜잭션 경계 안에서 수행하는 서비스입니다.
 */
class OrderService {

    companion object: KLogging()

    /**
     * 고객/주문 스키마를 생성합니다.
     */
    fun init() {
        log.info { "스키마 생성. ${CustomerTable.tableName}, ${OrderTable.tableName}" }
        SchemaUtils.create(CustomerTable, OrderTable)
    }

    /**
     * 고객 엔티티를 생성합니다.
     */
    fun createCustomer(name: String, mobile: String? = null): CustomerEntity {
        log.debug { "고객 생성. name=$name" }
        return CustomerEntity.new {
            this.name = name
            this.mobile = mobile
        }
    }

    /**
     * ```sql
     * INSERT INTO ORDERS (ID, CUSTOMER, PRODUCT_NAME)
     * VALUES ('fb401281-66ab-4171-8e88-06c347ba3863', '86c69c45-66e2-4727-a324-d3b7e46623db', 'Product1')
     * ```
     */
    fun createOrder(customer: CustomerEntity, productName: String): OrderEntity {
        log.debug { "주문 생성. customer=${customer.name}, productName=$productName" }

        return OrderEntity.new {
            this.customer = customer
            this.productName = productName
        }
    }

    /**
     * 고객 생성 후 즉시 주문을 생성합니다.
     */
    fun doBoth(customerName: String, productName: String, customerMobile: String? = null): OrderEntity {
        val customer = createCustomer(customerName, customerMobile)
        return createOrder(customer, productName)
    }

    /**
     * ```sql
     * SELECT ORDERS.ID, ORDERS.CUSTOMER, ORDERS.PRODUCT_NAME
     *   FROM ORDERS
     *  WHERE ORDERS.PRODUCT_NAME = 'Product1'
     * ```
     */
    fun findOrderByProductName(productName: String): OrderEntity? {
        return OrderEntity.find { OrderTable.productName eq productName }.firstOrNull()
    }

    /**
     * 현재 트랜잭션 컨텍스트에서 블록을 실행합니다.
     */
    fun transaction(block: () -> Unit) {
        block()
    }

    /**
     * 고객/주문 스키마를 제거합니다.
     */
    fun cleanUp() {
        log.info { "스키마 삭제. ${CustomerTable.tableName}, ${OrderTable.tableName}" }
        SchemaUtils.drop(CustomerTable, OrderTable)
    }
}
