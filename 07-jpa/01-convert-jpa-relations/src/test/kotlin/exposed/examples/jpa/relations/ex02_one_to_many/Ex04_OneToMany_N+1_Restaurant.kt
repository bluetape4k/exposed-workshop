package exposed.examples.jpa.relations.ex02_one_to_many

import exposed.examples.jpa.relations.ex02_one_to_many.schema.RestaurantSchema.Menu
import exposed.examples.jpa.relations.ex02_one_to_many.schema.RestaurantSchema.MenuTable
import exposed.examples.jpa.relations.ex02_one_to_many.schema.RestaurantSchema.Restaurant
import exposed.examples.jpa.relations.ex02_one_to_many.schema.RestaurantSchema.RestaurantTable
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Transaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.random.Random

class `Ex04_OneToMany_N+1_Restaurant`: AbstractExposedTest() {


    companion object: KLogging()

    /**
     * Eager loading `Menu` entities (`with(Restaurant::menus)`)
     *
     * 참고: [Eager Loaing](https://jetbrains.github.io/Exposed/dao-relationships.html#eager-loading)
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `handle one-to-many relationship`(testDB: TestDB) {
        withTables(testDB, RestaurantTable, MenuTable) {
            createSamples("KFC")
            createSamples("McDonald")

            entityCache.clear()

            /**
             * Earger loading `Menu` entities (`with(Restaurant::menus)`)
             *
             * ```sql
             * SELECT restaurant.id, restaurant."name"
             *   FROM restaurant;
             *
             * SELECT menu.id, menu."name", menu.price, menu.restaurant_id
             *   FROM menu
             *  WHERE menu.restaurant_id IN (1, 2);
             * ```
             */
            val restaurants = Restaurant.all().with(Restaurant::menus).toList()
            val restaurant = restaurants.first()
            log.debug { "Restaurant: $restaurant" }

            restaurant.menus.count() shouldBeEqualTo 2L
            restaurant.menus.forEach { menu ->
                log.debug { ">> Menu: $menu" }
            }
        }
    }

    private fun Transaction.createSamples(name: String): Restaurant {
        val restaurant = Restaurant.new {
            this.name = name
        }

        Menu.new {
            this.name = "Chicken"
            price = Random.nextDouble(5.0, 10.0).toBigDecimal()
            this.restaurant = restaurant
        }

        Menu.new {
            this.name = "Burger"
            this.price = Random.nextDouble(5.0, 7.0).toBigDecimal()
            this.restaurant = restaurant
        }

        commit()
        return restaurant
    }
}
