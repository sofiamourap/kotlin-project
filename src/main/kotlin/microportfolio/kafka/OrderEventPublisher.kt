package microportfolio.kafka

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.ApplicationConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties

fun interface OrderEventPublisher {
    fun publish(event: OrderPlaced)
}

object NoOpOrderEventPublisher : OrderEventPublisher {
    override fun publish(event: OrderPlaced) = Unit
}

class KafkaOrderEventPublisher(
    private val producer: KafkaProducer<String, String>,
    private val topic: String,
) : OrderEventPublisher {
    override fun publish(event: OrderPlaced) {
        val json = Json.encodeToString(event)
        // Key = userId: same user's orders land on the same partition (order preserved per user).
        producer.send(ProducerRecord(topic, event.userId, json)).get()
    }
    // static factory method
    companion object {
        fun from(config: ApplicationConfig, application: Application): KafkaOrderEventPublisher {
            val kafka = config.config("kafka")
            val producer = KafkaProducer<String, String>(
                Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.property("bootstrapServers").getString())
                    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                    put(ProducerConfig.ACKS_CONFIG, "all")
                    put(ProducerConfig.CLIENT_ID_CONFIG, "micro-portfolio-api")
                },
            )
            application.monitor.subscribe(ApplicationStopped) {
                producer.close()
            }
            return KafkaOrderEventPublisher(producer, kafka.property("orderPlacedTopic").getString())
        }
    }
}
