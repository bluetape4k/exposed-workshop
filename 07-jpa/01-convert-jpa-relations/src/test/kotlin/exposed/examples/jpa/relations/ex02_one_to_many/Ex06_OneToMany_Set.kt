package exposed.examples.jpa.relations.ex02_one_to_many

import exposed.shared.dao.idEquals
import exposed.shared.dao.idHashCode
import exposed.shared.dao.toStringBuilder
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContainSame
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.entityCache
import org.jetbrains.exposed.dao.flushCache
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.math.BigDecimal

/**
 * one-to-many relation 에서 many 쪽이 set 으로 매핑된 경우
 *
 * Exposed 에서는 명시적으로 SET 을 지원하지 않지만, Table 정의 시 unique key를 설정하면 SET 처럼 동작한다.
 */
class Ex06_OneToMany_Set: AbstractExposedTest() {


    companion object: KLogging()

    /**
     * 경매 물품
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS bidding_items (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL
     * );
     * ```
     */
    object BiddingItemTable: IntIdTable("bidding_items") {
        val name = varchar("name", 255)
    }

    /**
     * 경매 입찰
     *
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS bids (
     *      id SERIAL PRIMARY KEY,
     *      amount DECIMAL(10, 2) DEFAULT 0 NOT NULL,
     *      item_id INT NOT NULL,
     *
     *      CONSTRAINT fk_bids_item_id__id FOREIGN KEY (item_id)
     *      REFERENCES bidding_items(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     *
     * -- bids 가 set 방식이기 위해 unique key를 설정한다.
     * ALTER TABLE bids ADD CONSTRAINT bid_item_id UNIQUE (id, item_id);
     * ```
     */
    object BidTable: IntIdTable("bids") {
        val amount = decimal("amount", 10, 2).default(0.toBigDecimal())
        val itemId = reference("item_id", BiddingItemTable, onDelete = CASCADE)

        init {
            // one-to-many relation 에서 SET 방식으로 매핑하기 위해 unique key를 설정한다.
            uniqueIndex("bid_item_id", id, itemId)
        }
    }

    class BiddingItem(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<BiddingItem>(BiddingItemTable)

        var name: String by BiddingItemTable.name

        // 경매 입찰들 (Bid 가 Unique 해야 한다)
        val bids: SizedIterable<Bid> by Bid referrersOn BidTable.itemId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .toString()
    }

    class Bid(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Bid>(BidTable)

        var amount: BigDecimal by BidTable.amount

        // many-to-one relation
        var item: BiddingItem by BiddingItem referencedOn BidTable.itemId

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("amount", amount)
            .toString()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many set with bidirectional`(testDB: TestDB) {
        withTables(testDB, BiddingItemTable, BidTable) {
            val item1 = BiddingItem.new { name = "TV" }
            val bid1 = Bid.new { amount = 100.toBigDecimal(); item = item1 }
            val bid2 = Bid.new { amount = 200.toBigDecimal(); item = item1 }
            val bid3 = Bid.new { amount = 300.toBigDecimal(); item = item1 }

            flushCache()
            entityCache.clear()

            val loaded = BiddingItem.findById(item1.id)!!
            loaded shouldBeEqualTo item1
            loaded.bids.toSet() shouldContainSame setOf(bid1, bid2, bid3)

            val bidToRemove = loaded.bids.first()
            bidToRemove.delete()

            flushCache()
            entityCache.clear()

            val loaded2 = BiddingItem.findById(item1.id)!!
            loaded2 shouldBeEqualTo item1

            // 이미 DB 상에서 Unique 하기 때문에, 굳이 `toSet()` 하지 않아도 된다.
            // SELECT bids.id, bids.amount, bids.item_id FROM bids WHERE bids.item_id = 1
            loaded2.bids.toSet() shouldContainSame setOf(bid1, bid2, bid3) - bidToRemove

            log.debug { "delete bidding item" }
            loaded2.delete()
            entityCache.clear()

            BiddingItem.all().count() shouldBeEqualTo 0L
            // onDelete = CASCADE로 인해 bids도 삭제된다.
            Bid.all().count() shouldBeEqualTo 0L
        }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS products (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      description TEXT NULL,
     *      initial_price DECIMAL(10, 2) NULL,
     *      reserve_price DECIMAL(10, 2) NULL,
     *      start_date DATE NULL,
     *      end_date DATE NULL,
     *      status VARCHAR(10) NOT NULL
     * );
     * ```
     */
    object ProductTable: IntIdTable("products") {
        val name = varchar("name", 255)
        val description = text("description").nullable()
        val initialPrice = decimal("initial_price", 10, 2).nullable()
        val reservePrice = decimal("reserve_price", 10, 2).nullable()
        val startDate = date("start_date").nullable()
        val endDate = date("end_date").nullable()

        val status = enumerationByName("status", 10, ProductStatus::class)
            .clientDefault { ProductStatus.ACTIVE }
    }

    /**
     * ```sql
     * -- Postgres
     * CREATE TABLE IF NOT EXISTS product_images (
     *      id SERIAL PRIMARY KEY,
     *      "name" VARCHAR(255) NOT NULL,
     *      filename VARCHAR(255) NULL,
     *      size_x INT NULL,
     *      size_y INT NULL,
     *      product_id INT NULL,
     *
     *      CONSTRAINT fk_product_images_product_id__id FOREIGN KEY (product_id)
     *      REFERENCES products(id) ON DELETE CASCADE ON UPDATE RESTRICT
     * );
     *
     * ALTER TABLE product_images ADD CONSTRAINT product_image_name_product_id UNIQUE ("name", product_id);
     * ```
     */
    object ProductImageTable: IntIdTable("product_images") {
        val name = varchar("name", 255)
        val filename = varchar("filename", 255).nullable()
        val sizeX = integer("size_x").nullable()
        val sizeY = integer("size_y").nullable()
        val productId: Column<EntityID<Int>?> =
            optReference("product_id", ProductTable, onDelete = CASCADE)

        init {
            uniqueIndex("product_image_name_product_id", name, productId)
        }
    }

    class Product(id: EntityID<Int>): IntEntity(id) {
        companion object: IntEntityClass<Product>(ProductTable)

        var name by ProductTable.name
        var description by ProductTable.description
        var initialPrice by ProductTable.initialPrice
        var reservePrice by ProductTable.reservePrice
        var startDate by ProductTable.startDate
        var endDate by ProductTable.endDate
        var status by ProductTable.status

        /**
         * JPA의 ElementCollection과 같은 역할을 수행한다. [ProductImage]는 Entity 가 아니라 Value Object 이다.
         * 단, 컬렉션의 요소를 조작하는 것은 `addImages`, `removeImages` 메소드를 통해서만 가능하다.
         *
         * [ProductImage] 를 얻고 가져온다.
         *
         * ```sql
         * -- Postgres
         * SELECT product_images.id,
         *        product_images."name",
         *        product_images.filename,
         *        product_images.size_x,
         *        product_images.size_y,
         *        product_images.product_id
         *   FROM product_images
         *  WHERE product_images.product_id = 1
         * ```
         */
        val images: Set<ProductImage>
            get() = ProductImageTable.selectAll()
                .where { ProductImageTable.productId eq id }
                .map {
                    ProductImage(
                        name = it[ProductImageTable.name],
                        filename = it[ProductImageTable.filename],
                        sizeX = it[ProductImageTable.sizeX],
                        sizeY = it[ProductImageTable.sizeY]
                    )
                }
                .toSet()

        fun addImages(vararg imagesToAdd: ProductImage) {
            // 중복된 값이 들어가지 않도록 image name, productId 조합이 unique key로 설정되어 있다.
            ProductImageTable.batchInsert(
                imagesToAdd.asList(),
                ignore = true,
                shouldReturnGeneratedValues = false
            ) { image ->
                this[ProductImageTable.name] = image.name
                this[ProductImageTable.filename] = image.filename
                this[ProductImageTable.sizeX] = image.sizeX
                this[ProductImageTable.sizeY] = image.sizeY
                this[ProductImageTable.productId] = this@Product.id
            }
        }

        fun removeImages(vararg imagesToRemove: ProductImage) {
            ProductImageTable.deleteWhere {
                ProductImageTable.productId eq this@Product.id and
                        (ProductImageTable.name inList imagesToRemove.map { it.name })
            }
        }

        override fun equals(other: Any?): Boolean = idEquals(other)
        override fun hashCode(): Int = idHashCode()
        override fun toString(): String = toStringBuilder()
            .add("name", name)
            .add("status", status)
            .toString()
    }

    /**
     * [ProductImage]는 Entity가 아니라 Value Object (Component) 이다.
     */
    data class ProductImage(
        val name: String,
        val filename: String? = null,
        val sizeX: Int? = null,
        val sizeY: Int? = null,
    ): Serializable

    enum class ProductStatus {
        UNKNOWN,
        ACTIVE,
        INACTIVE,
        SOLD_OUT
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `one-to-many with embeddable component by @ElementCollection`(testDB: TestDB) {
        // H2, H2_MARIADB 는 INSERT IGNORE 를 지원하지 않는다.
        Assumptions.assumeTrue { testDB !in setOf(TestDB.H2, TestDB.H2_MARIADB) }

        withTables(testDB, ProductTable, ProductImageTable) {
            val product1 = Product.new { name = "Car" }
            val image1 = ProductImage("front")
            val image2 = ProductImage("interior")
            val image3 = ProductImage("engine room")

            product1.addImages(image1, image2, image3)

            flushCache()
            entityCache.clear()

            val loaded = Product.findById(product1.id)!!
            log.debug { "loaded product: $loaded" }
            loaded.images shouldContainSame setOf(image1, image2, image3)

            val imageToRemove = image2
            loaded.removeImages(imageToRemove)

            flushCache()
            entityCache.clear()

            val loaded2 = Product.findById(product1.id)!!
            loaded2 shouldBeEqualTo product1
            loaded2.images shouldContainSame setOf(image1, image3)

            /**
             * [Product]를 삭제하면 [ProductImage]도 삭제된다. (onDelete = CASCADE)
             *
             * ```sql
             * DELETE FROM products WHERE products.id = 1
             * ```
             */
            loaded2.delete()

            Product.all().count() shouldBeEqualTo 0L
            ProductImageTable.selectAll().count() shouldBeEqualTo 0L
        }
    }
}
