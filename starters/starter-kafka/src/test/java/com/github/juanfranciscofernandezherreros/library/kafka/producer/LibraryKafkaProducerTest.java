package com.github.juanfranciscofernandezherreros.library.kafka.producer;

import com.github.juanfranciscofernandezherreros.library.kafka.autoconfigure.KafkaLibraryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LibraryKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private LibraryKafkaProducer<String> producer;
    private KafkaLibraryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new KafkaLibraryProperties();
        properties.setDefaultTopic("test-topic");
        producer = new LibraryKafkaProducer<>(kafkaTemplate, properties);
    }

    @Test
    void sendsToDefaultTopicWhenNoTopicProvided() {
        producer.send("value");
        verify(kafkaTemplate).send("test-topic", "value");
    }

    @Test
    void sendsKeyedMessageToDefaultTopic() {
        producer.send("key", "value");
        verify(kafkaTemplate).send("test-topic", "key", "value");
    }

    @Test
    void sendsKeyedMessageToExplicitTopic() {
        producer.send("explicit-topic", "key", "value");
        verify(kafkaTemplate).send("explicit-topic", "key", "value");
    }
}
