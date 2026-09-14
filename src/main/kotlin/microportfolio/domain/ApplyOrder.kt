package microportfolio.domain

import microportfolio.kafka.OrderPlaced
import microportfolio.orders.HoldingDecision
import microportfolio.orders.HoldingState
import microportfolio.orders.nextHolding
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.uuid.Uuid

/** Apply one OrderPlaced event. Safe to run twice: already FILLED is a no-op. */
fun applyOrderPlaced(event: OrderPlaced) {
    val orderId = Uuid.parse(event.orderId)
    val userId = Uuid.parse(event.userId)
    val quantity = BigDecimal(event.quantity)
    val price = BigDecimal(event.price)
    val side = OrderSide.valueOf(event.side)

    transaction {
        val order = Orders
            .selectAll()
            .where { Orders.id eq EntityID(orderId, Orders) }
            .firstOrNull()
            ?: return@transaction

        if (order[Orders.status] == OrderStatus.FILLED.name) {
            return@transaction
        }

        val existing = Portfolios
            .selectAll()
            .where {
                (Portfolios.userId eq EntityID(userId, Users)) and
                    (Portfolios.assetSymbol eq event.symbol)
            }
            .firstOrNull()

        val current = existing?.let {
            HoldingState(it[Portfolios.quantity], it[Portfolios.averageBuyPrice])
        }

        when (val decision = nextHolding(current, side, quantity, price)) {
            is HoldingDecision.Insufficient -> {
                setOrderStatus(orderId, OrderStatus.REJECTED)
            }
            is HoldingDecision.Remove -> {
                Portfolios.deleteWhere {
                    (Portfolios.userId eq EntityID(userId, Users)) and
                        (Portfolios.assetSymbol eq event.symbol)
                }
                setOrderStatus(orderId, OrderStatus.FILLED)
            }
            is HoldingDecision.Upsert -> {
                if (existing == null) {
                    Portfolios.insert { row ->
                        row[Portfolios.userId] = EntityID(userId, Users)
                        row[assetSymbol] = event.symbol
                        row[Portfolios.quantity] = decision.holding.quantity
                        row[averageBuyPrice] = decision.holding.averageBuyPrice
                    }
                } else {
                    Portfolios.update({
                        (Portfolios.userId eq EntityID(userId, Users)) and
                            (Portfolios.assetSymbol eq event.symbol)
                    }) { row ->
                        row[Portfolios.quantity] = decision.holding.quantity
                        row[averageBuyPrice] = decision.holding.averageBuyPrice
                        row[updatedAt] = LocalDateTime.now()
                    }
                }
                setOrderStatus(orderId, OrderStatus.FILLED)
            }
        }
    }
}

private fun setOrderStatus(orderId: Uuid, status: OrderStatus) {
    Orders.update({ Orders.id eq EntityID(orderId, Orders) }) { row ->
        row[Orders.status] = status.name
        row[updatedAt] = LocalDateTime.now()
    }
}
