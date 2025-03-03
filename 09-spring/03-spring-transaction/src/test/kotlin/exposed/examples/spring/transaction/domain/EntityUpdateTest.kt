package exposed.examples.spring.transaction.domain

import exposed.examples.spring.transaction.AbstractSpringTransactionTest
import exposed.examples.spring.transaction.utils.execute
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insertAndGetId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.utility.Base58
import kotlin.test.fail

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class EntityUpdateTest: AbstractSpringTransactionTest() {

    object T1: IntIdTable() {
        val c1 = varchar("c1", Int.MIN_VALUE.toString().length)
        val c2 = varchar("c2", Int.MIN_VALUE.toString().length).nullable()
    }

    class E1(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<E1>(T1)

        var c1 by T1.c1
        var c2 by T1.c2

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder().add("c1", c1).toString()
    }

    @BeforeAll
    fun beforeAll() {
        transactionManager.execute {
            SchemaUtils.create(T1)
        }
    }

    @AfterAll
    fun afterAll() {
        transactionManager.execute {
            SchemaUtils.drop(T1)
        }
    }

    @Test
    @Transactional
    @Commit
    @Order(0)
    fun `새로운 엔티티를 추가한다`() {
        val c1 = Base58.randomString(8)

        val id = T1.insertAndGetId {
            it[T1.c1] = c1
        }

        E1.findById(id)?.c1 shouldBeEqualTo c1
    }

    @Test
    @Transactional
    @Commit
    @Order(1)
    fun `기존 엔티티를 수정한다`() {
        val entity = E1.findById(1) ?: fail("Entity not found")

//        T1.update({ T1.id eq entity.id }) {
//            it[c1] = "updated"
//            it[c2] = "new"
//        }
        entity.c1 = "updated"
        entity.c2 = "new"
    }

    @Test
    @Transactional
    @Commit
    @Order(2)
    fun `수정된 엔티티를 조회한다`() {
        val entity = E1.findById(1) ?: fail("Entity not found")
        entity.c1 shouldBeEqualTo "updated"
        entity.c2 shouldBeEqualTo "new"
    }
}
