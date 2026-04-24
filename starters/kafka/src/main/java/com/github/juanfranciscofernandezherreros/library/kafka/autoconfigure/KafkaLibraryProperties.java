package com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for the Kafka library starter.
 * <p>
 * All properties are prefixed with {@code library.kafka}.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * library:
 *   kafka:
 *     default-topic: my-events
 *     client-id-prefix: my-service
 *     zookeeper-connect: localhost:2181
 *     schema-registry-url: http://localhost:8081
 *     streams:
 *       application-id: my-streams-app
 *       state-dir: /tmp/kafka-streams
 * </pre>
 */
@ConfigurationProperties(prefix = "library.kafka")
public class KafkaLibraryProperties {

    /** Default topic used by {@code LibraryKafkaProducer} when no explicit topic is provided. */
    private String defaultTopic = "library-events";

    /**
     * Client ID prefix added to Kafka producer/consumer client IDs created by this library.
     * Useful for observability and identifying library traffic in broker metrics.
     */
    private String clientIdPrefix = "library";

    /**
     * ZooKeeper connection string used for Kafka admin operations on clusters that
     * still rely on ZooKeeper (pre-KRaft). Format: {@code host:port[,host:port]/chroot}.
     */
    private String zookeeperConnect = "localhost:2181";

    /**
     * Confluent Schema Registry URL used by Avro serialisers/deserialisers.
     * Add {@code io.confluent:kafka-avro-serializer} to your project and configure
     * {@code spring.kafka.producer.value-serializer} / {@code consumer.value-deserializer}
     * to {@code io.confluent.kafka.serializers.KafkaAvroSerializer} /
     * {@code KafkaAvroDeserializer} to activate Avro support.
     */
    private String schemaRegistryUrl = "http://localhost:8081";

    /** Kafka Streams specific configuration. */
    @NestedConfigurationProperty
    private Streams streams = new Streams();

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

    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    public String getSchemaRegistryUrl() {
        return schemaRegistryUrl;
    }

    public void setSchemaRegistryUrl(String schemaRegistryUrl) {
        this.schemaRegistryUrl = schemaRegistryUrl;
    }

    public Streams getStreams() {
        return streams;
    }

    public void setStreams(Streams streams) {
        this.streams = streams;
    }

    /**
     * Kafka Streams sub-properties ({@code library.kafka.streams.*}).
     */
    public static class Streams {

        /** Application ID for the Kafka Streams topology. Must be unique per cluster. */
        private String applicationId = "library-streams-app";

        /** Local directory where Kafka Streams stores its state. */
        private String stateDir = "/tmp/kafka-streams";

        public String getApplicationId() {
            return applicationId;
        }

        public void setApplicationId(String applicationId) {
            this.applicationId = applicationId;
        }

        public String getStateDir() {
            return stateDir;
        }

        public void setStateDir(String stateDir) {
            this.stateDir = stateDir;
        }
    }
}
