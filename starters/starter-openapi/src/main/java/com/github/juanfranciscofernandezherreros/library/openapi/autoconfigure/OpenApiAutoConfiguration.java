package com.github.juanfranciscofernandezherreros.library.openapi.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.openapi.generator.OpenApiGeneratorService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the OpenAPI code-generation starter.
 *
 * <p>When {@code library.openapi.enabled} is {@code true} (the default) and
 * {@code library.openapi.spec-path} is set, an {@link ApplicationRunner} is registered
 * that generates DTO and controller source files on startup.
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenApiProperties.class)
@ConditionalOnProperty(prefix = "library.openapi", name = "enabled", matchIfMissing = true)
public class OpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenApiGeneratorService openApiGeneratorService(OpenApiProperties properties) {
        return new OpenApiGeneratorService(properties);
    }

    @Bean
    public ApplicationRunner openApiGenerationRunner(OpenApiGeneratorService generatorService) {
        return args -> generatorService.generate();
    }
}
