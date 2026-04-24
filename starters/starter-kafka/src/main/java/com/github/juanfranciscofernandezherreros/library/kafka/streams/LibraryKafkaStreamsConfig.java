package com.github.juanfranciscofernandezherreros.library.kafka.streams;

import com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure.KafkaLibraryProperties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Streams configuration provided by the library Kafka starter.
 * <p>
 * Registers a default {@link KafkaStreamsConfiguration} bean that bootstraps a
 * Kafka Streams topology using properties from {@code application.yml} and
 * {@link KafkaLibraryProperties}.
 *
 * <p>Enable Kafka Streams in the consuming application by importing this class:
 * <pre>
 * {@literal @}Import(LibraryKafkaStreamsConfig.class)
 * {@literal @}SpringBootApplication
 * public class MyApplication { ... }
 * </pre>
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * spring:
 *   kafka:
 *     bootstrap-servers: localhost:9092
 * library:
 *   kafka:
 *     streams:
 *       application-id: my-streams-app
 *       state-dir: /tmp/kafka-streams
 * </pre>
 */
@Configuration
@EnableKafkaStreams
public class LibraryKafkaStreamsConfig {

    private static final Logger log = LoggerFactory.getLogger(LibraryKafkaStreamsConfig.class);

    private final KafkaLibraryProperties properties;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public LibraryKafkaStreamsConfig(KafkaLibraryProperties properties) {
        this.properties = properties;
    }

    /**
     * Provides the default {@link KafkaStreamsConfiguration} bean required by
     * {@link KafkaStreamsDefaultConfiguration}.
     *
     * @return streams configuration map
     */
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kafkaStreamsConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, properties.getStreams().getApplicationId());
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.STATE_DIR_CONFIG, properties.getStreams().getStateDir());

        log.debug("Kafka Streams configured: applicationId={}, bootstrapServers={}",
                properties.getStreams().getApplicationId(), bootstrapServers);

        return new KafkaStreamsConfiguration(config);
    }

    /**
     * Example pass-through Kafka Streams topology.
     * <p>
     * Reads from the library default topic and logs each record. Replace or extend
     * this bean in the consuming application to implement the actual stream processing.
     *
     * @param builder the {@link StreamsBuilder} injected by the Kafka Streams framework
     * @return the {@link KStream} representing the topology
     */
    @Bean
    public KStream<String, String> libraryKStream(StreamsBuilder builder) {
        KStream<String, String> stream = builder.stream(properties.getDefaultTopic());
        stream.peek((key, value) ->
                log.debug("Kafka Streams record: key={}, value={}", key, value));
        return stream;
    }
}
