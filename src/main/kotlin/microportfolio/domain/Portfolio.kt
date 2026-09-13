package microportfolio.domain

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.math.BigDecimal
import kotlin.uuid.Uuid

object Portfolios : UuidTable("portfolios") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val assetSymbol = varchar("asset_symbol", 10)
    val quantity = decimal("quantity", 19, 8)
    val averageBuyPrice = decimal("average_buy_price", 19, 8)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(userId, assetSymbol)
    }
}

@Serializable
data class HoldingResponse(
    val assetSymbol: String,
    val quantity: String,
    val averageBuyPrice: String,
)

/** Must be called inside a transaction. */
fun holdingQuantity(userId: Uuid, symbol: String): BigDecimal =
    Portfolios
        .selectAll()
        .where {
            (Portfolios.userId eq EntityID(userId, Users)) and
                (Portfolios.assetSymbol eq symbol)
        }
        .firstOrNull()
        ?.get(Portfolios.quantity)
        ?: BigDecimal.ZERO

/** Must be called inside a transaction. */
fun findHoldingsByUserId(userId: Uuid): List<HoldingResponse> =
    Portfolios
        .selectAll()
        .where { Portfolios.userId eq EntityID(userId, Users) }
        .map { row ->
            HoldingResponse(
                assetSymbol = row[Portfolios.assetSymbol],
                quantity = row[Portfolios.quantity].toPlainString(),
                averageBuyPrice = row[Portfolios.averageBuyPrice].toPlainString(),
            )
        }
