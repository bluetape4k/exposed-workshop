package exposed.multitenant.onboarding.service

import exposed.multitenant.onboarding.model.OnboardTenantCommand
import exposed.multitenant.onboarding.model.TenantRecord
import exposed.multitenant.onboarding.model.TenantStatus
import exposed.multitenant.onboarding.persistence.TenantCatalog
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class TenantOnboardingService(
    private val database: Database,
) {

    fun initializeCatalog() {
        transaction(database) {
            SchemaUtils.create(TenantCatalog)
        }
    }

    fun onboard(command: OnboardTenantCommand): TenantRecord {
        val tenantId = normalizeTenantId(command.tenantId)
        val displayName = command.displayName.trim()
        require(displayName.isNotBlank()) { "displayName must not be blank" }

        val schemaName = schemaNameFor(tenantId)
        return transaction(database) {
            if (findById(tenantId) != null) {
                throw DuplicateTenantException(tenantId)
            }
            try {
                exec("CREATE SCHEMA $schemaName")
                exec(
                    """
                    CREATE TABLE $schemaName.PROVISIONED_MARKER (
                        ID INT PRIMARY KEY,
                        CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """.trimIndent(),
                )

                if (command.failAfterSchemaCreation) {
                    throw TenantProvisioningException("Simulated provisioning failure for $tenantId")
                }

                TenantCatalog.insert {
                    it[TenantCatalog.tenantId] = tenantId
                    it[TenantCatalog.displayName] = displayName
                    it[TenantCatalog.provisionedSchema] = schemaName
                    it[TenantCatalog.status] = TenantStatus.ACTIVE.name
                }
                TenantRecord(
                    tenantId = tenantId,
                    displayName = displayName,
                    schemaName = schemaName,
                    status = TenantStatus.ACTIVE,
                )
            } catch (e: TenantProvisioningException) {
                exec("DROP SCHEMA IF EXISTS $schemaName CASCADE")
                throw e
            } catch (e: RuntimeException) {
                exec("DROP SCHEMA IF EXISTS $schemaName CASCADE")
                throw e
            }
        }
    }

    fun find(tenantId: String): TenantRecord? =
        transaction(database) {
            findById(normalizeTenantId(tenantId))
        }

    fun schemaExists(schemaName: String): Boolean =
        transaction(database) {
            exec(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA
                 WHERE SCHEMA_NAME = '${schemaName.uppercase()}'
                """.trimIndent(),
            ) { rs ->
                rs.next()
                rs.getInt(1) > 0
            } ?: false
        }

    fun markerTableExists(schemaName: String): Boolean =
        transaction(database) {
            exec(
                """
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                 WHERE TABLE_SCHEMA = '${schemaName.uppercase()}'
                   AND TABLE_NAME = 'PROVISIONED_MARKER'
                """.trimIndent(),
            ) { rs ->
                rs.next()
                rs.getInt(1) > 0
            } ?: false
        }

    private fun findById(tenantId: String): TenantRecord? =
        TenantCatalog
            .selectAll()
            .where { TenantCatalog.tenantId eq tenantId }
            .singleOrNull()
            ?.let {
                TenantRecord(
                    tenantId = it[TenantCatalog.tenantId],
                    displayName = it[TenantCatalog.displayName],
                    schemaName = it[TenantCatalog.provisionedSchema],
                    status = TenantStatus.valueOf(it[TenantCatalog.status]),
                )
            }

    private fun normalizeTenantId(tenantId: String): String {
        val normalized = tenantId.trim().lowercase()
        require(tenantIdRegex.matches(normalized)) {
            "tenantId must contain lowercase letters, numbers, or hyphen"
        }
        return normalized
    }

    private fun schemaNameFor(tenantId: String): String =
        "TENANT_${tenantId.replace('-', '_').uppercase()}"

    companion object {
        private val tenantIdRegex = Regex("[a-z0-9][a-z0-9-]{1,38}[a-z0-9]")
    }
}

class DuplicateTenantException(
    tenantId: String,
) : IllegalArgumentException("Tenant already exists: $tenantId")

class TenantProvisioningException(
    message: String,
) : RuntimeException(message)
