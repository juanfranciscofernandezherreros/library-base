package com.github.juanfranciscofernandezherreros.library.openapi.generator;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generates a Spring MVC {@code @RestController} stub class from a group of OpenAPI operations
 * that share the same tag.
 *
 * <p>Each operation becomes a method that returns
 * {@code ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()} by default,
 * so the generated class compiles and is immediately deployable. Developers replace
 * the stub body with their actual business logic.
 */
public class ControllerGenerator {

    private final JavaTypeResolver typeResolver = new JavaTypeResolver();

    /**
     * Generates a {@code @RestController} stub class source for the given tag and operations.
     *
     * @param tag         the OpenAPI tag name (used as the class name prefix)
     * @param operations  list of operations belonging to this tag
     * @param basePackage the root package; the controller is placed in {@code <basePackage>.controller}
     * @return complete Java source code as a string
     */
    public String generate(String tag, List<OperationEntry> operations, String basePackage) {
        String controllerPackage = basePackage + ".controller";
        String dtoPackage = basePackage + ".dto";
        String className = capitalize(tag) + "Controller";

        Set<String> imports = new LinkedHashSet<>();
        imports.add("org.springframework.http.HttpStatus");
        imports.add("org.springframework.http.ResponseEntity");
        imports.add("org.springframework.web.bind.annotation.PathVariable");
        imports.add("org.springframework.web.bind.annotation.RequestBody");
        imports.add("org.springframework.web.bind.annotation.RequestMapping");
        imports.add("org.springframework.web.bind.annotation.RequestParam");
        imports.add("org.springframework.web.bind.annotation.RestController");

        // Collect methods first to discover required imports
        List<MethodSpec> methods = new ArrayList<>();
        for (OperationEntry entry : operations) {
            MethodSpec ms = buildMethod(entry, dtoPackage, imports);
            methods.add(ms);
        }

        // Build source
        StringBuilder src = new StringBuilder();
        src.append("package ").append(controllerPackage).append(";\n\n");

        for (String imp : imports) {
            src.append("import ").append(imp).append(";\n");
        }

        src.append("\n");
        src.append("@RestController\n");
        src.append("public class ").append(className).append(" {\n");

        for (MethodSpec ms : methods) {
            src.append("\n");
            src.append("    @RequestMapping(value = \"").append(ms.path)
               .append("\", method = org.springframework.web.bind.annotation.RequestMethod.")
               .append(ms.httpMethod.toUpperCase(Locale.ROOT)).append(")\n");
            src.append("    public ").append(ms.returnType).append(" ").append(ms.methodName)
               .append("(").append(ms.params).append(") {\n");
            src.append("        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();\n");
            src.append("    }\n");
        }

        src.append("}\n");

        return src.toString();
    }

    @SuppressWarnings("rawtypes")
    private MethodSpec buildMethod(OperationEntry entry, String dtoPackage, Set<String> imports) {
        Operation op = entry.getOperation();
        String httpMethod = entry.getHttpMethod();
        String path = entry.getPath();

        // Determine method name
        String methodName = op.getOperationId() != null
                ? sanitizeMethodName(op.getOperationId())
                : httpMethod.toLowerCase(Locale.ROOT) + pathToMethodSuffix(path);

        // Determine return type from 200/201 response
        String returnType = resolveReturnType(op, dtoPackage, imports);

        // Build parameter list
        StringBuilder params = new StringBuilder();
        List<Parameter> parameters = op.getParameters();
        boolean firstParam = true;

        if (parameters != null) {
            for (Parameter param : parameters) {
                if (!firstParam) {
                    params.append(", ");
                }
                firstParam = false;
                String javaType = typeResolver.resolve(param.getSchema());
                if (typeResolver.requiresListImport(param.getSchema())) {
                    imports.add("java.util.List");
                }
                switch (param.getIn()) {
                    case "path" ->
                        params.append("@PathVariable(\"").append(param.getName()).append("\") ")
                              .append(javaType).append(" ").append(sanitizeFieldName(param.getName()));
                    case "query" ->
                        params.append("@RequestParam(value = \"").append(param.getName())
                              .append("\", required = ").append(Boolean.TRUE.equals(param.getRequired())).append(") ")
                              .append(javaType).append(" ").append(sanitizeFieldName(param.getName()));
                    case "header" -> {
                        imports.add("org.springframework.web.bind.annotation.RequestHeader");
                        params.append("@RequestHeader(value = \"").append(param.getName())
                              .append("\", required = ").append(Boolean.TRUE.equals(param.getRequired())).append(") ")
                              .append(javaType).append(" ").append(sanitizeFieldName(param.getName()));
                    }
                    case "cookie" -> {
                        imports.add("org.springframework.web.bind.annotation.CookieValue");
                        params.append("@CookieValue(value = \"").append(param.getName())
                              .append("\", required = ").append(Boolean.TRUE.equals(param.getRequired())).append(") ")
                              .append(javaType).append(" ").append(sanitizeFieldName(param.getName()));
                    }
                    default ->
                        params.append(javaType).append(" ").append(sanitizeFieldName(param.getName()));
                }
            }
        }

        // Add request body parameter if present
        RequestBody requestBody = op.getRequestBody();
        if (requestBody != null) {
            Content content = requestBody.getContent();
            if (content != null) {
                io.swagger.v3.oas.models.media.MediaType mediaType =
                        content.get("application/json");
                if (mediaType == null && !content.isEmpty()) {
                    mediaType = content.values().iterator().next();
                }
                if (mediaType != null) {
                    Schema bodySchema = mediaType.getSchema();
                    String bodyType = typeResolver.resolve(bodySchema);
                    if (typeResolver.requiresListImport(bodySchema)) {
                        imports.add("java.util.List");
                    }
                    addDtoImport(bodyType, dtoPackage, imports);
                    if (!firstParam) {
                        params.append(", ");
                    }
                    params.append("@RequestBody ").append(bodyType).append(" body");
                }
            }
        }

        return new MethodSpec(path, httpMethod, methodName, returnType, params.toString());
    }

    @SuppressWarnings("rawtypes")
    private String resolveReturnType(Operation op, String dtoPackage, Set<String> imports) {
        ApiResponses responses = op.getResponses();
        if (responses == null) {
            return "ResponseEntity<?>";
        }

        // Try 200 first, then 201, then the first available response
        ApiResponse response = responses.get("200");
        if (response == null) {
            response = responses.get("201");
        }
        if (response == null && !responses.isEmpty()) {
            response = responses.values().iterator().next();
        }

        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return "ResponseEntity<Void>";
        }

        io.swagger.v3.oas.models.media.MediaType mediaType =
                response.getContent().get("application/json");
        if (mediaType == null) {
            mediaType = response.getContent().values().iterator().next();
        }

        if (mediaType.getSchema() == null) {
            return "ResponseEntity<Void>";
        }

        Schema schema = mediaType.getSchema();
        String javaType = typeResolver.resolve(schema);
        if (typeResolver.requiresListImport(schema)) {
            imports.add("java.util.List");
        }
        addDtoImport(javaType, dtoPackage, imports);

        return "ResponseEntity<" + javaType + ">";
    }

    private void addDtoImport(String javaType, String dtoPackage, Set<String> imports) {
        // Add import for any DTO type (ends with "Dto")
        if (javaType.endsWith("Dto")) {
            imports.add(dtoPackage + "." + javaType);
        } else if (javaType.startsWith("List<") && javaType.endsWith("Dto>")) {
            String inner = javaType.substring(5, javaType.length() - 1);
            imports.add(dtoPackage + "." + inner);
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "Default";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String sanitizeMethodName(String name) {
        if (name == null || name.isBlank()) {
            return "handle";
        }
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '-' || c == '_' || c == ' ') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : (i == 0 ? Character.toLowerCase(c) : c));
                nextUpper = false;
            }
        }
        return result.toString();
    }

    private String sanitizeFieldName(String name) {
        return sanitizeMethodName(name);
    }

    private String pathToMethodSuffix(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        StringBuilder suffix = new StringBuilder();
        boolean nextUpper = true;
        for (char c : path.toCharArray()) {
            if (c == '/' || c == '{' || c == '}' || c == '-' || c == '_') {
                nextUpper = true;
            } else {
                suffix.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return suffix.toString();
    }

    private static final class MethodSpec {
        final String path;
        final String httpMethod;
        final String methodName;
        final String returnType;
        final String params;

        MethodSpec(String path, String httpMethod, String methodName,
                   String returnType, String params) {
            this.path = path;
            this.httpMethod = httpMethod;
            this.methodName = methodName;
            this.returnType = returnType;
            this.params = params;
        }
    }
}
