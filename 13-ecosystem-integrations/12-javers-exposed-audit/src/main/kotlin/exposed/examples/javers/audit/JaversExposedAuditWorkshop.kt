package exposed.examples.javers.audit

import io.bluetape4k.javers.persistence.exposed.hook.ExposedJaversEntityHookMapping
import io.bluetape4k.javers.persistence.exposed.hook.ExposedJaversEntityHookSubscription
import io.bluetape4k.javers.persistence.exposed.repository.ExposedCdoSnapshotRepository
import io.bluetape4k.javers.repository.jql.queryByInstanceId
import org.javers.core.Changes
import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.metamodel.annotation.Id
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.SnapshotType
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.dao.IntEntity
import org.jetbrains.exposed.v1.dao.IntEntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.io.Serializable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.WeakHashMap

/** 감사 이력 예제에서 사용하는 고객 테이블입니다. */
object Customers : IntIdTable("audit_customers") {
    val name = varchar("name", 100)
    val email = varchar("email", 200)
    val secret = varchar("secret", 200)
}

/** Exposed DAO 고객 엔티티입니다. */
class CustomerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CustomerEntity>(Customers)

    var name by Customers.name
    var email by Customers.email
    var secret by Customers.secret

    /** 감사 저장소에 보낼 민감하지 않은 detached 객체를 만듭니다. */
    fun toAuditObject(): AuditedCustomer {
        return AuditedCustomer(
            id = id.value,
            name = name,
            email = email,
        )
    }
}

/** JaVers에 저장할 고객 표현입니다. `secret`은 의도적으로 포함하지 않습니다. */
data class AuditedCustomer(
    @Id val id: Int,
    val name: String,
    val email: String,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** 한 JDBC 트랜잭션의 감사 주체와 요청 식별자입니다. */
data class AuditContext(
    val actor: String,
    val requestId: String,
) {
    init {
        require(actor.isNotBlank()) { "actor must not be blank" }
        require(requestId.isNotBlank()) { "requestId must not be blank" }
    }
}

/** 현재 스레드의 감사 컨텍스트를 중첩 가능하게 관리합니다. */
object AuditContextHolder {
    private val current = ThreadLocal<AuditContext?>()

    /** 블록 동안 컨텍스트를 설정하고 정상·예외 종료 후 이전 값을 복원합니다. */
    fun <T> with(context: AuditContext, block: () -> T): T {
        val previous = current.get()
        current.set(context)
        return try {
            block()
        } finally {
            if (previous == null) {
                current.remove()
            } else {
                current.set(previous)
            }
        }
    }

    /** 컨텍스트가 없으면 익명 감사 기록을 만들지 않고 실패합니다. */
    fun requireCurrent(): AuditContext {
        return current.get() ?: error("AuditContext is required for JaVers commit")
    }

    /** 테스트와 작업 경계에서 남은 스레드 로컬 상태를 제거합니다. */
    internal fun clear() {
        current.remove()
    }
}

/** 특정 고객의 조회 결과를 묶은 읽기 전용 감사 이력입니다. */
data class AuditHistory(
    val current: AuditedCustomer?,
    val snapshots: List<CdoSnapshot>,
    val changes: Changes,
)

/** JaVers 스냅샷·변경·이력 조회를 제공합니다. */
class JaversAuditHistory(
    private val javers: Javers,
) {
    /** 고객의 스냅샷을 최신 커밋부터 조회합니다. */
    fun snapshots(customerId: Int): List<CdoSnapshot> {
        return javers.findSnapshots(queryByInstanceId<AuditedCustomer>(customerId))
    }

    /** 고객의 속성 변경을 조회합니다. */
    fun changes(customerId: Int): Changes {
        return javers.findChanges(queryByInstanceId<AuditedCustomer>(customerId))
    }

    /** 스냅샷과 변경을 함께 반환합니다. */
    fun history(customerId: Int): AuditHistory {
        val snapshots = snapshots(customerId)
        return AuditHistory(
            current = snapshots.firstOrNull()?.toCurrent(customerId),
            snapshots = snapshots,
            changes = changes(customerId),
        )
    }

    private fun CdoSnapshot.toCurrent(customerId: Int): AuditedCustomer? {
        val name = if (type == SnapshotType.TERMINAL) null else getPropertyValue("name") as? String
        val email = if (type == SnapshotType.TERMINAL) null else getPropertyValue("email") as? String
        return if (name != null && email != null) {
            AuditedCustomer(id = customerId, name = name, email = email)
        } else {
            null
        }
    }
}

private val subscriptionLock = Any()
private var activeSubscriptionDatabase: Database? = null
private val javersDatabaseBindings = WeakHashMap<Javers, Database>()

/** 전역 Exposed hook의 소유권과 lifecycle을 감싸는 예제용 구독 핸들입니다. */
class AuditSubscription internal constructor(
    private val database: Database,
    private val delegate: ExposedJaversEntityHookSubscription,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    /** 구독을 한 번만 닫고 전역 hook 소유권을 반환합니다. */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            synchronized(subscriptionLock) {
                try {
                    delegate.close()
                } finally {
                    if (activeSubscriptionDatabase === database) {
                        activeSubscriptionDatabase = null
                    }
                }
            }
        }
    }
}

/** H2/JDBC 예제에 사용할 JaVers 인스턴스를 생성하고 저장소 스키마를 보장합니다. */
fun createJavers(database: Database): Javers {
    val repository = ExposedCdoSnapshotRepository(database)
    repository.ensureSchema()
    val javers = JaversBuilder.javers()
        .registerJaversRepository(repository)
        .build()
    synchronized(javersDatabaseBindings) {
        javersDatabaseBindings[javers] = database
    }
    return javers
}

/**
 * 지정한 JDBC [database]의 고객 DAO lifecycle만 JaVers에 연결하고 요청 메타데이터를 기록합니다.
 *
 * provider의 hook은 전역이므로 다른 [Database]에서 같은 DAO를 변경하면 감사 저장소 오염을
 * 막기 위해 즉시 실패합니다. 이 예제는 전역 hook 소유권을 하나의 [Database]에만 허용하며,
 * 반환된 subscription을 애플리케이션 lifecycle에서 `try/finally` 또는 `.use`로 닫아야 합니다.
 */
fun subscribeAudit(
    database: Database,
    javers: Javers,
): AuditSubscription {
    synchronized(javersDatabaseBindings) {
        check(javersDatabaseBindings[javers] === database) {
            "JaVers instance and audit subscription must use the same Database"
        }
    }
    val delegate = synchronized(subscriptionLock) {
        check(activeSubscriptionDatabase == null) {
            "Only one JaVers audit subscription may be active for the global Exposed EntityHook"
        }
        activeSubscriptionDatabase = database
        runCatching {
            ExposedJaversEntityHookSubscription.subscribe(
                javers = javers,
                mappings = listOf(
                    ExposedJaversEntityHookMapping.of(CustomerEntity) { entity ->
                        entity.toAuditObject()
                    },
                ),
                authorProvider = {
                    requireCurrentDatabase(database)
                    AuditContextHolder.requireCurrent().actor
                },
                commitPropertiesProvider = { change ->
                    requireCurrentDatabase(database)
                    val context = AuditContextHolder.requireCurrent()
                    mapOf(
                        "requestId" to context.requestId,
                        "changeType" to change.changeType.name,
                    )
                },
            )
        }.onFailure {
            activeSubscriptionDatabase = null
        }.getOrThrow()
    }

    return AuditSubscription(database, delegate)
}

private fun requireCurrentDatabase(expected: Database) {
    val current = TransactionManager.current().db
    check(current === expected) {
        "JaVers audit subscription is bound to a different Database"
    }
}
