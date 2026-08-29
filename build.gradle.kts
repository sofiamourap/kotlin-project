plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

group = "microportfolio"
version = "1.0.0-SNAPSHOT"

application {
    // Your code has no main() of its own. EngineMain is Ktor's entry point:
    // it reads src/main/resources/application.yaml and starts the modules
    // listed there.
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // --- Ktor server ---------------------------------------------------
    // Every `ktorLibs.*` alias comes from the Ktor-published catalog wired up
    // in settings.gradle.kts, so all Ktor artifacts share one version. Never
    // hardcode a Ktor version next to these: mixing 2.x and 3.x is a runtime
    // NoSuchMethodError waiting to happen.
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.config.yaml)      // lets Ktor read application.yaml
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.serialization.kotlinx.json)

    // JWT support. Version comes from libs.versions.toml, which is kept equal
    // to the ktor-version-catalog version in settings.gradle.kts.
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.get()}")

    // --- Persistence ---------------------------------------------------
    implementation(libs.postgresql)   // JDBC driver
    implementation(libs.hikaricp)     // connection pool
    implementation(libs.exposed.core) // SQL DSL
    implementation(libs.exposed.jdbc)

    // --- Logging -------------------------------------------------------
    implementation(libs.logback.classic)

    // --- Test ----------------------------------------------------------
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
