package com.github.juanfranciscofernandezherreros.library.openapi.generator;

import com.github.juanfranciscofernandezherreros.library.openapi.autoconfigure.OpenApiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link OpenApiGeneratorService}: parses the bundled
 * {@code petstore.yaml} fixture and asserts that the expected Java source files
 * are generated with the correct content.
 */
class OpenApiGeneratorServiceTest {

    @TempDir
    Path tempDir;

    private OpenApiGeneratorService buildService(Path outputDir) {
        OpenApiProperties props = new OpenApiProperties();
        URL specUrl = getClass().getClassLoader().getResource("petstore.yaml");
        assertThat(specUrl).isNotNull();
        props.setSpecPath(specUrl.toString());
        props.setOutputDir(outputDir.toString());
        props.setBasePackage("com.example.api");
        return new OpenApiGeneratorService(props);
    }

    @Test
    void generatesExpectedDtoFiles() throws IOException {
        buildService(tempDir).generate();

        Path dtoDir = tempDir.resolve("com/example/api/dto");
        assertThat(dtoDir).isDirectory();
        assertThat(dtoDir.resolve("PetDto.java")).exists();
        assertThat(dtoDir.resolve("ErrorDto.java")).exists();
    }

    @Test
    void petDtoHasCorrectContent() throws IOException {
        buildService(tempDir).generate();

        String content = Files.readString(tempDir.resolve("com/example/api/dto/PetDto.java"));
        assertThat(content).contains("package com.example.api.dto;");
        assertThat(content).contains("@Data");
        assertThat(content).contains("@Builder");
        assertThat(content).contains("@NoArgsConstructor");
        assertThat(content).contains("@AllArgsConstructor");
        assertThat(content).contains("private Long id;");
        assertThat(content).contains("private String name;");
        assertThat(content).contains("private String tag;");
    }

    @Test
    void generatesControllerFile() throws IOException {
        buildService(tempDir).generate();

        Path controllerDir = tempDir.resolve("com/example/api/controller");
        assertThat(controllerDir).isDirectory();
        assertThat(controllerDir.resolve("PetsController.java")).exists();
    }

    @Test
    void controllerHasCorrectContent() throws IOException {
        buildService(tempDir).generate();

        String content = Files.readString(
                tempDir.resolve("com/example/api/controller/PetsController.java"));
        assertThat(content).contains("package com.example.api.controller;");
        assertThat(content).contains("@RestController");
        assertThat(content).contains("ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()");
        assertThat(content).contains("listPets");
        assertThat(content).contains("createPet");
        assertThat(content).contains("getPetById");
        assertThat(content).contains("@PathVariable(\"petId\")");
        assertThat(content).contains("@RequestParam(value = \"limit\"");
        assertThat(content).contains("@RequestBody");
    }

    @Test
    void throwsWhenSpecPathIsBlank() {
        OpenApiProperties props = new OpenApiProperties();
        props.setSpecPath("   ");
        props.setOutputDir(tempDir.toString());
        OpenApiGeneratorService service = new OpenApiGeneratorService(props);

        assertThatThrownBy(service::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("spec-path");
    }

    @Test
    void throwsWhenSpecCannotBeParsed() {
        OpenApiProperties props = new OpenApiProperties();
        props.setSpecPath("/non/existent/spec.yaml");
        props.setOutputDir(tempDir.toString());
        OpenApiGeneratorService service = new OpenApiGeneratorService(props);

        assertThatThrownBy(service::generate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse");
    }
}
