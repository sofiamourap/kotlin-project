package microportfolio.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import java.math.BigDecimal
import kotlin.uuid.Uuid

enum class OrderSide { BUY, SELL }

enum class OrderStatus { PENDING, FILLED, REJECTED }

object Orders : UuidTable("orders") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val symbol = varchar("symbol", 10)
    val side = varchar("side", 8)
    val quantity = decimal("quantity", 19, 8)
    val price = decimal("price", 19, 8)
    val status = varchar("status", 16)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}

@Serializable
data class PlaceOrderRequest(
    val symbol: String,
    val side: String,
    val quantity: String,
)

@Serializable
data class PlaceOrderResponse(
    val orderId: String,
    val status: String,
    val symbol: String,
    val side: String,
    val quantity: String,
    val price: String,
)

/** Must be called inside a transaction. */
fun insertPendingOrder(
    userId: Uuid,
    symbol: String,
    side: OrderSide,
    quantity: BigDecimal,
    price: BigDecimal,
): Uuid =
    Orders.insertAndGetId { row ->
        row[Orders.userId] = EntityID(userId, Users)
        row[Orders.symbol] = symbol
        row[Orders.side] = side.name
        row[Orders.quantity] = quantity
        row[Orders.price] = price
        row[Orders.status] = OrderStatus.PENDING.name
    }.value
