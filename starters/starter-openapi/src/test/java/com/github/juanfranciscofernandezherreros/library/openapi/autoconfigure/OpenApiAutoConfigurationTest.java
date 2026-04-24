package com.github.juanfranciscofernandezherreros.library.openapi.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.openapi.generator.OpenApiGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link OpenApiAutoConfiguration} registers beans correctly
 * and honours the {@code library.openapi.enabled} toggle.
 */
class OpenApiAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration.class));

    @Test
    void serviceAndRunnerBeansPresentWhenEnabledAndSpecPathSet() {
        contextRunner
                .withPropertyValues(
                        "library.openapi.enabled=true",
                        // Use a non-existent path — we only check bean registration, not execution
                        "library.openapi.spec-path=classpath:petstore.yaml",
                        "library.openapi.output-dir=/tmp/test-openapi-output",
                        "library.openapi.base-package=com.example.api")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(OpenApiGeneratorService.class);
                    // ApplicationRunner is registered as a bean
                    assertThat(ctx).hasBean("openApiGenerationRunner");
                });
    }

    @Test
    void noBeansWhenDisabled() {
        contextRunner
                .withPropertyValues(
                        "library.openapi.enabled=false",
                        "library.openapi.spec-path=classpath:petstore.yaml")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(OpenApiGeneratorService.class));
    }

    @Test
    void customServiceBeanNotOverridden() {
        contextRunner
                .withPropertyValues(
                        "library.openapi.enabled=true",
                        "library.openapi.spec-path=classpath:petstore.yaml")
                .withBean(OpenApiGeneratorService.class,
                        () -> new OpenApiGeneratorService(new OpenApiProperties()))
                .run(ctx -> assertThat(ctx).hasSingleBean(OpenApiGeneratorService.class));
    }
}
