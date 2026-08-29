package microportfolio.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.response.respond
import java.util.Date

/** Typed view over the `jwt` block of application.yaml. */
data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
) {
    companion object {
        fun from(config: ApplicationConfig): JwtSettings {
            val jwt = config.config("jwt")
            return JwtSettings(
                secret = jwt.property("secret").getString(),
                issuer = jwt.property("issuer").getString(),
                audience = jwt.property("audience").getString(),
                realm = jwt.property("realm").getString(),
            )
        }
    }
}

const val AUTH_JWT = "auth-jwt"

fun Application.configureSecurity() {
    val settings = JwtSettings.from(environment.config)

    authentication {
        jwt(AUTH_JWT) {
            realm = settings.realm
            verifier(
                JWT.require(Algorithm.HMAC256(settings.secret))
                    .withAudience(settings.audience)
                    .withIssuer(settings.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }
}

fun createToken(userId: String, settings: JwtSettings, ttlMillis: Long = 24 * 60 * 60 * 1000): String =
    JWT.create()
        .withAudience(settings.audience)
        .withIssuer(settings.issuer)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + ttlMillis))
        .sign(Algorithm.HMAC256(settings.secret))
