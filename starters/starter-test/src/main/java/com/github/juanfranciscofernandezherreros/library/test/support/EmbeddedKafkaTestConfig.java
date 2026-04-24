package com.github.juanfranciscofernandezherreros.library.test.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

/**
 * Reusable {@link TestConfiguration} that starts an in-process Kafka broker.
 * <p>
 * Import this configuration in tests that require Kafka without a running
 * external broker:
 *
 * <pre>{@code
 * @Import(EmbeddedKafkaTestConfig.class)
 * class MyKafkaTest extends BaseIntegrationTest { ... }
 * }</pre>
 */
@TestConfiguration
public class EmbeddedKafkaTestConfig {

    /** Default number of partitions created for each test topic. */
    private static final int DEFAULT_PARTITIONS = 1;

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        return new EmbeddedKafkaKraftBroker(1, DEFAULT_PARTITIONS);
    }
}
