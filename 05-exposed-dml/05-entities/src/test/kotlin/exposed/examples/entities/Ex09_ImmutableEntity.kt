package exposed.examples.entities

import exposed.examples.entities.Ex09_ImmutableEntity.Schema.CachedOrganization
import exposed.examples.entities.Ex09_ImmutableEntity.Schema.Organization
import exposed.examples.entities.Ex09_ImmutableEntity.Schema.Organizations
import exposed.shared.tests.JdbcExposedTestBase
import exposed.shared.tests.TestDB
import exposed.shared.tests.withTables
import io.bluetape4k.exposed.dao.idEquals
import io.bluetape4k.exposed.dao.idHashCode
import io.bluetape4k.exposed.dao.toStringBuilder
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.ImmutableCachedEntityClass
import org.jetbrains.exposed.v1.dao.ImmutableEntityClass
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * 읽기 전용 / 캐싱된 엔티티 사용 예
 */
class Ex09_ImmutableEntity: JdbcExposedTestBase() {

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
        object Organizations: LongIdTable("organization") {
            val name = varchar("name", 256)
            val etag = long("etag").default(0)
        }

        /**
         * 읽기 전용 엔티티
         *
         * 엔티티의 값을 변경하려면 forceUpdateEntity 메서드를 사용해야 한다.
         */
        class Organization(id: EntityID<Long>): LongEntity(id) {
            // 읽기 전용 엔티티를 사용하기 위해서는 ImmutableEntityClass 를 사용한다.
            companion object: ImmutableEntityClass<Long, Organization>(Organizations)

            // immutable 이므로 모든 프로퍼티는 val 로 선언한다.
            val name: String by Schema.Organizations.name
            val etag: Long by Schema.Organizations.etag

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
        class CachedOrganization(id: EntityID<Long>): LongEntity(id) {
            // 읽기 전용 + 캐시된 엔티티를 사용하기 위해서는 ImmutableCachedEntityClass 를 사용한다.
            companion object: ImmutableCachedEntityClass<Long, CachedOrganization>(Organizations)

            // Cached Entity 는 캐시된 값을 사용한다. (read-only)
            val name: String by Schema.Organizations.name
            val etag: Long by Schema.Organizations.etag

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
        withTables(testDB, Organizations) {
            // Immutable 엔티티만 있으므로, DSL 로 새로운 레코드를 생성합니다.
            transaction {
                // INSERT INTO organization ("name", etag) VALUES ('JetBrains', 0)
                Organizations.insert {
                    it[name] = "JetBrains"
                    it[etag] = 0
                }
            }

            transaction {
                // NOTE: 읽기전용이므로, new 작업은 불가하다 (컴파일 예외 발생)
                // Organization.new {
                //    name = "Platform"
                //    etag = 10
                // }
                val org = Organization.all().single()

                // Immutable 엔티티를 강제로 업데이트
                // UPDATE organization SET etag=42 WHERE organization.id = 1
                Organization.forceUpdateEntity(org, Organizations.etag, 42)

                // 강제 업데이트된 정보를 DB로부터 읽어온다.
                // SELECT organization.id, organization."name", organization.etag FROM organization
                Organization.all().single().etag shouldBeEqualTo 42L
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `immutable entity read after update with cached entity`(testDB: TestDB) {
        withTables(testDB, Organizations) {
            transaction {
                // INSERT INTO organization ("name", etag) VALUES ('JetBrains', 0)
                Organizations.insert {
                    it[name] = "JetBrains"
                    it[etag] = 0
                }
            }
            transaction {
                // UPDATE organization SET "name"='JetBrains Inc.'
                Organizations.update {
                    it[name] = "JetBrains Inc."
                }
            }

            transaction {
                val org = CachedOrganization.all().single()

                // Immutable Cached 엔티티를 강제로 업데이트
                //  UPDATE organization SET "name"='JetBrains Gmbh' WHERE organization.id = 1
                CachedOrganization.forceUpdateEntity(org, Organizations.name, "JetBrains Gmbh")

                // DSL 을 이용하여 직접 업데이트한 경우 Cached 엔티티에 반영되지 않는다.
                // UPDATE organization SET etag=1 WHERE organization.id = 1
                Organizations.update({ Organizations.id eq org.id }) {
                    it[etag] = 1
                }
                // 엔티티에는 DSL Update 가 반영 안된다.
                org.name shouldBeEqualTo "JetBrains Gmbh"
                org.etag shouldBeEqualTo 0L

                // DB에서 다시 로드 시에는 값이 Update 된다.
                val org2 = CachedOrganization.all().single()

                org2.name shouldBeEqualTo "JetBrains Gmbh"
                org2.etag shouldBeEqualTo 1L

                // 캐시된 엔티티는 update 되지 않는다.
                org.etag shouldNotBeEqualTo org2.etag
            }

            log.debug { "New other transaction" }

            // 다른 Transaction 에서도 업데이트된 캐시 값 (org2) 을 사용한다.
            transaction {
                val org = CachedOrganization.all().single()

                org.name shouldBeEqualTo "JetBrains Gmbh"
                org.etag shouldBeEqualTo 1L
            }
        }
    }
}
