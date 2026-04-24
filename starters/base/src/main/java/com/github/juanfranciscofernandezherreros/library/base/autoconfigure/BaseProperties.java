package com.github.juanfranciscofernandezherreros.library.base.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the base library module.
 * <p>
 * All properties are prefixed with {@code library.base}.
 */
@ConfigurationProperties(prefix = "library.base")
public class BaseProperties {

    /** Human-readable name exposed by the library instance. */
    private String applicationName = "library-base";

    /** Whether to enable verbose logging in library components. */
    private boolean verboseLogging = false;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }
}
