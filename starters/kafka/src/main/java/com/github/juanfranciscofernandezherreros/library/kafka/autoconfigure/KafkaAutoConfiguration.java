package com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.kafka.producer.LibraryKafkaProducer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Auto-configuration for the Kafka library starter.
 * <p>
 * Activates only when {@link KafkaTemplate} is present on the classpath and
 * registers a {@link LibraryKafkaProducer} unless a custom one has been defined.
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
}
