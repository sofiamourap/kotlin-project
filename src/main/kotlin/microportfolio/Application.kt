package microportfolio

import microportfolio.plugins.configureDatabase
import microportfolio.plugins.configureSecurity
import io.ktor.server.application.Application

/**
 * The single application module referenced from application.yaml.
 * Order matters: security must be installed before routing declares
 * authenticate("auth-jwt").
 */
fun Application.module() {
    configureDatabase()
    configureSerialization()
    configureStatusPages()
    configureSecurity()
    configureRouting()
}
