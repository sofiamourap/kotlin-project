package microportfolio.orders

import microportfolio.domain.OrderSide
import java.math.BigDecimal

sealed class OrderValidation {
    data class Valid(
        val symbol: String,
        val side: OrderSide,
        val quantity: BigDecimal,
        val price: BigDecimal,
    ) : OrderValidation()

    data class Invalid(val message: String) : OrderValidation()
}

/**
 * Pure function: no DB, no HTTP. Same input always yields the same result.
 * [quotePrice] is null when the symbol is not in the fake market.
 */
fun validateOrder(
    symbol: String,
    side: String,
    quantity: String,
    quotePrice: BigDecimal?,
    heldQuantity: BigDecimal,
): OrderValidation {
    val normalizedSymbol = symbol.trim().uppercase()
    if (normalizedSymbol.isBlank() || quotePrice == null) {
        return OrderValidation.Invalid("Unknown symbol")
    }

    val parsedSide = runCatching { OrderSide.valueOf(side.trim().uppercase()) }.getOrNull()
        ?: return OrderValidation.Invalid("Side must be BUY or SELL")

    val qty = runCatching { BigDecimal(quantity.trim()) }.getOrNull()
        ?: return OrderValidation.Invalid("Quantity must be a number")
    if (qty <= BigDecimal.ZERO) {
        return OrderValidation.Invalid("Quantity must be greater than zero")
    }

    if (parsedSide == OrderSide.SELL && qty > heldQuantity) {
        return OrderValidation.Invalid("Insufficient holdings")
    }

    return OrderValidation.Valid(normalizedSymbol, parsedSide, qty, quotePrice)
}
