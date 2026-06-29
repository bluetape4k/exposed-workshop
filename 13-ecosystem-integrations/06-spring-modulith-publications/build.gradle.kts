plugins {
    alias(libs.plugins.kotlin.spring)
    application
}

application {
    mainClass.set("exposed.examples.spring.modulith.publications.SpringModulithPublicationApplicationKt")
}

configurations {
    testImplementation.get().extendsFrom(compileOnly.get(), runtimeOnly.get())
}

dependencies {
    implementation(platform(libs.jackson3.bom))
    implementation(platform(libs.spring.modulith.bom))

    implementation(libs.exposed.spring.modulith)
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.java.time)
    implementation(libs.jetbrains.exposed.spring7.transaction)

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.spring.modulith.events.api)
    implementation(libs.spring.modulith.events.core)
    implementation(libs.spring.modulith.events.jackson)
    implementation(libs.jackson3.module.kotlin)

    runtimeOnly(libs.h2.v2)

    testImplementation(libs.bluetape4k.junit5)
    testImplementation(libs.awaitility.kotlin)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "junit", module = "junit")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(module = "mockito-core")
    }
}
