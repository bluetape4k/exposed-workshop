configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.jetbrains.exposed.bom))

    testImplementation(project(":exposed-shared-tests"))

    testImplementation(libs.jetbrains.exposed.core)
    testImplementation(libs.jetbrains.exposed.jdbc)
    testImplementation(libs.jetbrains.exposed.dao)
    testImplementation(libs.exposed.core)

    implementation(libs.bluetape4k.io)

    // Compression
    testRuntimeOnly(libs.lz4.java)
    testRuntimeOnly(libs.snappy.java)
    testRuntimeOnly(libs.zstd.jni)

    // Serialization
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

    // Jdbc Drivers
    testRuntimeOnly(libs.h2.v2)
    testRuntimeOnly(libs.mariadb.java.client)
    testRuntimeOnly(libs.mysql.connector.j)
    testRuntimeOnly(libs.postgresql.driver)
    testRuntimeOnly(libs.pgjdbc.ng)

    // Coroutines
    testImplementation(libs.bluetape4k.coroutines)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
