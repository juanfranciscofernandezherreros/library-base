package com.github.juanfranciscofernandezherreros.library.base.autoconfigure;

import com.github.juanfranciscofernandezherreros.library.base.service.BaseService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the base library starter.
 * <p>
 * Registers {@link BaseService} unless a custom implementation is already present
 * in the application context.
 */
@AutoConfiguration
@EnableConfigurationProperties(BaseProperties.class)
public class BaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BaseService baseService(BaseProperties properties) {
        return new BaseService(properties);
    }
}
