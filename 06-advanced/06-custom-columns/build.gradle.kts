configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(libs.jetbrains.exposed.core)
    testImplementation(libs.jetbrains.exposed.jdbc)
    testImplementation(libs.jetbrains.exposed.dao)
    testImplementation(libs.exposed.core)

    implementation(libs.bluetape4k.io)

    // 압축 컬럼 타입 예제를 위한 압축 의존성
    testRuntimeOnly(libs.lz4.java)
    testRuntimeOnly(libs.snappy.java)
    testRuntimeOnly(libs.zstd.jni)

    // 사용자 정의 컬럼 직렬화를 위한 Kotlin Serialization 의존성
    testRuntimeOnly(libs.kryo)
    testRuntimeOnly(libs.fory.kotlin)

    // Identifier 자동 생성
    testRuntimeOnly(libs.bluetape4k.idgenerators)
    testRuntimeOnly(libs.java.uuid.generator)

    testImplementation(libs.bluetape4k.junit5)

    testImplementation(libs.bluetape4k.testcontainers)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.mariadb)
    testImplementation(libs.testcontainers.mysql)
    testImplementation(libs.testcontainers.postgresql)

    // 테스트 데이터베이스별 JDBC 드라이버 의존성
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)

    // 코루틴 기반 트랜잭션 예제 의존성
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
