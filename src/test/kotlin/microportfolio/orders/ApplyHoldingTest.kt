package microportfolio.orders

import microportfolio.domain.OrderSide
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApplyHoldingTest {

    @Test
    fun `first buy creates a holding at the quote price`() {
        val result = nextHolding(null, OrderSide.BUY, BigDecimal("0.5"), BigDecimal("65000"))
        val upsert = assertIs<HoldingDecision.Upsert>(result)
        assertEquals(BigDecimal("0.5"), upsert.holding.quantity)
        assertEquals(BigDecimal("65000"), upsert.holding.averageBuyPrice)
    }

    @Test
    fun `second buy averages the price`() {
        val current = HoldingState(BigDecimal("1"), BigDecimal("100"))
        val result = nextHolding(current, OrderSide.BUY, BigDecimal("1"), BigDecimal("200"))
        val upsert = assertIs<HoldingDecision.Upsert>(result)
        assertEquals(BigDecimal("2"), upsert.holding.quantity)
        assertEquals(BigDecimal("150.00000000"), upsert.holding.averageBuyPrice)
    }

    @Test
    fun `sell reduces quantity and keeps the average`() {
        val current = HoldingState(BigDecimal("1"), BigDecimal("100"))
        val result = nextHolding(current, OrderSide.SELL, BigDecimal("0.4"), BigDecimal("180"))
        val upsert = assertIs<HoldingDecision.Upsert>(result)
        assertEquals(BigDecimal("0.6"), upsert.holding.quantity)
        assertEquals(BigDecimal("100"), upsert.holding.averageBuyPrice)
    }

    @Test
    fun `sell of the full holding removes it`() {
        val current = HoldingState(BigDecimal("1"), BigDecimal("100"))
        val result = nextHolding(current, OrderSide.SELL, BigDecimal("1"), BigDecimal("180"))
        assertIs<HoldingDecision.Remove>(result)
    }

    @Test
    fun `sell without enough quantity is insufficient`() {
        val current = HoldingState(BigDecimal("0.2"), BigDecimal("100"))
        val result = nextHolding(current, OrderSide.SELL, BigDecimal("1"), BigDecimal("180"))
        assertIs<HoldingDecision.Insufficient>(result)
    }
}
