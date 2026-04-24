package com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.kafka.consumer.LibraryKafkaConsumer;
import com.github.juanfranciscofernandezherreros.library.kafka.producer.LibraryKafkaProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class KafkaAutoConfigurationTest {

    @SuppressWarnings("unchecked")
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
            .withBean("kafkaTemplate", KafkaTemplate.class, () -> mock(KafkaTemplate.class));

    @Test
    void registersLibraryKafkaProducer() {
        contextRunner.run(context ->
                assertThat(context).hasSingleBean(LibraryKafkaProducer.class));
    }

    @Test
    void doesNotRegisterConsumerByDefault() {
        contextRunner.run(context ->
                assertThat(context).doesNotHaveBean(LibraryKafkaConsumer.class));
    }

    @Test
    void registersConsumerWhenEnabled() {
        contextRunner
                .withPropertyValues("library.kafka.consumer.enabled=true")
                .run(context ->
                        assertThat(context).hasSingleBean(LibraryKafkaConsumer.class));
    }

    @Test
    void registersKafkaLibraryPropertiesWithDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(KafkaLibraryProperties.class);
            KafkaLibraryProperties props = context.getBean(KafkaLibraryProperties.class);
            assertThat(props.getDefaultTopic()).isEqualTo("library-events");
            assertThat(props.getClientIdPrefix()).isEqualTo("library");
        });
    }

    @Test
    void respectsCustomKafkaProperties() {
        contextRunner
                .withPropertyValues(
                        "library.kafka.default-topic=my-events",
                        "library.kafka.client-id-prefix=my-service")
                .run(context -> {
                    KafkaLibraryProperties props = context.getBean(KafkaLibraryProperties.class);
                    assertThat(props.getDefaultTopic()).isEqualTo("my-events");
                    assertThat(props.getClientIdPrefix()).isEqualTo("my-service");
                });
    }
}
