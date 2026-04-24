package com.github.juanfranciscofernandezherreros.library.kafka.producer;

import com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure.KafkaLibraryProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Generic Kafka producer provided by the library Kafka starter.
 * <p>
 * Wraps {@link KafkaTemplate} and applies the library's default topic configuration,
 * simplifying message publishing for consumers of the library.
 *
 * @param <V> the message value type
 */
public class LibraryKafkaProducer<V> {

    private final KafkaTemplate<String, V> kafkaTemplate;
    private final KafkaLibraryProperties properties;

    public LibraryKafkaProducer(KafkaTemplate<String, V> kafkaTemplate,
                                KafkaLibraryProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    /**
     * Sends a message to the configured default topic.
     *
     * @param value the message payload
     * @return a {@link CompletableFuture} representing the send result
     */
    public CompletableFuture<SendResult<String, V>> send(V value) {
        return kafkaTemplate.send(properties.getDefaultTopic(), value);
    }

    /**
     * Sends a keyed message to the configured default topic.
     *
     * @param key   the partition key
     * @param value the message payload
     * @return a {@link CompletableFuture} representing the send result
     */
    public CompletableFuture<SendResult<String, V>> send(String key, V value) {
        return kafkaTemplate.send(properties.getDefaultTopic(), key, value);
    }

    /**
     * Sends a keyed message to an explicit topic.
     *
     * @param topic the target topic
     * @param key   the partition key
     * @param value the message payload
     * @return a {@link CompletableFuture} representing the send result
     */
    public CompletableFuture<SendResult<String, V>> send(String topic, String key, V value) {
        return kafkaTemplate.send(topic, key, value);
    }
}
