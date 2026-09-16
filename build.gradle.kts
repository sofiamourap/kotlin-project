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
    implementation("io.ktor:ktor-server-call-id:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-call-logging:${libs.versions.ktor.get()}")

    // JWT support. Version comes from libs.versions.toml, which is kept equal
    // to the ktor-version-catalog version in settings.gradle.kts.
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.get()}")

    // BCrypt for password hashing.
    implementation("org.mindrot:jbcrypt:0.4")

    // --- Persistence ---------------------------------------------------
    implementation(libs.postgresql)   // JDBC driver
    implementation(libs.hikaricp)     // connection pool
    implementation(libs.exposed.core) // SQL DSL
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time) // datetime / CurrentDateTime columns
    implementation(libs.kafka.clients)

    // --- Logging -------------------------------------------------------
    implementation(libs.logback.classic)

    // --- Test ----------------------------------------------------------
    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
