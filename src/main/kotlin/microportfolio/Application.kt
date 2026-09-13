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
    configureSerialization() // app.use(json())
    configureStatusPages() // app.use(errorHandler)
    configureSecurity() // app.use(jwt) + jsonwebtoken.sign
    configureRouting() // app.get/post/put/delete/etc.
}
