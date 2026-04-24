package com.github.juanfranciscofernandezherreros.library.base.service;

import com.github.juanfranciscofernandezherreros.library.base.autoconfigure.BaseProperties;

/**
 * Core service provided by the base library starter.
 * <p>
 * Consumers can inject this bean to access base library utilities.
 */
public class BaseService {

    private final BaseProperties properties;

    public BaseService(BaseProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns the configured application name.
     *
     * @return application name
     */
    public String getApplicationName() {
        return properties.getApplicationName();
    }

    /**
     * Checks whether verbose logging is enabled.
     *
     * @return {@code true} if verbose logging is active
     */
    public boolean isVerboseLogging() {
        return properties.isVerboseLogging();
    }
}
