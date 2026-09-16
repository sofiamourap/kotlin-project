package microportfolio

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.log
import io.ktor.server.plugins.callid.callId
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import microportfolio.domain.HoldingResponse
import microportfolio.domain.OrderStatus
import microportfolio.domain.PlaceOrderRequest
import microportfolio.domain.PlaceOrderResponse
import microportfolio.domain.Users
import microportfolio.domain.findHoldingsByUserId
import microportfolio.domain.holdingQuantity
import microportfolio.domain.insertPendingOrder
import microportfolio.kafka.NoOpOrderEventPublisher
import microportfolio.kafka.OrderEventPublisher
import microportfolio.kafka.OrderPlaced
import microportfolio.orders.OrderValidation
import microportfolio.orders.validateOrder
import microportfolio.plugins.AUTH_JWT
import microportfolio.plugins.JwtSettings
import microportfolio.plugins.createToken
import microportfolio.quotes.FakeQuoteService
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import kotlin.uuid.Uuid

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String)

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class RegisterResponse(val id: String, val email: String)

@Serializable
data class PortfolioResponse(val userId: String?, val holdings: List<HoldingResponse> = emptyList())

fun Application.configureRouting(publisher: OrderEventPublisher = NoOpOrderEventPublisher) {
    val jwtSettings = JwtSettings.from(environment.config)
    val quotes = FakeQuoteService()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        post("/auth/register") {
            val request = call.receive<RegisterRequest>()
            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email and password are required"))
                return@post
            }

            val existing = transaction {
                Users.selectAll().where { Users.email eq request.email }.count()
            }
            if (existing > 0) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
                return@post
            }

            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
            val userId = transaction {
                // it syntax: is the argument of the lambda function. should be used for simple functions with a single argument.
                Users.insertAndGetId {
                    it[email] = request.email
                    it[Users.passwordHash] = passwordHash
                }
            }

            call.respond(
                HttpStatusCode.Created,
                RegisterResponse(id = userId.value.toString(), email = request.email),
            )
        }

        post("/auth/login") {
            val request = call.receive<LoginRequest>()
            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Email and password are required"))
                return@post
            }

            val user = transaction {
                Users.selectAll().where { Users.email eq request.email }.firstOrNull()
            }
            if (user == null || !BCrypt.checkpw(request.password, user[Users.passwordHash])) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
                return@post
            }

            val token = createToken(
                userId = user[Users.id].value.toString(),
                settings = jwtSettings,
            )
            call.respond(LoginResponse(token = token))
        }

        authenticate(AUTH_JWT) {
            get("/portfolio") {
                val userId = jwtUserId(call) ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user id"))
                    return@get
                }

                val holdings = transaction { findHoldingsByUserId(userId) }
                call.respond(PortfolioResponse(userId = userId.toString(), holdings = holdings))
            }

            post("/orders") {
                val userId = jwtUserId(call) ?: run {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid user id"))
                    return@post
                }

                val request = call.receive<PlaceOrderRequest>()
                val quotePrice = quotes.priceFor(request.symbol)
                val heldQuantity = transaction { holdingQuantity(userId, request.symbol.trim().uppercase()) }

                when (val result = validateOrder(request.symbol, request.side, request.quantity, quotePrice, heldQuantity)) {
                    is OrderValidation.Invalid -> {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to result.message))
                    }
                    is OrderValidation.Valid -> {
                        val orderId = transaction {
                            insertPendingOrder(
                                userId = userId,
                                symbol = result.symbol,
                                side = result.side,
                                quantity = result.quantity,
                                price = result.price,
                            )
                        }
                        try {
                            publisher.publish(
                                OrderPlaced(
                                    orderId = orderId.toString(),
                                    userId = userId.toString(),
                                    symbol = result.symbol,
                                    side = result.side.name,
                                    quantity = result.quantity.toPlainString(),
                                    price = result.price.toPlainString(),
                                    requestId = call.callId.orEmpty(),
                                ),
                            )
                        } catch (e: Exception) {
                            call.application.log.error("Failed to publish OrderPlaced", e)
                            call.respond(
                                HttpStatusCode.ServiceUnavailable,
                                mapOf("error" to "Order saved but event could not be published"),
                            )
                            return@post
                        }
                        call.respond(
                            HttpStatusCode.Accepted,
                            PlaceOrderResponse(
                                orderId = orderId.toString(),
                                status = OrderStatus.PENDING.name,
                                symbol = result.symbol,
                                side = result.side.name,
                                quantity = result.quantity.toPlainString(),
                                price = result.price.toPlainString(),
                            ),
                        )
                    }
                }
            }
        }
    }
}

private fun jwtUserId(call: ApplicationCall): Uuid? {
    val raw = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()
    return raw?.let { runCatching { Uuid.parse(it) }.getOrNull() }
}
