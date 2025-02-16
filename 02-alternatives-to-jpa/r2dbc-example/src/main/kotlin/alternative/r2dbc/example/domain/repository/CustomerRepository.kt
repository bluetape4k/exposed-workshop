package alternative.r2dbc.example.domain.repository

import alternative.r2dbc.example.domain.model.Customer
import kotlinx.coroutines.flow.Flow
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * [CoroutineCrudRepository] 를 사용하여 [Customer] 엔티티를 저장하고 조회하는 Repository 를 정의합니다.
 *
 * spring-data 프로젝트들의 장점 중 하나인 이런 방식으로 쉽게 다양한 함수를 만들 수 있습니다.
 */
interface CustomerRepository: CoroutineCrudRepository<Customer, Long> {

    fun findByFirstname(firstname: String): Flow<Customer>

    @Query("select id, firstname, lastname from customer c where c.lastname = :lastname")
    fun findByLastname(lastname: String): Flow<Customer>
}
