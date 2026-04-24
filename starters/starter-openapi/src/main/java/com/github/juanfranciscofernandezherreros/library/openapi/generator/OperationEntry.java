package com.github.juanfranciscofernandezherreros.library.openapi.generator;

import io.swagger.v3.oas.models.Operation;

/**
 * Carries metadata for a single OpenAPI operation together with its HTTP method and path.
 */
public final class OperationEntry {

    private final String path;
    private final String httpMethod;
    private final Operation operation;

    public OperationEntry(String path, String httpMethod, Operation operation) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.operation = operation;
    }

    public String getPath() {
        return path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public Operation getOperation() {
        return operation;
    }
}
