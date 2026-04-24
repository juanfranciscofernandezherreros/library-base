package com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Kafka library starter.
 * <p>
 * All properties are prefixed with {@code library.kafka}.
 */
@ConfigurationProperties(prefix = "library.kafka")
public class KafkaLibraryProperties {

    /** Default topic used by {@code LibraryKafkaProducer} when no explicit topic is provided. */
    private String defaultTopic = "library-events";

    /**
     * Client ID prefix added to Kafka producer client IDs created by this library.
     * Useful for observability and identifying library traffic in broker metrics.
     */
    private String clientIdPrefix = "library";

    public String getDefaultTopic() {
        return defaultTopic;
    }

    public void setDefaultTopic(String defaultTopic) {
        this.defaultTopic = defaultTopic;
    }

    public String getClientIdPrefix() {
        return clientIdPrefix;
    }

    public void setClientIdPrefix(String clientIdPrefix) {
        this.clientIdPrefix = clientIdPrefix;
    }
}
