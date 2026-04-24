package com.github.juanfranciscofernandezherreros.library.test.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.test.support.EmbeddedKafkaTestConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

/**
 * Auto-configuration for the test starter.
 * <p>
 * Imports {@link EmbeddedKafkaTestConfig} when {@link EmbeddedKafkaBroker} is
 * present on the classpath (i.e. {@code spring-kafka-test} is a dependency).
 */
@AutoConfiguration
@ConditionalOnClass(EmbeddedKafkaBroker.class)
@Import(EmbeddedKafkaTestConfig.class)
public class TestAutoConfiguration {
}
