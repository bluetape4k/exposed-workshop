package exposed.examples.benchmark.crud.setup

import jakarta.persistence.SharedCacheMode
import jakarta.persistence.ValidationMode
import jakarta.persistence.spi.ClassTransformer
import jakarta.persistence.spi.PersistenceUnitInfo
import jakarta.persistence.spi.PersistenceUnitTransactionType
import java.net.URL
import javax.sql.DataSource

/**
 * Spring 없이 프로그래밍 방식으로 JPA를 설정하기 위한 PersistenceUnitInfo 구현체입니다.
 */
class PersistenceUnitInfoImpl(
    private val persistenceUnitName: String,
    private val managedClassNames: List<String>,
    private val dataSource: DataSource,
): PersistenceUnitInfo {

    override fun getPersistenceUnitName(): String = persistenceUnitName
    override fun getPersistenceProviderClassName(): String = "org.hibernate.jpa.HibernatePersistenceProvider"
    override fun getTransactionType(): PersistenceUnitTransactionType = PersistenceUnitTransactionType.RESOURCE_LOCAL
    override fun getJtaDataSource(): DataSource? = null
    override fun getNonJtaDataSource(): DataSource = dataSource
    override fun getMappingFileNames(): MutableList<String> = mutableListOf()
    override fun getJarFileUrls(): MutableList<URL> = mutableListOf()
    override fun getPersistenceUnitRootUrl(): URL? = null
    override fun getManagedClassNames(): MutableList<String> = managedClassNames.toMutableList()
    override fun excludeUnlistedClasses(): Boolean = true
    override fun getSharedCacheMode(): SharedCacheMode = SharedCacheMode.UNSPECIFIED
    override fun getValidationMode(): ValidationMode = ValidationMode.NONE
    override fun getProperties(): java.util.Properties = java.util.Properties()
    override fun getPersistenceXMLSchemaVersion(): String = "3.0"
    override fun getClassLoader(): ClassLoader = Thread.currentThread().contextClassLoader
    override fun addTransformer(transformer: ClassTransformer?) {}
    override fun getNewTempClassLoader(): ClassLoader? = null
}
