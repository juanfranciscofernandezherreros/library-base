package com.github.juanfranciscofernandezherreros.library.test.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class BaseIntegrationTestTest {

    @Test
    void isAbstractClass() {
        assertThat(Modifier.isAbstract(BaseIntegrationTest.class.getModifiers())).isTrue();
    }

    @Test
    void hasSpringBootTestAnnotation() {
        assertThat(BaseIntegrationTest.class.isAnnotationPresent(SpringBootTest.class)).isTrue();
    }

    @Test
    void activatesTestProfile() {
        ActiveProfiles annotation = BaseIntegrationTest.class.getAnnotation(ActiveProfiles.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly("test");
    }
}
