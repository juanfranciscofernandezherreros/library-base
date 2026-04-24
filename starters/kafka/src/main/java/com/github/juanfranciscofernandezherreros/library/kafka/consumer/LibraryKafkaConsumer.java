package com.github.juanfranciscofernandezherreros.library.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Example Kafka consumer provided by the library Kafka starter.
 * <p>
 * Demonstrates how to declare a {@link KafkaListener} that reads from a topic
 * configured via {@code application.yml}. Consumer projects are expected to
 * extend or replace this class with their own business logic.
 *
 * <p>Example {@code application.yml} configuration:
 * <pre>
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 *     consumer:
 *       group-id: my-consumer-group
 *       auto-offset-reset: earliest
 *       key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 *       value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
 * library:
 *   kafka:
 *     default-topic: my-events
 * </pre>
 *
 * @param <V> the message value type
 */
public class LibraryKafkaConsumer<V> {

    private static final Logger log = LoggerFactory.getLogger(LibraryKafkaConsumer.class);

    /**
     * Processes an incoming Kafka message.
     * <p>
     * The topic and group ID are resolved from {@code application.yml} via SpEL
     * expressions referencing the {@code library.kafka.default-topic} and
     * {@code spring.kafka.consumer.group-id} properties.
     *
     * @param payload   the message payload
     * @param topic     the topic from which the message was received
     * @param partition the partition from which the message was received
     * @param offset    the offset of the message within the partition
     */
    @KafkaListener(
            topics = "${library.kafka.default-topic:library-events}",
            groupId = "${spring.kafka.consumer.group-id:library-consumer-group}"
    )
    public void consume(
            @Payload V payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received message: topic={}, partition={}, offset={}, payload={}",
                topic, partition, offset, payload);

        process(payload);
    }

    /**
     * Processes the received message payload.
     * <p>
     * Override this method in subclasses to implement custom message handling logic.
     *
     * @param payload the message payload
     */
    protected void process(V payload) {
        log.debug("Processing payload: {}", payload);
    }
}
