package exposed.examples.jpa.ex01_relations.ex02_one_to_many.schema

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SizedIterable

object RestaurantSchema {

    val restaurantTables = arrayOf(RestaurantTable, MenuTable)

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS restaurant (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object RestaurantTable: IntIdTable("restaurant") {
        val name = varchar("name", 255)
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS menu (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      price DECIMAL(10, 2) NOT NULL,
     *      restaurant_id INT NOT NULL,
     *
     *      CONSTRAINT fk_menu_restaurant_id__id FOREIGN KEY (restaurant_id)
     *      REFERENCES restaurant(id) ON DELETE RESTRICT ON UPDATE RESTRICT
     * );
     *
     * CREATE INDEX menu_restaurant_id ON menu (restaurant_id);
     * ```
     */
    object MenuTable: IntIdTable("menu") {
        val name = varchar("name", 255)
        val price = decimal("price", 10, 2)

        // reference to Restaurant
        val restaurantId = reference("restaurant_id", RestaurantTable).index()
    }

    class Restaurant(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Restaurant>(RestaurantTable)

        var name by RestaurantTable.name

        // one-to-many relationship
        val menus: SizedIterable<Menu> by Menu referrersOn MenuTable.restaurantId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Menu(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Menu>(MenuTable)

        var name by MenuTable.name
        var price by MenuTable.price

        // many-to-one relationship
        var restaurant: Restaurant by Restaurant referencedOn MenuTable.restaurantId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("price", price)
            .add("restaurant id", restaurant.id._value)
            .toString()
    }

}
