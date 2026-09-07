dependencies {
    api(libs.hikaricp)
    api(libs.exposed.tenant.jdbc.snapshot)

    testImplementation(libs.kotlin.test.junit5)
    testRuntimeOnly(libs.h2.v2)
}
