package exposed.examples.custom.entities

import exposed.examples.custom.entities.Ex09_ImmutableEntity.Schema.Organization.etag
import exposed.examples.entities.Ex09_ImmutableEntity.Schema.ECachedOrganization
import exposed.examples.entities.Ex09_ImmutableEntity.Schema.EOrganization
import exposed.examples.entities.Ex09_ImmutableEntity.Schema.Organization
import exposed.shared.tests.AbstractExposedTest
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.jetbrains.exposed.dao.ImmutableCachedEntityClass
import org.jetbrains.exposed.dao.ImmutableEntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 읽기 전용 / 캐싱된 엔티티 사용 예
 */
class Ex09_ImmutableEntity: AbstractExposedTest() {

    companion object: KLogging()

    object Schema {
        /**
         * ```sql
         * -- Postgres
         * CREATE TABLE IF NOT EXISTS organization (
         *      id BIGSERIAL PRIMARY KEY,
         *      "name" VARCHAR(256) NOT NULL,
         *      etag BIGINT DEFAULT 0 NOT NULL
         * )
         * ```
         */
        object Organization: LongIdTable("organization") {
            val name = varchar("name", 256)
            val etag = long("etag").default(0)
        }

        /**
         * 읽기 전용 엔티티
         *
         * 엔티티의 값을 변경하려면 forceUpdateEntity 메서드를 사용해야 한다.
         */
        class EOrganization(id: EntityID<Long>): LongEntity(id) {
            // 읽기 전용 엔티티를 사용하기 위해서는 ImmutableEntityClass 를 사용한다.
            companion object: ImmutableEntityClass<Long, EOrganization>(Organization, EOrganization::class.java)

            // immutable 이므로 모든 프로퍼티는 val 로 선언한다.
            val name: String by Schema.Organization.name
            val etag: Long by Schema.Organization.etag

            override fun equals(other: Any?): Boolean = idEquals(other)
            override fun hashCode(): Int = idHashCode()
            override fun toString(): String = toStringBuilder()
                .add("name", name)
                .add("etag", etag)
                .toString()
        }

        /**
         * 캐시된 엔티티
         *
         * 엔티티의 값을 변경하려면 forceUpdateEntity 메서드를 사용해야 한다.
         */
        class ECachedOrganization(id: EntityID<Long>): LongEntity(id) {
            // 읽기 전용, 캐시된 엔티티를 사용하기 위해서는 ImmutableCachedEntityClass 를 사용한다.
            companion object: ImmutableCachedEntityClass<Long, ECachedOrganization>(
                Organization,
                ECachedOrganization::class.java
            )

            // Cached Entity 는 캐시된 값을 사용한다. (read-only)
            val name: String by Schema.Organization.name
            val etag: Long by Schema.Organization.etag

            override fun equals(other: Any?): Boolean = idEquals(other)
            override fun hashCode(): Int = idHashCode()
            override fun toString(): String = toStringBuilder()
                .add("name", name)
                .add("etag", etag)
                .toString()
        }
    }

    /**
     * `forceUpdateEntity` 메서드를 사용하여 Immutable Entity의 값을 변경할 수 있다.
     */
    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `immutable entity read after update`(testDB: TestDB) {
        withTables(testDB, Organization) {
            // Immutable 엔티티만 있으므로, DSL 로 새로운 레코드를 생성합니다.
            transaction {
                // INSERT INTO organization ("name", etag) VALUES ('JetBrains', 0)
                Organization.insert {
                    it[name] = "JetBrains"
                    it[etag] = 0
                }
            }

            transaction {
                val org = EOrganization.all().single()

                // Immutable 엔티티를 강제로 업데이트
                // UPDATE organization SET etag=42 WHERE organization.id = 1
                EOrganization.forceUpdateEntity(org, Organization.etag, 42)

                // 강제 업데이트된 정보를 DB로부터 읽어온다.
                // SELECT organization.id, organization."name", organization.etag FROM organization
                EOrganization.all().single().etag shouldBeEqualTo 42L
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `immutable entity read after update with cached entity`(testDB: TestDB) {
        withTables(testDB, Organization) {
            transaction {
                // INSERT INTO organization ("name", etag) VALUES ('JetBrains', 0)
                Organization.insert {
                    it[name] = "JetBrains"
                    it[etag] = 0
                }
            }
            transaction {
                // UPDATE organization SET "name"='JetBrains Inc.'
                Organization.update {
                    it[name] = "JetBrains Inc."
                }
            }

            transaction {
                val org = ECachedOrganization.all().single()

                // Immutable Cached 엔티티를 강제로 업데이트
                //  UPDATE organization SET "name"='JetBrains Gmbh' WHERE organization.id = 1
                ECachedOrganization.forceUpdateEntity(org, Organization.name, "JetBrains Gmbh")

                // DSL 을 이용하여 직접 업데이트한 경우 Cached 엔티티에 반영되지 않는다.
                // UPDATE organization SET etag=1 WHERE organization.id = 1
                Organization.update({ Organization.id eq org.id }) {
                    it[etag] = 1
                }
                // 엔티티에는 DSL Update 가 반영 안된다.
                org.name shouldBeEqualTo "JetBrains Gmbh"
                org.etag shouldBeEqualTo 0L

                // DB에서 다시 로드 시에는 값이 Update 된다.
                val org2 = ECachedOrganization.all().single()

                org2.name shouldBeEqualTo "JetBrains Gmbh"
                org2.etag shouldBeEqualTo 1L

                // 캐시된 엔티티는 update 되지 않는다.
                org.etag shouldNotBeEqualTo org2.etag
            }

            log.debug { "New other transaction" }

            // 다른 Transaction 에서는 다른 캐시를 사용하므로 업데이트된 값을 읽어온다.
            transaction {
                // 모두 캐시되어 있으므로 실제 DB로 부터 읽어오지는 않습니다.
                val org = ECachedOrganization.all().single()

                org.name shouldBeEqualTo "JetBrains Gmbh"
                org.etag shouldBeEqualTo 1L
            }
        }
    }
}
