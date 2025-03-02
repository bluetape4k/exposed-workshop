package exposed.examples.jpa.ex03_inheritance

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.date
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 한 테이블에서 여러가지 종류의 엔티티를 표현하는 방식
 */
class Ex01_SingleTable_Inheritance: AbstractExposedTest() {

    /**
     * 한 테이블에 2가지 종류의 컬럼 정보를 저장하는 테이블
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS billing (
     *      -- 공통 속성
     *      id SERIAL PRIMARY KEY,
     *      "owner" VARCHAR(255) NOT NULL,
     *      swift VARCHAR(255) NOT NULL,
     *      dtype VARCHAR(31) DEFAULT 'UNKNOWN' NOT NULL,
     *
     *      -- CreditCard 속성
     *      card_number VARCHAR(32) NULL,
     *      company_name VARCHAR(255) NULL,
     *      exp_month INT NULL,
     *      exp_year INT NULL,
     *      start_date DATE NULL,
     *      end_date DATE NULL,
     *
     *      -- BankAccount 속성
     *      account_number VARCHAR(255) NULL,
     *      bank_name VARCHAR(255) NULL
     * );
     *
     * CREATE INDEX billing_owner ON billing ("owner");
     * ```
     */
    object BillingTable: IntIdTable("billing") {

        val owner = varchar("owner", 255).index()
        val swift = varchar("swift", 255)

        /**
         * 지불 방식 ([CreditCard] or [BankAccount])을 구분하는 컬럼
         */
        val dtype = enumerationByName<BillingType>("dtype", 32).default(BillingType.UNKNOWN)

        // CreditCard (공통 속성을 제외하면 모든 속성은 nullable 이어야 합니다)
        val cardNumber = varchar("card_number", 32).nullable()
        val companyName = varchar("company_name", 255).nullable()
        val expMonth = integer("exp_month").nullable()
        val expYear = integer("exp_year").nullable()
        val startDate = date("start_date").nullable()
        val endDate = date("end_date").nullable()

        // BankAccount (공통 속성을 제외하면 모든 속성은 nullable 이어야 합니다)
        val accountNumber = varchar("account_number", 255).nullable()
        val bankName = varchar("bank_name", 255).nullable()
    }

    /**
     * 지불 방식으로 [BillingTable]의 엔티티 종류를 구분하도록 하는 enum
     */
    enum class BillingType {
        UNKNOWN,
        CREDIT_CARD,
        BANK_ACCOUNT
    }

    /**
     * Billing 정보의 공통 속성을 가진 추상 클래스
     */
    abstract class Billing(id: EntityID<Int>): IntEntity(id) {
        var owner by BillingTable.owner
        var swift by BillingTable.swift
        var dtype: BillingType by BillingTable.dtype

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("owner", owner)
            .add("swift", swift)
            .add("dtype", dtype)
            .toString()
    }

    /**
     * Billing 정보 중 CreditCard 정보를 가진 엔티티
     */
    class CreditCard(id: EntityID<Int>): Billing(id) {
        companion object: IntEntityClass<CreditCard>(BillingTable) {
            override fun new(id: Int?, init: CreditCard.() -> Unit): CreditCard {
                return super.new(id, init).apply {
                    dtype = BillingType.CREDIT_CARD
                }
            }

            /**
             * CreditCard 엔티티에 해당하는 정보 중 해당 id 값을 가지는 엔티티를 조회합니다.
             */
            override fun findById(id: EntityID<Int>): CreditCard? {
                val query = BillingTable
                    .select(
                        BillingTable.id,
                        BillingTable.owner,
                        BillingTable.swift,
                        BillingTable.dtype,
                        BillingTable.cardNumber,
                        BillingTable.companyName,
                        BillingTable.expMonth,
                        BillingTable.expYear,
                        BillingTable.startDate,
                        BillingTable.endDate,
                    )
                    .where { BillingTable.id eq id }
                    .andWhere { BillingTable.dtype eq BillingType.CREDIT_CARD }

                return CreditCard.wrapRows(query).singleOrNull()
            }

            fun countCreditCard(): Long = super.count(BillingTable.dtype eq BillingType.CREDIT_CARD)
        }

        var cardNumber by BillingTable.cardNumber
        var companyName by BillingTable.companyName
        var expMonth by BillingTable.expMonth
        var expYear by BillingTable.expYear
        var startDate by BillingTable.startDate
        var endDate by BillingTable.endDate

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("owner", owner)
            .add("swift", swift)
            .add("dtype", dtype)
            .add("cardNumber", cardNumber)
            .add("companyName", companyName)
            .add("expMonth", expMonth)
            .add("expYear", expYear)
            .add("startDate", startDate)
            .add("endDate", endDate)
            .toString()
    }

    /**
     * Billing 정보 중 BankAccount 정보를 가진 엔티티
     */
    class BankAccount(id: EntityID<Int>): Billing(id) {
        companion object: IntEntityClass<BankAccount>(BillingTable) {
            override fun new(id: Int?, init: BankAccount.() -> Unit): BankAccount {
                return super.new(id, init).apply {
                    dtype = BillingType.BANK_ACCOUNT
                }
            }

            /**
             * BankAccount 엔티티에 해당하는 정보 중 해당 id 값을 가지는 엔티티를 조회합니다.
             */
            override fun findById(id: EntityID<Int>): BankAccount? {
                val query = BillingTable
                    .select(
                        BillingTable.id,
                        BillingTable.owner,
                        BillingTable.swift,
                        BillingTable.dtype,
                        BillingTable.accountNumber,
                        BillingTable.bankName
                    )
                    .where { BillingTable.id eq id }
                    .andWhere { BillingTable.dtype eq BillingType.BANK_ACCOUNT }

                return BankAccount.wrapRows(query).singleOrNull()
            }


            fun countBankAccount(): Long = super.count(BillingTable.dtype eq BillingType.BANK_ACCOUNT)
        }

        var accountNumber by BillingTable.accountNumber
        var bankName by BillingTable.bankName

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("owner", owner)
            .add("swift", swift)
            .add("dtype", dtype)
            .add("accountNumber", accountNumber)
            .add("bankName", bankName)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `single table with tow type entities`(testDB: TestDB) {
        withTables(testDB, BillingTable) {
            /**
             * ```sql
             * -- Insert BankAccount
             * INSERT INTO billing ("owner", swift, account_number, bank_name, dtype, card_number, company_name, exp_month, exp_year)
             * VALUES ('debop', 'NACFKRSE', '123-456-7890', 'Kookmin Bank', 'BANK_ACCOUNT', NULL, NULL, NULL, NULL)
             * ```
             */
            val account = BankAccount.new {
                owner = "debop"
                swift = "NACFKRSE"
                accountNumber = "123-456-7890"
                bankName = "Kookmin Bank"
            }

            /**
             * ```sql
             * -- Insrt CreditCard
             * INSERT INTO billing ("owner", swift, account_number, bank_name, dtype, card_number, company_name, exp_month, exp_year)
             * VALUES ('debop', 'NACFKRSE', NULL, NULL, 'CREDIT_CARD', '1234-5678-9012-3456', 'VISA', 12, 2023)
             * ```
             */
            val card = CreditCard.new {
                owner = "debop"
                swift = "AXBCRGHCX"
                cardNumber = "1234-5678-9012-3456"
                companyName = "VISA"
                expMonth = 12
                expYear = 2032
            }

            entityCache.clear()

            /**
             * ```sql
             * -- Select BankAccount
             * SELECT billing.id,
             *        billing."owner",
             *        billing.swift,
             *        billing.dtype,
             *        billing.account_number,
             *        billing.bank_name
             *   FROM billing
             *  WHERE (billing.id = 1)
             *    AND (billing.dtype = 'BANK_ACCOUNT')
             * ```
             */
            val account2 = BankAccount.findById(account.id)!!   // 재정의 되었습니다.
            account2 shouldBeEqualTo account

            /**
             * ```sql
             * SELECT billing.id,
             *        billing."owner",
             *        billing.swift,
             *        billing.dtype,
             *        billing.card_number,
             *        billing.company_name,
             *        billing.exp_month,
             *        billing.exp_year,
             *        billing.start_date,
             *        billing.end_date,
             *   FROM billing
             *  WHERE billing.id = 2
             *    AND billing.dtype = 'CREDIT_CARD'
             * ```
             */
            val card2 = CreditCard.findById(card.id)!!     // 재정의 되었습니다.

            card2 shouldBeEqualTo card

            /**
             * BankAccount 만 삭제
             * ```sql
             * DELETE FROM billing WHERE billing.dtype = 'BANK_ACCOUNT'
             * ```
             */
            BillingTable.deleteWhere { BillingTable.dtype eq BillingType.BANK_ACCOUNT }
            /**
             * ```sql
             * SELECT COUNT(billing.id) FROM billing WHERE billing.dtype = 'BANK_ACCOUNT'
             * ```
             */
            BankAccount.count(BillingTable.dtype eq BillingType.BANK_ACCOUNT) shouldBeEqualTo 0L

            /**
             * ```sql
             * SELECT COUNT(billing.id) FROM billing WHERE billing.dtype = 'BANK_ACCOUNT'
             * ```
             */
            BankAccount.countBankAccount() shouldBeEqualTo 0L

            /**
             * 같은 테이블이지만, bank account의 모든 정보가 삭제되어도, credit card 정보는 남아있다
             * ```sql
             * SELECT COUNT(billing.id) FROM billing WHERE billing.dtype = 'CREDIT_CARD'
             * ```
             */
            CreditCard.countCreditCard() shouldBeEqualTo 1L

            /**
             * ```sql
             * DELETE FROM billing WHERE billing.dtype = 'CREDIT_CARD'
             * ```
             */
            BillingTable.deleteWhere { BillingTable.dtype eq BillingType.CREDIT_CARD }

            /**
             * ```sql
             * SELECT COUNT(billing.id) FROM billing WHERE billing.dtype = 'CREDIT_CARD'
             * ```
             */
            CreditCard.countCreditCard() shouldBeEqualTo 0L
        }
    }
}
