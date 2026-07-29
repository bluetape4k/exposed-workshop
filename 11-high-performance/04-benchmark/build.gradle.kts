import groovy.json.JsonSlurper
import java.time.Instant

plugins {
    alias(libs.plugins.exposed)
    kotlin("plugin.allopen")
    alias(libs.plugins.kotlinx.benchmark)
}

exposed {
    migrations {
        tablesPackage = "exposed.examples.benchmark"
        databaseUrl = "jdbc:h2:mem:11-high-performance-04-benchmark-migrations;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
        databaseUser = "sa"
        databasePassword = ""
    }
}

dependencies {
    implementation(project(":03-routing-datasource"))

    implementation(libs.caffeine)
    implementation(libs.kotlinx.benchmark.runtime)
    implementation(libs.kotlinx.benchmark.runtime.jvm)
    implementation(libs.jmh.core)

    // Exposed ORM 및 DSL 참조 라이브러리
    implementation(libs.jetbrains.exposed.core)
    implementation(libs.jetbrains.exposed.jdbc)
    implementation(libs.jetbrains.exposed.dao)
    implementation(libs.jetbrains.exposed.java.time)

    // JPA/Hibernate 벤치마크 비교 참조 라이브러리
    implementation(libs.jakarta.persistence.api)
    implementation(libs.hibernate.core)

    // 벤치마크 데이터베이스 실행 구성 요소
    implementation(libs.h2.v2)
    implementation(libs.hikaricp)
    implementation(libs.postgresql.driver)

    // 벤치마크용 컨테이너 실행 구성 요소
    implementation(libs.bluetape4k.testcontainers)
    implementation(libs.testcontainers.postgresql)
}

allOpen {
    annotation("org.openjdk.jmh.annotations.State")
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
}

benchmark {
    targets {
        register("main")
    }

    configurations {
        named("main") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            mode = "avgt"
            outputTimeUnit = "us"
            reportFormat = "json"
        }

        register("smoke") {
            warmups = 2
            iterations = 3
            iterationTime = 300
            iterationTimeUnit = "ms"
            mode = "avgt"
            outputTimeUnit = "us"
            reportFormat = "json"
        }
    }
}

abstract class BenchmarkMarkdownReportTask: DefaultTask() {

    @get:Input
    abstract val profile: Property<String>

    @get:InputDirectory
    abstract val reportsRoot: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun writeMarkdown() {
        @Suppress("UNCHECKED_CAST")
        fun Any?.asStringAnyMap(): Map<String, Any?>? = this as? Map<String, Any?>

        val reportsDir = reportsRoot.get().asFile
        val jsonFile = reportsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .maxByOrNull { it.lastModified() }
            ?: error("Benchmark JSON report not found under ${reportsDir.absolutePath}. Run the benchmark task first.")

        val rows = (JsonSlurper().parse(jsonFile) as? List<*>)?.mapNotNull { element ->
            element.asStringAnyMap()
        }.orEmpty()

        val markdown = buildString {
            appendLine("# Benchmark Report")
            appendLine()
            appendLine("- Profile: `${profile.get()}`")
            appendLine("- Generated At: `${Instant.now()}`")
            appendLine("- Source JSON: `${jsonFile.absolutePath}`")
            appendLine()
            appendLine("| Benchmark | Mode | Params | Score | Error | Unit |")
            appendLine("| --- | --- | --- | ---: | ---: | --- |")

            rows.sortedBy { it["benchmark"].toString() }.forEach { row ->
                val params = row["params"].asStringAnyMap()
                val metric = row["primaryMetric"].asStringAnyMap()
                val benchmarkName = row["benchmark"].toString().substringAfterLast('.')
                val mode = row["mode"]?.toString().orEmpty()
                val paramsText = params?.entries
                    ?.sortedBy { it.key }
                    ?.joinToString("<br/>") { (key, value) -> "$key=$value" }
                    ?.ifBlank { "-" }
                    ?: "-"
                val score = (metric?.get("score") as? Number)?.let { "%.3f".format(it.toDouble()) } ?: "-"
                val error = (metric?.get("scoreError") as? Number)?.let { "%.3f".format(it.toDouble()) } ?: "-"
                val unit = metric?.get("scoreUnit")?.toString().orEmpty().ifBlank { "-" }

                appendLine("| $benchmarkName | $mode | $paramsText | $score | $error | $unit |")
            }
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(markdown)
        }

        logger.lifecycle("Wrote benchmark markdown report to ${outputFile.get().asFile}")
    }
}

val benchmarkProfile = providers.gradleProperty("benchmarkProfile").orElse("main")

tasks.register<BenchmarkMarkdownReportTask>("benchmarkMarkdown") {
    group = "benchmark"
    description = "Run benchmarks and write the latest JSON result as Markdown."
    profile.set(benchmarkProfile)
    reportsRoot.set(layout.buildDirectory.dir(benchmarkProfile.map { "reports/benchmarks/$it" }))
    outputFile.set(layout.buildDirectory.file(benchmarkProfile.map { "reports/benchmarks/$it/benchmark-report.md" }))

    dependsOn(benchmarkProfile.map { profileName ->
        when (profileName) {
            "main" -> "benchmark"
            "smoke" -> "smokeBenchmark"
            else -> error("Unsupported benchmarkProfile=$profileName. Use main or smoke.")
        }
    })
}
