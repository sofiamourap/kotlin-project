package microportfolio

import io.ktor.server.application.Application
import microportfolio.kafka.KafkaOrderEventPublisher
import microportfolio.kafka.startOrderPlacedConsumer
import microportfolio.plugins.configureDatabase
import microportfolio.plugins.configureSecurity

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
    val publisher = KafkaOrderEventPublisher.from(environment.config, this)
    configureRouting(publisher)
    startOrderPlacedConsumer() // start the consumer for the order placed event
}
