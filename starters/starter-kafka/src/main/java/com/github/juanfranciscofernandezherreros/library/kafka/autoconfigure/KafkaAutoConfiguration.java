package com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.kafka.consumer.LibraryKafkaConsumer;
import com.github.juanfranciscofernandezherreros.library.kafka.producer.LibraryKafkaProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for the Kafka library starter.
 * <p>
 * Activates only when {@link KafkaTemplate} is present on the classpath and
 * registers a {@link LibraryKafkaProducer} and a {@link LibraryKafkaConsumer}
 * unless custom implementations have already been defined.
 * <p>
 * The consumer bean is only registered when
 * {@code library.kafka.consumer.enabled=true} (default: {@code false}) to avoid
 * starting an unwanted listener in projects that only use the producer.
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaLibraryProperties.class)
public class KafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public <V> LibraryKafkaProducer<V> libraryKafkaProducer(KafkaTemplate<String, V> kafkaTemplate,
                                                             KafkaLibraryProperties properties) {
        return new LibraryKafkaProducer<>(kafkaTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "library.kafka.consumer.enabled", havingValue = "true")
    public <V> LibraryKafkaConsumer<V> libraryKafkaConsumer() {
        return new LibraryKafkaConsumer<>();
    }
}
