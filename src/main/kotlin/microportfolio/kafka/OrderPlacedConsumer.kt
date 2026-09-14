package microportfolio.kafka

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import microportfolio.domain.applyOrderPlaced
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.errors.WakeupException
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.Properties

fun Application.startOrderPlacedConsumer() {
    val kafka = environment.config.config("kafka")
    val topic = kafka.property("orderPlacedTopic").getString()
    val consumer = KafkaConsumer<String, String>(
        Properties().apply {
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.property("bootstrapServers").getString())
            put(ConsumerConfig.GROUP_ID_CONFIG, kafka.property("consumerGroup").getString())
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java.name)
            put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
            put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
            put(ConsumerConfig.CLIENT_ID_CONFIG, "micro-portfolio-holdings")
        },
    )

    monitor.subscribe(ApplicationStopped) {
        consumer.wakeup()
    }

    launch(Dispatchers.IO) {
        try {
            consumer.subscribe(listOf(topic))
            log.info("OrderPlaced consumer subscribed to $topic")
            while (true) {
                val records = consumer.poll(Duration.ofMillis(500))
                for (record in records) {
                    try {
                        val event = Json.decodeFromString<OrderPlaced>(record.value())
                        applyOrderPlaced(event)
                        log.info("Applied order ${event.orderId} (${event.side} ${event.quantity} ${event.symbol})")
                    } catch (e: Exception) {
                        log.error("Failed to apply OrderPlaced: ${record.value()}", e)
                    }
                }
                if (!records.isEmpty) {
                    consumer.commitSync()
                }
            }
        } catch (_: WakeupException) {
            log.info("OrderPlaced consumer stopping")
        } finally {
            consumer.close()
        }
    }
}
