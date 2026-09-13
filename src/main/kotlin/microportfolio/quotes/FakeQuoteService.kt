package microportfolio.quotes

import java.math.BigDecimal

/** In-memory fake market. Replace with a real feed later if you want. */
class FakeQuoteService(
    private val prices: Map<String, BigDecimal> = mapOf(
        "BTC" to BigDecimal("65000"),
        "ETH" to BigDecimal("3500"),
        "AAPL" to BigDecimal("180"),
    ),
) {
    fun priceFor(symbol: String): BigDecimal? = prices[symbol.trim().uppercase()]
}
