package exposed.multitenant.onboarding.model

import java.io.Serializable

data class OnboardTenantCommand(
    val tenantId: String,
    val displayName: String,
    val failAfterSchemaCreation: Boolean = false,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

data class TenantRecord(
    val tenantId: String,
    val displayName: String,
    val schemaName: String,
    val status: TenantStatus,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

enum class TenantStatus {
    ACTIVE,
}
