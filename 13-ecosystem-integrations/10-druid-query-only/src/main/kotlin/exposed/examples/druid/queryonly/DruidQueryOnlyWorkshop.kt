package exposed.examples.druid.queryonly

import io.bluetape4k.exposed.druid.DruidAvaticaSerialization
import io.bluetape4k.exposed.druid.DruidColumnMetadata
import io.bluetape4k.exposed.druid.DruidConnectionOptions
import io.bluetape4k.exposed.druid.DruidJdbc
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.Serializable

private const val DEFAULT_DRUID_AVATICA_ENDPOINT = "http://localhost:8888/druid/v2/sql/avatica/"
private val DRUID_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

/** Apache Druid query-only 예제에서 사용하는 검증된 연결 profile이다. */
data class DruidQueryProfile(
    val avaticaEndpoint: String = DEFAULT_DRUID_AVATICA_ENDPOINT,
    val datasource: String = "wikipedia",
    val schema: String = "druid",
    val transparentReconnection: Boolean = true,
    val serialization: DruidAvaticaSerialization = DruidAvaticaSerialization.JSON,
    val user: String? = null,
    val password: String? = null,
    val contextProperties: Map<String, String> = mapOf("sqlTimeZone" to "Etc/UTC"),
) : Serializable {

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    init {
        datasource.requireNotBlank("datasource")
        schema.requireNotBlank("schema")
        user?.requireNotBlank("user")
        password?.requireNotBlank("password")
        contextProperties.forEach { (key, value) ->
            key.requireNotBlank("contextProperties key")
            value.requireNotBlank("contextProperties value for '$key'")
        }
    }

    /** profile을 provider의 `DruidConnectionOptions`로 변환한다. */
    fun toConnectionOptions(): DruidConnectionOptions =
        DruidConnectionOptions(
            avaticaEndpoint = avaticaEndpoint,
            transparentReconnection = transparentReconnection,
            serialization = serialization,
            user = user,
            password = password,
            contextProperties = contextProperties,
        )
}

/** 기본 local Avatica endpoint를 사용하는 Druid query profile을 반환한다. */
fun defaultDruidQueryProfile(): DruidQueryProfile = DruidQueryProfile()

/** Druid datasource count 조회에 사용하는 읽기 전용 SQL을 생성한다. */
internal fun buildDatasourceCountQuery(datasource: String): String {
    datasource.requireNotBlank("datasource")
    require(DRUID_IDENTIFIER.matches(datasource)) {
        "datasource must be a simple Druid identifier: $datasource"
    }
    return "SELECT COUNT(*) AS row_count FROM \"$datasource\""
}

/** Druid datasource의 row count를 동기 query로 조회한다. */
fun queryDatasourceRowCount(profile: DruidQueryProfile): List<Long> =
    DruidJdbc.query(
        sql = buildDatasourceCountQuery(profile.datasource),
        options = profile.toConnectionOptions(),
    ) { resultSet -> resultSet.getLong("row_count") }

/** Druid datasource의 row count를 지정한 dispatcher에서 suspend query로 조회한다. */
suspend fun queryDatasourceRowCountSuspend(
    profile: DruidQueryProfile,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): List<Long> =
    DruidJdbc.querySuspend(
        sql = buildDatasourceCountQuery(profile.datasource),
        options = profile.toConnectionOptions(),
        dispatcher = dispatcher,
    ) { resultSet -> resultSet.getLong("row_count") }

/** Druid `INFORMATION_SCHEMA.COLUMNS`에서 profile datasource의 column metadata를 조회한다. */
fun listDatasourceColumns(profile: DruidQueryProfile): List<DruidColumnMetadata> =
    DruidJdbc.listColumns(
        datasource = profile.datasource,
        schema = profile.schema,
        options = profile.toConnectionOptions(),
    )
