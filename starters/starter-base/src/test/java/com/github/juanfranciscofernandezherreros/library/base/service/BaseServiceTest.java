package com.github.juanfranciscofernandezherreros.library.base.service;

import com.github.juanfranciscofernandezherreros.library.base.autoconfigure.BaseProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseServiceTest {

    @Test
    void returnsDefaultApplicationName() {
        BaseProperties properties = new BaseProperties();
        BaseService service = new BaseService(properties);
        assertThat(service.getApplicationName()).isEqualTo("library-base");
    }

    @Test
    void returnsCustomApplicationName() {
        BaseProperties properties = new BaseProperties();
        properties.setApplicationName("my-service");
        BaseService service = new BaseService(properties);
        assertThat(service.getApplicationName()).isEqualTo("my-service");
    }

    @Test
    void verboseLoggingIsDisabledByDefault() {
        BaseProperties properties = new BaseProperties();
        BaseService service = new BaseService(properties);
        assertThat(service.isVerboseLogging()).isFalse();
    }

    @Test
    void verboseLoggingCanBeEnabled() {
        BaseProperties properties = new BaseProperties();
        properties.setVerboseLogging(true);
        BaseService service = new BaseService(properties);
        assertThat(service.isVerboseLogging()).isTrue();
    }
}
