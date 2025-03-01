package exposed.examples.jpa.ex05_relations.ex03_many_to_one

import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.Brewery
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.Jug
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.JugMeter
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.SalesForce
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.SalesGuy
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.withBeerTables
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.withJugTables
import exposed.examples.jpa.ex05_relations.ex03_many_to_one.ManyToOneSchema.withSalesTables
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import io.bluetape4k.collections.size
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class Ex01_ManyToOne: AbstractExposedTest() {

    companion object: KLogging()

    /**
     * unidirectional many-to-one
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-one unidirectional`(testDB: TestDB) {
        withJugTables(testDB) { jugs, jugMeters ->
            val jug = Jug.findById(1)!!

            val emmanuel = JugMeter.findById(1)!!
            emmanuel.memberOf shouldBeEqualTo jug

            val jerome = JugMeter.findById(2)!!
            jerome.memberOf shouldBeEqualTo jug

            jugMeters.deleteAll()
            entityCache.clear()

            jugs.selectAll().count() shouldBeEqualTo 1
            JugMeter.all().count() shouldBeEqualTo 0

            jugs.deleteWhere { jugs.id eq 1 }
            Jug.all().count() shouldBeEqualTo 0
        }
    }

    /**
     * ```sql
     * SELECT BREWERY.ID, BREWERY."name"
     *   FROM BREWERY;
     *
     * SELECT BEER.ID, BEER."name", BEER.BREWERY_ID
     *   FROM BEER
     *  WHERE BEER.BREWERY_ID = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-one bidirectional lazy loading`(testDB: TestDB) {
        withBeerTables(testDB) { brewerys, beers ->
            val brewery1 = Brewery.findById(1)!!

            // Fetch lazy loading
            val loaded = Brewery.all().first()
            loaded shouldBeEqualTo brewery1
            loaded.beers shouldHaveSize 3
        }
    }

    /**
     * Eager loading with `with(Brewery::beers)`
     *
     * ```sql
     * -- Postgres
     * SELECT brewery.id, brewery."name" FROM brewery;
     * SELECT beer.id, beer."name", beer.brewery_id FROM beer WHERE beer.brewery_id IN (1, 2);
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-one bidirectional eager loading`(testDB: TestDB) {
        withBeerTables(testDB) { brewerys, beers ->
            val berlin = Brewery.findById(1)!!

            // Eager loading
            val loadedBrewerys = Brewery.all().with(Brewery::beers)
            val loaded = loadedBrewerys.first()
            loaded shouldBeEqualTo berlin
            loaded.beers shouldHaveSize 3
            log.debug { "loaded=$loaded" }

            loadedBrewerys.forEach {
                it.beers.size() shouldBeGreaterThan 0
            }
        }
    }

    /**
     * ```sql
     * -- Postgres
     * DELETE FROM beer WHERE beer.id = 1;
     * SELECT brewery.id, brewery."name" FROM brewery WHERE brewery.id = 1;
     * SELECT beer.id, beer."name", beer.brewery_id FROM beer WHERE beer.brewery_id = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-one bidirectional - remove details`(testDB: TestDB) {
        withBeerTables(testDB) { brewerys, beers ->
            val brewery = Brewery.findById(1)!!
            brewery.beers shouldHaveSize 3

            val beerToRemove = brewery.beers.first()
            beerToRemove.delete()

            entityCache.clear()
            Brewery.findById(1)!!.beers shouldHaveSize 2
        }
    }

    /**
     * ```sql
     * -- Postgres
     * SELECT sales_forces.id, sales_forces."name" FROM sales_forces WHERE sales_forces.id = 1;
     * SELECT sales_guys.id, sales_guys."name", sales_guys.sales_force_id FROM sales_guys WHERE sales_guys.sales_force_id = 1;
     * ```
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `many-to-one bidirectional with cascade all`(testDB: TestDB) {
        withSalesTables(testDB) { salesForces, salesGuys ->
            val salesForce = SalesForce.new { name = "BMW Korea" }
            val salesGuy1 = SalesGuy.new { name = "debop"; this.salesForce = salesForce }
            val salesGuy2 = SalesGuy.new { name = "smith"; this.salesForce = salesForce }
            val salesGuy3 = SalesGuy.new { name = "james"; this.salesForce = salesForce }

            flushCache()
            entityCache.clear()

            // eager loading
            val loaded = SalesForce.findById(salesForce.id)!!.load(SalesForce::salesGuys)
            log.debug { "loaded=$loaded" }
            loaded shouldBeEqualTo salesForce
            loaded.salesGuys.toList() shouldBeEqualTo listOf(salesGuy1, salesGuy2, salesGuy3)

            val guyToRemove = loaded.salesGuys.last()
            guyToRemove.delete()

            flushCache()
            entityCache.clear()

            // eager loading
            val loaded2 = SalesForce.findById(salesForce.id)!!.load(SalesForce::salesGuys)
            loaded2 shouldBeEqualTo salesForce
            loaded2.salesGuys.toList() shouldBeEqualTo listOf(salesGuy1, salesGuy2)
        }
    }
}
