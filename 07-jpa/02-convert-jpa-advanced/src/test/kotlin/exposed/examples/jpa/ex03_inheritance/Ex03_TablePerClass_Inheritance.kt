package exposed.examples.jpa.ex03_inheritance

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.javatime.date
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.*

/**
 * JPA의 Table Per Class Inheritance 를 Exposed 로 구현한 예
 */
class Ex03_TablePerClass_Inheritance: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * JPA의 Table Per Class Inheritance 의 경우 다중의 테이블에서 고유한 값을 사용해야 해서,
     * UUID 같은 수형으로 전역적으로 Unique 하게 관리해야 한다.
     */
    abstract class AbstractBillingTable(name: String): UUIDTable(name) {
        val owner = varchar("owner", 64).index()
        val swift = varchar("swift", 16)
    }

    /**
     * CreditCard Table
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS CREDIT_CARD (
     *      ID UUID PRIMARY KEY,
     *      OWNER VARCHAR(64) NOT NULL,
     *      SWIFT VARCHAR(16) NOT NULL,
     *      CARD_NUMBER VARCHAR(24) NOT NULL,
     *      COMPANY_NAME VARCHAR(128) NOT NULL,
     *      EXP_YEAR INT NOT NULL,
     *      EXP_MONTH INT NOT NULL,
     *      START_DATE DATE NOT NULL,
     *      END_DATE DATE NOT NULL
     * );
     *
     * CREATE INDEX CREDIT_CARD_OWNER ON CREDIT_CARD (OWNER);
     * ```
     */
    object CreditCardTable: AbstractBillingTable("credit_card") {
        val cardNumber = varchar("card_number", 24)
        val companyName = varchar("company_name", 128)
        val expYear = integer("exp_year")
        val expMonth = integer("exp_month")
        val startDate = date("start_date")
        val endDate = date("end_date")
    }

    /**
     * BankAccount Table
     *
     * ```sql
     * CREATE TABLE IF NOT EXISTS BANK_ACCOUNT (
     *      ID UUID PRIMARY KEY,
     *      OWNER VARCHAR(64) NOT NULL,
     *      SWIFT VARCHAR(16) NOT NULL,
     *      ACCOUNT_NUMBER VARCHAR(24) NOT NULL,
     *      BANK_NAME VARCHAR(128) NOT NULL
     * );
     *
     * CREATE INDEX BANK_ACCOUNT_OWNER ON BANK_ACCOUNT (OWNER);
     *```
     */
    object BankAccountTable: AbstractBillingTable("bank_account") {
        val accountNumber = varchar("account_number", 24)
        val bankName = varchar("bank_name", 128)
    }

    /**
     * CreditCard Entity
     */
    class CreditCard(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<CreditCard>(CreditCardTable)

        var owner by CreditCardTable.owner
        var swift by CreditCardTable.swift

        var cardNumber by CreditCardTable.cardNumber
        var companyName by CreditCardTable.companyName
        var expYear by CreditCardTable.expYear
        var expMonth by CreditCardTable.expMonth
        var startDate by CreditCardTable.startDate
        var endDate by CreditCardTable.endDate

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("owner", owner)
            .add("swift", swift)
            .add("card number", cardNumber)
            .add("company name", companyName)
            .toString()
    }

    /**
     * BankAccount Entity
     */
    class BankAccount(id: EntityID<UUID>): UUIDEntity(id) {
        companion object: UUIDEntityClass<BankAccount>(BankAccountTable)

        var owner by BankAccountTable.owner
        var swift by BankAccountTable.swift

        var accountNumber by BankAccountTable.accountNumber
        var bankName by BankAccountTable.bankName

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("owner", owner)
            .add("swift", swift)
            .add("account number", accountNumber)
            .add("bank name", bankName)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `table per class inheritance`(testDB: TestDB) {
        withTables(testDB, BankAccountTable, CreditCardTable) {
            val account = BankAccount.new {
                owner = "debop"
                swift = "KODBKRSE"
                accountNumber = "123-456-7890"
                bankName = "Kookmin Bank"
            }

            val card = CreditCard.new {
                owner = "debop"
                swift = "KODBKRSE"
                cardNumber = "1234-5678-9012-3456"
                companyName = "VISA"
                expMonth = 12
                expYear = 2029
                startDate = LocalDate.now()
                endDate = LocalDate.now().plusYears(5)
            }

            flushCache()
            entityCache.clear()

            /**
             * ```sql
             * -- Postgres
             * SELECT bank_account.id,
             *        bank_account."owner",
             *        bank_account.swift,
             *        bank_account.account_number,
             *        bank_account.bank_name
             *   FROM bank_account
             *  WHERE bank_account.id = '3a6596e0-032c-49bd-994e-421470a7b5b1'
             * ```
             */
            val account2 = BankAccount.findById(account.id)!!
            account2 shouldBeEqualTo account

            /**
             * ```sql
             * -- Postgres
             * SELECT credit_card.id,
             *        credit_card."owner",
             *        credit_card.swift,
             *        credit_card.card_number,
             *        credit_card.company_name,
             *        credit_card.exp_year,
             *        credit_card.exp_month,
             *        credit_card.start_date,
             *        credit_card.end_date
             *   FROM credit_card
             *  WHERE credit_card.id = '74ec7165-b420-486b-a913-ca91f074591b'
             * ```
             */
            val card2 = CreditCard.findById(card.id)!!
            card2 shouldBeEqualTo card

            // BankAccount Table을 삭제합니다.
            BankAccountTable.deleteAll()

            // SELECT COUNT(bank_account.id) FROM bank_account
            BankAccount.count() shouldBeEqualTo 0L
            // SELECT COUNT(credit_card.id) FROM credit_card
            CreditCard.count() shouldBeEqualTo 1L

            // CreditCard Table을 삭제합니다.
            CreditCardTable.deleteAll()
            CreditCard.count() shouldBeEqualTo 0L
        }
    }
}
