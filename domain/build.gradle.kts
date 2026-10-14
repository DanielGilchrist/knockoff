plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.pitest)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.timeout.default", "10s")
}

pitest {
    pitestVersion = libs.versions.pitest
    junit5PluginVersion = libs.versions.pitestJunit5
    targetClasses = setOf("dev.danielgilchrist.knockoff.domain.*")
    mutators = setOf("STRONGER", "-VOID_METHOD_CALLS")
    threads = 4
    outputFormats = setOf("HTML", "CSV")
    timestampedReports = false
}

tasks.named("pitest") {
    outputs.cacheIf("Reports restored from the build cache have been seen to come from another source tree") { false }
}

val verifyMutants by tasks.registering {
    val report = layout.buildDirectory.file("reports/pitest/mutations.csv")
    val accepted = rootProject.layout.projectDirectory.file("config/pitest/accepted-survivors.txt")
    val classes = layout.buildDirectory.dir("classes/kotlin/main")
    val sources = layout.projectDirectory.dir("src/main/kotlin")
    dependsOn(tasks.named("pitest"))

    doLast {
        val inlinedCallSites = mutableMapOf<String, Map<Int, Int>>()

        fun callSiteLine(
            className: String,
            line: Int,
        ): Int {
            val callSites =
                inlinedCallSites.getOrPut(className) {
                    val classFile = classes.get().file(className.replace('.', '/') + ".class").asFile
                    val debug =
                        classFile
                            .readText(Charsets.ISO_8859_1)
                            .substringAfter("*S KotlinDebug", "")
                            .substringAfter("*L\n", "")
                            .substringBefore("*E")
                    Regex("""(\d+)#1:(\d+)(?:,(\d+))?""")
                        .findAll(debug)
                        .flatMap { match ->
                            val (callSite, start, count) = match.destructured
                            (0 until (count.toIntOrNull() ?: 1)).map { start.toInt() + it to callSite.toInt() }
                        }.toMap()
                }
            return callSites[line] ?: line
        }

        fun isDomainSource(mutant: List<String>): Boolean {
            val packagePath = mutant[1].substringBeforeLast('.').replace('.', '/')
            return sources.file("$packagePath/${mutant[0]}").asFile.exists()
        }

        fun key(mutant: List<String>): String {
            val line = callSiteLine(mutant[1], mutant[4].toInt())
            return "${mutant[5]} ${mutant[1]} ${mutant[3]} L$line ${mutant[2].substringAfterLast('.')}"
        }

        val mutants =
            report
                .get()
                .asFile
                .readLines()
                .map { it.split(",") }
        val failedToRun = mutants.filter { it[5] in setOf("RUN_ERROR", "MEMORY_ERROR") }
        val undetected = mutants.filter { it[5] in setOf("SURVIVED", "NO_COVERAGE") }
        val outsideDomain = undetected.filterNot(::isDomainSource)
        val problems =
            failedToRun.map { "  PIT could not run: ${it.joinToString(",")}" } +
                outsideDomain.map { "  undetected outside domain sources, must be killed: ${it.joinToString(",")}" }

        if (problems.isNotEmpty()) {
            throw GradleException((listOf("Mutation testing failed") + problems).joinToString("\n"))
        }

        val survivors = undetected.map(::key).groupingBy { it }.eachCount()
        val expected =
            accepted.asFile
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .groupingBy { it }
                .eachCount()
        val differences =
            (survivors.keys + expected.keys)
                .sorted()
                .filter { survivors[it] != expected[it] }
                .map { "  $it: ${survivors[it] ?: 0} surviving, ${expected[it] ?: 0} accepted" }

        if (differences.isNotEmpty()) {
            throw GradleException(
                (listOf("Mutation survivors differ from config/pitest/accepted-survivors.txt") + differences)
                    .joinToString("\n"),
            )
        }
    }
}

tasks.named("check") {
    dependsOn(verifyMutants)
}
