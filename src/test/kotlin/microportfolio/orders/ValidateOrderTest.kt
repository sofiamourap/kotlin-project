package microportfolio.orders

import microportfolio.domain.OrderSide
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidateOrderTest {

    @Test
    fun `unknown symbol is rejected`() {
        val result = validateOrder("DOGE", "BUY", "1", quotePrice = null, heldQuantity = BigDecimal.ZERO)
        assertIs<OrderValidation.Invalid>(result)
        assertEquals("Unknown symbol", result.message)
    }

    @Test
    fun `sell without enough holdings is rejected`() {
        val result = validateOrder(
            symbol = "BTC",
            side = "SELL",
            quantity = "1",
            quotePrice = BigDecimal("65000"),
            heldQuantity = BigDecimal("0.5"),
        )
        assertIs<OrderValidation.Invalid>(result)
        assertEquals("Insufficient holdings", result.message)
    }

    @Test
    fun `valid buy snapshots the quote price`() {
        val result = validateOrder(
            symbol = "btc",
            side = "buy",
            quantity = "0.5",
            quotePrice = BigDecimal("65000"),
            heldQuantity = BigDecimal.ZERO,
        )
        val valid = assertIs<OrderValidation.Valid>(result)
        assertEquals("BTC", valid.symbol)
        assertEquals(OrderSide.BUY, valid.side)
        assertEquals(BigDecimal("0.5"), valid.quantity)
        assertEquals(BigDecimal("65000"), valid.price)
    }
}
