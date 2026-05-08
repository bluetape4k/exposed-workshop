import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    base
    // jacoco
    alias(libs.plugins.kotlin.jvm)

    // see: https://kotlinlang.org/docs/reference/compiler-plugins.html
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.allopen) apply false
    alias(libs.plugins.kotlin.noarg) apply false
    alias(libs.plugins.kotlin.jpa) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlinx.atomicfu)
    alias(libs.plugins.kotlin.kapt) apply false

    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)

    alias(libs.plugins.dependency.management)
    alias(libs.plugins.spring.boot) apply false

    alias(libs.plugins.test.logger)
    alias(libs.plugins.kotlinx.benchmark) apply false
    alias(libs.plugins.graalvm.native) apply false
}

val rootLibs = libs

allprojects {
    repositories {
        mavenCentral()
        google()

        // bluetape4k-assertions 는 1.8.0-SNAPSHOT 에만 존재하므로 snapshot 저장소 사용
        maven {
            name = "central-snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }

    // bluetape4k snapshot 버전 사용 시만 사용하세요.
//    configurations.all {
//        resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.DAYS)
//    }
}

subprojects {
    apply {
        plugin<JavaLibraryPlugin>()
        // Kotlin 1.9.20 부터는 pluginId 를 지정해줘야 합니다.
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlinx.atomicfu")
        plugin("io.spring.dependency-management")
        plugin("com.adarshr.test-logger")
        plugin("org.jetbrains.kotlinx.kover")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    kotlin {
        jvmToolchain(21)
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_3)
            apiVersion.set(KotlinVersion.KOTLIN_2_3)
            freeCompilerArgs = listOf(
                "-Xjsr305=strict",
                "-jvm-default=enable",
                // "-Xinline-classes",          // Kotlin 2.3 부터는 필요 없음
                "-Xstring-concat=indy",         // since Kotlin 1.4.20 for JVM 9+
                "-Xcontext-parameters",         // since Kotlin 1.6
                "-Xannotation-default-target=param-property"
            )
            val experimentalAnnotations = listOf(
                "kotlin.RequiresOptIn",
                "kotlin.ExperimentalStdlibApi",
                "kotlin.contracts.ExperimentalContracts",
                "kotlin.experimental.ExperimentalTypeInference",
                "kotlinx.coroutines.ExperimentalCoroutinesApi",
                "kotlinx.coroutines.InternalCoroutinesApi",
                "kotlinx.coroutines.FlowPreview",
                "kotlinx.coroutines.DelicateCoroutinesApi",
            )
            freeCompilerArgs.addAll(experimentalAnnotations.map { "-opt-in=$it" })
        }
    }

    atomicfu {
        transformJvm = true
        jvmVariant = "VH"     //  FU, VH, BOTH
    }

    tasks {
        compileJava {
            options.isIncremental = true
        }

        compileKotlin {
            compilerOptions {
                incremental = true
            }
        }

        // 멀티 모듈들을 테스트 시에만 동시에 실행되지 않게 하기 위해 Mutex 를 활용합니다.
        abstract class TestMutexService: BuildService<BuildServiceParameters.None>

        val testMutex = gradle.sharedServices.registerIfAbsent(
            "test-mutex",
            TestMutexService::class
        ) {
            maxParallelUsages.set(1)
        }

        test {
            // 멀티 모듈들을 테스트 시에만 동시에 실행되지 않게 하기 위해 Mutex 를 활용합니다.
            usesService(testMutex)

            useJUnitPlatform()

            // 테스트 시 아래와 같은 예외 메시지를 제거하기 위해서
            // OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
            jvmArgs(
                "-Xshare:off",
                "-Xms2G",
                "-Xmx4G",
                "-XX:+UseG1GC",
                "-XX:+UnlockExperimentalVMOptions",
                "-XX:+EnableDynamicAgentLoading",
                "--enable-preview",
                "-Didea.io.use.nio2=true",
                "-Duser.language=en",
                "-Duser.country=US"
            )

            // Gradle 프로퍼티를 JVM 시스템 프로퍼티로 전달하여 테스트 대상 DB를 제어합니다.
            // 사용 예:
            //   ./gradlew test -PuseFastDB=true          → H2 만 테스트
            //   ./gradlew test -PuseDB=H2,POSTGRESQL      → 지정한 DB만 테스트
            project.findProperty("useFastDB")?.toString()?.let {
                systemProperty("exposed.test.useFastDB", it)
            }
            project.findProperty("useDB")?.toString()?.let {
                systemProperty("exposed.test.useDB", it)
            }

            testLogging {
                showExceptions = true
                showCauses = true
                showStackTraces = true

                events("failed")
            }
        }

        testlogger {
            theme = com.adarshr.gradle.testlogger.theme.ThemeType.MOCHA_PARALLEL
            showFullStackTraces = true
        }

        val reportMerge by registering(ReportMergeTask::class) {
            val file = rootProject.layout.buildDirectory.asFile.get().resolve("reports/detekt/exposed.xml")
            output.set(file)
        }
        withType<Detekt>().configureEach detekt@{
            enabled = this@subprojects.name !== "exposed-tests"
            finalizedBy(reportMerge)
            reportMerge.configure {
                input.from(this@detekt.xmlReportFile)
            }
        }

        clean {
            doLast {
                delete("./.project")
                delete("./out")
                delete("./bin")
            }
        }
    }

    dependencyManagement {
        // HINT: Gradle 빌드 시, detachedConfiguration 이 많이 발생하는데, setApplyMavenExclusions(false) 를 추가하면 속도가 개선됩니다.
        // https://discuss.gradle.org/t/what-is-detachedconfiguration-i-have-a-lots-of-them-for-each-subproject-and-resolving-them-takes-95-of-build-time/31595/6
        setApplyMavenExclusions(false)

        imports {
            mavenBom(rootLibs.spring.boot.dependencies.get().toString())
            mavenBom(rootLibs.bluetape4k.bom.get().toString())
            mavenBom(rootLibs.kotlinx.coroutines.bom.get().toString())
            mavenBom(rootLibs.kotlin.bom.get().toString())
        }
    }

    dependencies {
        val api by configurations
        val testApi by configurations
        val implementation by configurations
        val testImplementation by configurations

        val compileOnly by configurations
        val testCompileOnly by configurations
        val testRuntimeOnly by configurations

        implementation(rootLibs.kotlin.stdlib)
        implementation(rootLibs.kotlin.reflect)
        testImplementation(rootLibs.kotlin.test)
        testImplementation(rootLibs.kotlin.test.junit5)

        implementation(rootLibs.kotlinx.coroutines.core)
        implementation(rootLibs.kotlinx.atomicfu)

        // 개발 시에는 logback 이 검증하기에 더 좋고, Production에서 비동기 로깅은 log4j2 가 성능이 좋다고 합니다.
        implementation(rootLibs.slf4j.api)
        implementation(rootLibs.bluetape4k.logging)
        implementation(rootLibs.logback)

        // JUnit 5
        testImplementation(rootLibs.bluetape4k.junit5)
        testImplementation(rootLibs.junit.jupiter)
        testRuntimeOnly(rootLibs.junit.platform.engine)

        testImplementation(rootLibs.bluetape4k.assertions)
        testImplementation(rootLibs.mockk)
        testImplementation(rootLibs.awaitility.kotlin)

        // Property based test
        testImplementation(rootLibs.datafaker)
        testImplementation(rootLibs.random.beans)
    }
}

dependencies {
    subprojects
        .filter { it.name != "04-benchmark" && it.name != "exposed-shared-tests" }
        .forEach { kover(it) }
}

kover {
    reports {
        total {
            xml {
                onCheck = false
            }
            html {
                onCheck = false
            }
        }
    }
}
