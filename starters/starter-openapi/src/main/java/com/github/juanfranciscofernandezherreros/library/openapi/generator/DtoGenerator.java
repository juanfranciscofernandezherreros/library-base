package com.github.juanfranciscofernandezherreros.library.openapi.generator;

import io.swagger.v3.oas.models.media.Schema;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generates a Lombok-annotated Java DTO class from an OpenAPI object schema.
 *
 * <p>Each schema property is mapped to a Java field annotated with
 * {@code @JsonProperty}. The class itself gets {@code @Data}, {@code @Builder},
 * {@code @NoArgsConstructor}, and {@code @AllArgsConstructor} Lombok annotations
 * so that it is immediately usable in Spring controllers and MapStruct mappers.
 */
public class DtoGenerator {

    private final JavaTypeResolver typeResolver = new JavaTypeResolver();

    /**
     * Generates the Java source of a DTO class for the given OpenAPI schema.
     *
     * @param schemaName  the schema name as declared in {@code components/schemas}
     * @param schema      the schema object
     * @param basePackage the root package; the DTO is placed in {@code <basePackage>.dto}
     * @return complete Java source code as a string
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String generate(String schemaName, Schema schema, String basePackage) {
        String dtoPackage = basePackage + ".dto";
        String className = schemaName + "Dto";

        Set<String> imports = new LinkedHashSet<>();
        imports.add("com.fasterxml.jackson.annotation.JsonProperty");
        imports.add("lombok.AllArgsConstructor");
        imports.add("lombok.Builder");
        imports.add("lombok.Data");
        imports.add("lombok.NoArgsConstructor");

        // Collect field declarations and detect extra imports
        StringBuilder fields = new StringBuilder();
        Map<String, Schema> properties = schema.getProperties();
        if (properties != null) {
            for (Map.Entry<String, Schema> entry : properties.entrySet()) {
                String fieldName = entry.getKey();
                Schema fieldSchema = entry.getValue();
                String javaType = typeResolver.resolve(fieldSchema);

                if (typeResolver.requiresListImport(fieldSchema)) {
                    imports.add("java.util.List");
                }

                fields.append("\n    @JsonProperty(\"").append(fieldName).append("\")\n");
                fields.append("    private ").append(javaType).append(" ")
                      .append(sanitizeFieldName(fieldName)).append(";\n");
            }
        }

        // Build source
        StringBuilder src = new StringBuilder();
        src.append("package ").append(dtoPackage).append(";\n\n");

        for (String imp : imports) {
            src.append("import ").append(imp).append(";\n");
        }

        src.append("\n");
        src.append("@Data\n");
        src.append("@Builder\n");
        src.append("@NoArgsConstructor\n");
        src.append("@AllArgsConstructor\n");
        src.append("public class ").append(className).append(" {\n");
        src.append(fields);
        src.append("}\n");

        return src.toString();
    }

    /**
     * Converts a JSON property name (which may contain hyphens or underscores) to a valid
     * Java camelCase identifier.
     */
    private String sanitizeFieldName(String name) {
        if (name == null || name.isBlank()) {
            return "field";
        }
        // Convert kebab-case or snake_case to camelCase
        StringBuilder result = new StringBuilder();
        boolean nextUpper = false;
        for (char c : name.toCharArray()) {
            if (c == '-' || c == '_') {
                nextUpper = true;
            } else {
                result.append(nextUpper ? Character.toUpperCase(c) : c);
                nextUpper = false;
            }
        }
        return result.toString();
    }
}
