package microportfolio

import microportfolio.plugins.AUTH_JWT
import microportfolio.plugins.JwtSettings
import microportfolio.plugins.createToken
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

fun Application.configureRouting() {
    val jwtSettings = JwtSettings.from(environment.config)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        post("/auth/register") {
            val request = call.receive<RegisterRequest>()
            // TODO: hash the password and insert a Users row before responding.
            call.respond(HttpStatusCode.Created, mapOf("email" to request.email))
        }

        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            // TODO: look the user up and verify the password hash.
            call.respond(LoginResponse(token = createToken(userId = request.email, settings = jwtSettings)))
        }

        authenticate(AUTH_JWT) {
            get("/portfolio") {
                val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
                call.respond(mapOf("userId" to userId))
            }
        }
    }
}
