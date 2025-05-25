package exposed.examples.jpa.ex05_relations.ex04_many_to_many

import exposed.examples.jpa.ex05_relations.ex04_many_to_many.BankSchema.AccountOwner
import exposed.examples.jpa.ex05_relations.ex04_many_to_many.BankSchema.BankAccount
import exposed.examples.jpa.ex05_relations.ex04_many_to_many.BankSchema.OwnerAccountMapTable
import exposed.examples.jpa.ex05_relations.ex04_many_to_many.BankSchema.getAccount
import exposed.examples.jpa.ex05_relations.ex04_many_to_many.BankSchema.getOwner
import exposed.examples.jpa.ex05_relations.ex04_many_to_many.BankSchema.withBankTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.collections.size
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.entityCache
import org.jetbrains.exposed.v1.dao.load
import org.jetbrains.exposed.v1.dao.with
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * many-to-many 관계를 owner 와 account 로 각각 조회하는 예제
 */
class Ex01_ManyToMany_Bank: AbstractExposedTest() {

    companion object: KLogging()


    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-many manipulation by owner`(testDB: TestDB) {
        withBankTables(testDB) { accounts, owners ->
            val account1 = getAccount(1)
            val account2 = getAccount(2)
            val account3 = getAccount(3)
            val account4 = getAccount(4)

            val owner1 = getOwner(1)
            val owner2 = getOwner(2)

            entityCache.clear()

            /**
             * Eager loading with `with(AccountOwner::accounts)`
             *
             * ```sql
             * -- Postgres
             * SELECT account_owner.id, account_owner.ssn FROM account_owner
             * SELECT bank_account.id,
             *        bank_account."number",
             *        owner_account_map.owner_id,
             *        owner_account_map.account_id
             *   FROM bank_account INNER JOIN owner_account_map ON owner_account_map.account_id = bank_account.id
             *  WHERE owner_account_map.owner_id IN (1, 2)
             * ```
             */
            val ownersWithAccounts = AccountOwner.all().with(AccountOwner::accounts).toList()
            ownersWithAccounts shouldHaveSize 2

            /**
             * Eager loading with `load(AccountOwner::accounts)`
             *
             * ```sql
             * -- Postgres
             * SELECT account_owner.id, account_owner.ssn
             *   FROM account_owner
             *  WHERE account_owner.id = 1;
             *
             * SELECT bank_account.id, bank_account."number",
             *        owner_account_map.owner_id, owner_account_map.account_id
             *   FROM bank_account
             *          INNER JOIN owner_account_map ON owner_account_map.account_id = bank_account.id
             *  WHERE owner_account_map.owner_id = 1;
             * ```
             */
            var loadedOwner1 = AccountOwner.findById(account1.id)!!.load(AccountOwner::accounts)
            loadedOwner1.accounts.count() shouldBeEqualTo 2L
            loadedOwner1.accounts shouldContainSame listOf(account1, account2)

            var loadedOwner2 = AccountOwner.findById(owner2.id)!!.load(AccountOwner::accounts)
            loadedOwner2.accounts.count().toInt() shouldBeEqualTo owner2.accounts.size()
            loadedOwner2.accounts shouldContainSame listOf(account1, account3, account4)

            entityCache.clear()

            /**
             * Eager loading with `load(BankAccount::owners)`
             * ```sql
             * -- Postgres
             * SELECT bank_account.id, bank_account."number"
             *   FROM bank_account
             *  WHERE bank_account.id = 1;
             *
             * SELECT account_owner.id, account_owner.ssn,
             *        owner_account_map.owner_id, owner_account_map.account_id
             *   FROM account_owner
             *          INNER JOIN owner_account_map ON owner_account_map.owner_id = account_owner.id
             *  WHERE owner_account_map.account_id = 1;
             * ```
             */
            val loadedAccount1 = BankAccount.findById(account1.id)!!.load(BankAccount::owners)
            loadedAccount1.owners.count() shouldBeEqualTo 2L
            loadedAccount1.owners shouldContainSame listOf(owner1, owner2)

            /**
             * Delete mapping (ownerId = 2, accountId = 3)
             *
             * ```sql
             * -- Postgres
             * DELETE FROM owner_account_map
             *  WHERE (owner_account_map.owner_id = 2) AND (owner_account_map.account_id = 3)
             * ```
             */
            OwnerAccountMapTable.deleteWhere {
                (ownerId eq owner2.id) and (accountId eq account3.id)
            }

            entityCache.clear()

            loadedOwner1 = AccountOwner.findById(owner1.id)!!.load(AccountOwner::accounts)
            loadedOwner1.accounts.count() shouldBeEqualTo owner1.accounts.size().toLong()
            loadedOwner1.accounts shouldContainSame listOf(account1, account2)

            // owner2 에서 account3 을 삭제했으므로, owner2 에서 account3 은 삭제된다.
            loadedOwner2 = AccountOwner.findById(owner2.id)!!.load(AccountOwner::accounts)
            loadedOwner2.accounts.count() shouldBeEqualTo owner2.accounts.size().toLong()
            loadedOwner2.accounts shouldContainSame listOf(account1, account4)

            /**
             * many-to-many 관계만 삭제된다.
             *
             * ```sql
             * DELETE FROM owner_account_map WHERE owner_account_map.owner_id = 2
             * ```
             */
            OwnerAccountMapTable.deleteWhere { ownerId eq loadedOwner2.id }

            // DELETE FROM account_owner WHERE account_owner.id = 2
            loadedOwner2.delete()

            entityCache.clear()

            /**
             * Lazy loading
             * ```sql
             * -- Postgres
             * SELECT bank_account.id, bank_account."number"
             *   FROM bank_account
             * WHERE bank_account.id = 3;
             *
             * SELECT COUNT(*)
             *   FROM account_owner
             *          INNER JOIN owner_account_map ON account_owner.id = owner_account_map.owner_id
             *  WHERE owner_account_map.account_id = 3
             * ```
             */
            val removedAccount = BankAccount.findById(account3.id)!!
            removedAccount.owners.count() shouldBeEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-many manipulation by account`(testDB: TestDB) {
        withBankTables(testDB) { accounts, owners ->
            val account1 = getAccount(1)
            val account2 = getAccount(2)
            val account3 = getAccount(3)
            val account4 = getAccount(4)

            val owner1 = getOwner(1)
            val owner2 = getOwner(2)

            account1.assertAccountExists()
            account2.assertAccountExists()
            account3.assertAccountExists()
            account4.assertAccountExists()

            /**
             * Delete mapping (accountId = 3, ownerId = 2)
             *
             * ```sql
             * -- Postgres
             * DELETE FROM owner_account_map
             *  WHERE (owner_account_map.account_id = 3)
             *    AND (owner_account_map.owner_id = 2)
             * ```
             */
            OwnerAccountMapTable.deleteWhere { (accountId eq account3.id) and (ownerId eq owner2.id) }

            /**
             * Eager loading
             *
             * ```sql
             * -- Postgres
             * SELECT account_owner.id, account_owner.ssn
             *   FROM account_owner
             *  WHERE account_owner.id = 2;
             *
             * SELECT bank_account.id,
             *        bank_account."number",
             *        owner_account_map.owner_id,
             *        owner_account_map.account_id
             *   FROM bank_account
             *      INNER JOIN owner_account_map
             *        ON owner_account_map.account_id = bank_account.id
             *  WHERE owner_account_map.owner_id = 2
             * ```
             */
            val loaded = AccountOwner.findById(owner2.id)!!.load(AccountOwner::accounts)
            loaded.accounts.count().toInt() shouldBeEqualTo 2
        }
    }

    /**
     * Eager loading with `with(BankAccount::owners)`
     *
     * ```sql
     * -- Postgres
     * SELECT bank_account.id, bank_account."number"
     *   FROM bank_account
     *  WHERE bank_account.id = 1;
     *
     * SELECT account_owner.id,
     *        account_owner.ssn,
     *        owner_account_map.owner_id,
     *        owner_account_map.account_id
     *   FROM account_owner
     *          INNER JOIN owner_account_map ON owner_account_map.owner_id = account_owner.id
     *  WHERE owner_account_map.account_id = 1
     * ```
     */
    private fun BankAccount.assertAccountExists() {
        val account = this

        val loaded = BankAccount.findById(account.id)!!.load(BankAccount::owners)
        loaded shouldBeEqualTo account

        loaded.owners.count() shouldBeEqualTo account.owners.size().toLong()
    }
}
