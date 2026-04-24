package com.github.juanfranciscofernandezherreros.library.openapi.generator;

import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Converts an OpenAPI {@link Schema} into the corresponding Java type name string.
 *
 * <p>Supported mappings:
 * <ul>
 *   <li>{@code $ref} → resolved DTO class name (e.g. {@code PetDto})</li>
 *   <li>{@code string} → {@code String}</li>
 *   <li>{@code integer} (int32) → {@code Integer}, (int64) → {@code Long}</li>
 *   <li>{@code number} (float) → {@code Float}, otherwise → {@code Double}</li>
 *   <li>{@code boolean} → {@code Boolean}</li>
 *   <li>{@code array} → {@code List<ItemType>}</li>
 *   <li>anything else → {@code Object}</li>
 * </ul>
 */
public class JavaTypeResolver {

    /**
     * Resolves the Java type name for the given OpenAPI schema.
     *
     * @param schema the schema to resolve; may be {@code null}
     * @return a Java type name string, never {@code null}
     */
    @SuppressWarnings("rawtypes")
    public String resolve(Schema schema) {
        if (schema == null) {
            return "Object";
        }

        String ref = schema.get$ref();
        if (ref != null && !ref.isBlank()) {
            // "#/components/schemas/Pet" → "PetDto"
            String simpleName = ref.substring(ref.lastIndexOf('/') + 1);
            return simpleName + "Dto";
        }

        String type = schema.getType();
        String format = schema.getFormat();

        if ("string".equals(type)) {
            return "String";
        }
        if ("integer".equals(type)) {
            return "int64".equals(format) ? "Long" : "Integer";
        }
        if ("number".equals(type)) {
            return "float".equals(format) ? "Float" : "Double";
        }
        if ("boolean".equals(type)) {
            return "Boolean";
        }
        if ("array".equals(type) && schema instanceof ArraySchema) {
            String itemType = resolve(((ArraySchema) schema).getItems());
            return "List<" + itemType + ">";
        }

        return "Object";
    }

    /**
     * Returns {@code true} if the resolved type requires a {@code java.util.List} import.
     */
    @SuppressWarnings("rawtypes")
    public boolean requiresListImport(Schema schema) {
        if (schema == null) {
            return false;
        }
        return "array".equals(schema.getType());
    }
}
