package microportfolio.orders

import microportfolio.domain.OrderSide
import java.math.BigDecimal
import java.math.RoundingMode

data class HoldingState(
    val quantity: BigDecimal,
    val averageBuyPrice: BigDecimal,
)

sealed class HoldingDecision {
    data class Upsert(val holding: HoldingState) : HoldingDecision()
    data object Remove : HoldingDecision()
    data object Insufficient : HoldingDecision()
}

/** Pure BUY/SELL math. No DB. */
fun nextHolding(
    current: HoldingState?,
    side: OrderSide,
    quantity: BigDecimal,
    price: BigDecimal,
): HoldingDecision =
    when (side) {
        OrderSide.BUY -> {
            if (current == null) {
                HoldingDecision.Upsert(HoldingState(quantity, price))
            } else {
                val newQty = current.quantity + quantity
                val newAvg = (current.quantity * current.averageBuyPrice + quantity * price)
                    .divide(newQty, 8, RoundingMode.HALF_UP)
                HoldingDecision.Upsert(HoldingState(newQty, newAvg))
            }
        }
        OrderSide.SELL -> when {
            current == null || current.quantity < quantity -> HoldingDecision.Insufficient
            current.quantity.compareTo(quantity) == 0 -> HoldingDecision.Remove
            else -> HoldingDecision.Upsert(
                HoldingState(current.quantity - quantity, current.averageBuyPrice),
            )
        }
    }
