package microportfolio.kafka

import kotlinx.serialization.Serializable

/** Event on topic order-placed. Price is snapshotted so the consumer does not look up quotes again. */
@Serializable
data class OrderPlaced(
    val orderId: String,
    val userId: String,
    val symbol: String,
    val side: String,
    val quantity: String,
    val price: String,
    val requestId: String = "",
)
