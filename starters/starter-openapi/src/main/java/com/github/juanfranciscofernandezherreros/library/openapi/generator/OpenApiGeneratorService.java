package com.github.juanfranciscofernandezherreros.library.openapi.generator;

import com.github.juanfranciscofernandezherreros.library.openapi.autoconfigure.OpenApiProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates OpenAPI code generation: parses the spec and delegates to
 * {@link DtoGenerator} (schemas) and {@link ControllerGenerator} (paths),
 * writing every generated file to the configured output directory.
 *
 * <p>Generated layout under {@code outputDir}:
 * <pre>
 * {outputDir}/
 *   {basePackage.replace('.','/')}/
 *     dto/           ← one *Dto.java per schema
 *     controller/    ← one *Controller.java per tag (or "Default")
 * </pre>
 */
public class OpenApiGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(OpenApiGeneratorService.class);

    private final OpenApiProperties properties;
    private final DtoGenerator dtoGenerator;
    private final ControllerGenerator controllerGenerator;

    public OpenApiGeneratorService(OpenApiProperties properties) {
        this.properties = properties;
        this.dtoGenerator = new DtoGenerator();
        this.controllerGenerator = new ControllerGenerator();
    }

    /**
     * Runs the full generation pipeline.
     *
     * @throws IOException          if files cannot be written to the output directory
     * @throws IllegalStateException if the spec cannot be parsed or {@code spec-path} is not set
     */
    public void generate() throws IOException {
        String specPath = properties.getSpecPath();
        if (specPath == null || specPath.isBlank()) {
            throw new IllegalStateException(
                    "library.openapi.spec-path must be configured when library.openapi.enabled=true");
        }

        String resolvedPath = resolveSpecPath(specPath);
        log.info("Generating code from OpenAPI spec: {}", resolvedPath);

        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        OpenAPI openAPI = new OpenAPIV3Parser().read(resolvedPath, null, parseOptions);

        if (openAPI == null) {
            throw new IllegalStateException(
                    "Failed to parse OpenAPI specification from: " + resolvedPath);
        }

        Path outputBase = Path.of(properties.getOutputDir());
        int dtoCount = generateDtos(openAPI, outputBase);
        int controllerCount = generateControllers(openAPI, outputBase);

        log.info("OpenAPI generation complete: {} DTO(s), {} controller(s) written to {}",
                dtoCount, controllerCount, outputBase.toAbsolutePath());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("rawtypes")
    private int generateDtos(OpenAPI openAPI, Path outputBase) throws IOException {
        if (openAPI.getComponents() == null || openAPI.getComponents().getSchemas() == null) {
            return 0;
        }
        int count = 0;
        for (Map.Entry<String, Schema> entry : openAPI.getComponents().getSchemas().entrySet()) {
            String schemaName = entry.getKey();
            String source = dtoGenerator.generate(schemaName, entry.getValue(), properties.getBasePackage());
            writeFile(outputBase, properties.getBasePackage() + ".dto", schemaName + "Dto", source);
            count++;
        }
        return count;
    }

    private int generateControllers(OpenAPI openAPI, Path outputBase) throws IOException {
        if (openAPI.getPaths() == null) {
            return 0;
        }
        Map<String, List<OperationEntry>> byTag = collectOperationsByTag(openAPI);
        for (Map.Entry<String, List<OperationEntry>> entry : byTag.entrySet()) {
            String tag = entry.getKey();
            String source = controllerGenerator.generate(tag, entry.getValue(), properties.getBasePackage());
            String className = capitalize(tag) + "Controller";
            writeFile(outputBase, properties.getBasePackage() + ".controller", className, source);
        }
        return byTag.size();
    }

    private Map<String, List<OperationEntry>> collectOperationsByTag(OpenAPI openAPI) {
        Map<String, List<OperationEntry>> byTag = new LinkedHashMap<>();

        openAPI.getPaths().forEach((path, pathItem) -> {
            Map<String, Operation> ops = extractOperations(pathItem);
            ops.forEach((httpMethod, operation) -> {
                List<String> tags = operation.getTags();
                String tag = (tags != null && !tags.isEmpty()) ? tags.get(0) : "default";
                byTag.computeIfAbsent(tag, k -> new ArrayList<>())
                     .add(new OperationEntry(path, httpMethod, operation));
            });
        });

        return byTag;
    }

    private Map<String, Operation> extractOperations(PathItem pathItem) {
        Map<String, Operation> ops = new LinkedHashMap<>();
        if (pathItem.getGet() != null)    ops.put("GET",    pathItem.getGet());
        if (pathItem.getPost() != null)   ops.put("POST",   pathItem.getPost());
        if (pathItem.getPut() != null)    ops.put("PUT",    pathItem.getPut());
        if (pathItem.getDelete() != null) ops.put("DELETE", pathItem.getDelete());
        if (pathItem.getPatch() != null)  ops.put("PATCH",  pathItem.getPatch());
        if (pathItem.getHead() != null)   ops.put("HEAD",   pathItem.getHead());
        if (pathItem.getOptions() != null) ops.put("OPTIONS", pathItem.getOptions());
        return ops;
    }

    private void writeFile(Path base, String pkg, String className, String source) throws IOException {
        Path dir = base.resolve(pkg.replace('.', '/'));
        Files.createDirectories(dir);
        Path file = dir.resolve(className + ".java");
        Files.writeString(file, source, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.debug("Generated: {}", file);
    }

    /**
     * Resolves {@code classpath:} prefix to an absolute file path so that swagger-parser
     * can read the file.  Plain file paths and HTTP(S) URLs are returned unchanged.
     */
    private String resolveSpecPath(String specPath) {
        if (specPath.startsWith("classpath:")) {
            String resourcePath = specPath.substring("classpath:".length());
            try {
                return new ClassPathResource(resourcePath).getURL().toString();
            } catch (IOException e) {
                throw new IllegalStateException(
                        "Cannot resolve classpath resource: " + resourcePath, e);
            }
        }
        return specPath;
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) {
            return "Default";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
