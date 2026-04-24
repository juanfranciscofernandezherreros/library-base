package com.github.juanfranciscofernandezherreros.library.openapi.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the OpenAPI code-generation starter.
 *
 * <p>All properties are prefixed with {@code library.openapi}.
 *
 * <p>Example {@code application.yml}:
 * <pre>
 * library:
 *   openapi:
 *     spec-path: src/main/resources/openapi.yaml
 *     output-dir: target/generated-sources/openapi
 *     base-package: com.example.api
 *     enabled: true
 * </pre>
 */
@ConfigurationProperties(prefix = "library.openapi")
public class OpenApiProperties {

    /**
     * Path to the OpenAPI specification file (YAML or JSON).
     * Accepts a file-system path, a {@code classpath:} resource, or an HTTP/HTTPS URL.
     */
    private String specPath;

    /**
     * Root directory where generated Java source files are written.
     * Defaults to {@code target/generated-sources/openapi}.
     */
    private String outputDir = "target/generated-sources/openapi";

    /**
     * Base Java package for all generated classes.
     * DTOs are placed in {@code <basePackage>.dto} and controllers in {@code <basePackage>.controller}.
     */
    private String basePackage = "generated.api";

    /**
     * Whether to run code generation on application startup.
     * Set to {@code false} to disable generation without removing the dependency.
     */
    private boolean enabled = true;

    public String getSpecPath() {
        return specPath;
    }

    public void setSpecPath(String specPath) {
        this.specPath = specPath;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
