package com.github.juanfranciscofernandezherreros.library.base.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.base.service.BaseService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BaseAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BaseAutoConfiguration.class));

    @Test
    void registersBaseServiceWithDefaultProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(BaseService.class);
            BaseService service = context.getBean(BaseService.class);
            assertThat(service.getApplicationName()).isEqualTo("library-base");
            assertThat(service.isVerboseLogging()).isFalse();
        });
    }

    @Test
    void respectsCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "library.base.application-name=my-service",
                        "library.base.verbose-logging=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(BaseService.class);
                    BaseService service = context.getBean(BaseService.class);
                    assertThat(service.getApplicationName()).isEqualTo("my-service");
                    assertThat(service.isVerboseLogging()).isTrue();
                });
    }

    @Test
    void backsOffIfBaseServiceAlreadyDefined() {
        contextRunner
                .withBean(BaseService.class, () -> new BaseService(new BaseProperties()))
                .run(context -> assertThat(context).hasSingleBean(BaseService.class));
    }
}
