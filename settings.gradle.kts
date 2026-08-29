pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        // Second catalog, published by the Ktor team: every `ktorLibs.*` alias
        // resolves against this single Ktor version.
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.5.2") // keep in sync with libs.versions.toml -> ktor
    }
}

rootProject.name = "micro-portfolio"
