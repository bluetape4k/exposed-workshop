package exposed.shared.tenant.jdbc

import io.bluetape4k.exposed.tenant.jdbc.TenantJdbcResourceRegistry

/**
 * Spring property map의 tenant key를 normalize하고, 중복·누락 설정을 fail-fast로 검증합니다.
 *
 * 같은 normalized key가 두 번 등장하면 마지막 값을 조용히 선택하지 않고
 * `IllegalArgumentException`을 던져 설정 오타와 datasource 혼동을 차단합니다.
 */
fun <K : Any> Map<String, TenantJdbcSettings>.normalizeTenantJdbcSettings(
    parseTenant: (String) -> K,
    expectedTenants: Iterable<K>,
    tenantName: (K) -> String = { it.toString() },
): Map<K, TenantJdbcSettings> {
    val normalized = LinkedHashMap<K, TenantJdbcSettings>()
    for ((rawKey, settings) in this) {
        val tenant = parseTenant(rawKey)
        require(!normalized.containsKey(tenant)) {
            "Duplicate tenant datasource configuration: ${tenantName(tenant)}"
        }
        normalized[tenant] = settings
    }

    val missing = expectedTenants.filterNot(normalized::containsKey)
    require(missing.isEmpty()) {
        "Missing tenant datasource configuration: ${missing.joinToString { tenantName(it) }}"
    }

    return normalized.toMap()
}

/**
 * normalized 설정을 provider registry로 연결하고 datasource ownership을 위임합니다.
 *
 * Spring binding은 호출자에 남겨 두되, normalize·missing 확인·설정 검증·Hikari
 * factory·disposer 연결은 이 non-Spring helper에서 동일하게 실행합니다.
 */
fun <K : Any> Map<String, TenantJdbcSettings>.toTenantJdbcResourceRegistry(
    parseTenant: (String) -> K,
    expectedTenants: Iterable<K>,
    tenantName: (K) -> String = { it.toString() },
    poolName: (K) -> String = { "tenant-${tenantName(it)}" },
): TenantJdbcResourceRegistry<K> {
    val expected = expectedTenants.toList()
    val configured = normalizeTenantJdbcSettings(
        parseTenant = parseTenant,
        expectedTenants = expected,
        tenantName = tenantName,
    )

    configured.forEach { (tenant, settings) ->
        settings.validate(poolName(tenant))
    }

    return TenantJdbcResourceRegistry.create(
        tenants = expected,
        dataSourceFactory = { tenant ->
            configured.getValue(tenant).toHikariDataSource(poolName(tenant))
        },
        disposeDataSource = { _, dataSource -> dataSource.close() },
    )
}
