# library-base

> A collection of opinionated Spring Boot auto-configuration starters for building Java microservices — covering shared utilities, REST/OpenAPI, databases, Kafka, and test helpers.

---

## Table of contents

- [Overview](#overview)
- [Module structure](#module-structure)
- [Requirements](#requirements)
- [Building the library](#building-the-library)
- [Using the library in a microservice](#using-the-library-in-a-microservice)
  - [Option A — Inherit the parent POM](#option-a--inherit-the-parent-pom)
  - [Option B — Import the BOM only](#option-b--import-the-bom-only)
- [Starters reference](#starters-reference)
  - [starter-base](#starter-base)
  - [starter-openapi](#starter-openapi)
  - [starter-db](#starter-db)
  - [starter-kafka](#starter-kafka)
  - [starter-test](#starter-test)
- [Full example — Pet Store microservice](#full-example--pet-store-microservice)
- [Publishing to Nexus / Artifactory](#publishing-to-nexus--artifactory)

---

## Overview

`library-base` is a multi-module Maven project that provides ready-made Spring Boot starters for the most common cross-cutting concerns in a microservice landscape:

| Concern | Starter artifact |
|---------|-----------------|
| Shared utilities / base beans | `library-base-starter-base` |
| REST API from an OpenAPI spec | `library-base-starter-openapi` |
| Relational database (JPA + Flyway) | `library-base-starter-db` |
| Apache Kafka producer / consumer | `library-base-starter-kafka` |
| Test helpers (JUnit 5 + embedded Kafka) | `library-base-starter-test` |

All starters follow the Spring Boot auto-configuration contract: add the JAR to the classpath and set the relevant `application.yml` properties — no `@Import` or `@EnableXxx` annotations required.

---

## Module structure

```
library-base/
├── pom.xml                  ← root reactor (library-base-build)
├── parent/                  ← library-base-parent  (Java 17, Spring Boot 3.2.5, plugin management)
├── bom/                     ← library-base-bom     (centralised dependency versions)
└── starters/
    ├── starter-base/        ← library-base-starter-base
    ├── starter-openapi/     ← library-base-starter-openapi
    ├── starter-db/          ← library-base-starter-db
    ├── starter-kafka/       ← library-base-starter-kafka
    └── starter-test/        ← library-base-starter-test
```

---

## Requirements

| Tool | Minimum version |
|------|----------------|
| Java | 17 |
| Maven | 3.9 |
| Spring Boot | 3.2.5 |

---

## Building the library

Clone the repository and run `mvn clean install` from the root to compile, test, and install all modules into your local Maven repository (`~/.m2`):

```bash
# Full build (compile + test + install)
mvn clean install

# Skip tests for a faster iteration
mvn clean install -DskipTests

# Build and install a single starter (and its upstream dependencies)
mvn clean install -pl starters/starter-openapi -am
```

After the build the following artifacts are available in your local repository:

| Artifact | Packaging |
|----------|-----------|
| `com.github.juanfranciscofernandezherreros:library-base-parent:1.0.1` | pom |
| `com.github.juanfranciscofernandezherreros:library-base-bom:1.0.1` | pom |
| `com.github.juanfranciscofernandezherreros:library-base-starter-base:1.0.1` | jar |
| `com.github.juanfranciscofernandezherreros:library-base-starter-openapi:1.0.1` | jar |
| `com.github.juanfranciscofernandezherreros:library-base-starter-db:1.0.1` | jar |
| `com.github.juanfranciscofernandezherreros:library-base-starter-kafka:1.0.1` | jar |
| `com.github.juanfranciscofernandezherreros:library-base-starter-test:1.0.1` | jar |

---

## Using the library in a microservice

There are two integration patterns. **Option A** is recommended for new microservices because it also provides the plugin management needed for OpenAPI code generation.

### Option A — Inherit the parent POM

```xml
<parent>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-parent</artifactId>
  <version>1.0.1</version>
</parent>

<groupId>com.example</groupId>
<artifactId>my-service</artifactId>
<version>0.0.1-SNAPSHOT</version>
```

Inheriting `library-base-parent` automatically:
- Sets Java 17, UTF-8 encoding, and Spring Boot 3.2.5 plugin versions.
- Imports `library-base-bom` so you never need to specify versions for library-base starters.
- Provides plugin management for `openapi-generator-maven-plugin` and `build-helper-maven-plugin` (activate them by declaring them in `<build><plugins>`).
- Attaches sources and Javadoc JARs on every build.
- Disables the Spring Boot fat-JAR repackage goal (libraries must not be repackaged; override with `<skip>false</skip>` in your service's plugin configuration).

### Option B — Import the BOM only

If your microservice already has its own parent POM, import the BOM inside `<dependencyManagement>`:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-bom</artifactId>
      <version>1.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

You will still need to configure the compiler plugin manually if you want Lombok + MapStruct annotation processing or OpenAPI code generation.

---

## Starters reference

### starter-base

Provides shared Spring beans and a `BaseService` used by the other starters.  
No mandatory configuration — works out of the box.

**Dependency:**
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-base</artifactId>
</dependency>
```

**`application.yml` (all optional):**
```yaml
library:
  base:
    application-name: my-service   # default: "library-base"
    verbose-logging: false         # default: false
```

---

### starter-openapi

Reads an **OpenAPI 3 YAML/JSON spec** and at startup generates Java source files:
- **DTOs** in `<base-package>.dto`
- **Controller stubs** in `<base-package>.controller`

The `openapi-generator-maven-plugin` declared in `library-base-parent` also generates these interfaces at compile time so they are fully type-safe.

**Dependency:**
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-openapi</artifactId>
</dependency>
```

**`application.yml`:**
```yaml
library:
  openapi:
    enabled: true                                    # default: true
    spec-path: classpath:openapi.yaml                # file-system path, classpath: or http(s):// URL
    output-dir: target/generated-sources/openapi     # default
    base-package: com.example.myservice.api          # default: generated.api
```

**Maven code-generation properties** (override per-project):
```xml
<properties>
  <openapi.spec-path>${project.basedir}/src/main/resources/openapi.yaml</openapi.spec-path>
  <openapi.api-package>com.example.myservice.api</openapi.api-package>
  <openapi.model-package>com.example.myservice.api.model</openapi.model-package>
</properties>
```

Activate the generator plugins in your `<build><plugins>`:
```xml
<build>
  <plugins>
    <!-- Generate interfaces + DTOs from openapi.yaml -->
    <plugin>
      <groupId>org.openapitools</groupId>
      <artifactId>openapi-generator-maven-plugin</artifactId>
    </plugin>
    <!-- Register generated sources so javac picks them up -->
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>build-helper-maven-plugin</artifactId>
    </plugin>
  </plugins>
</build>
```

---

### starter-db

Auto-configures a JPA `JpaVendorAdapter` for PostgreSQL.  
Requires `spring-boot-starter-data-jpa` on the classpath (add it to your service).

**Dependency:**
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-db</artifactId>
</dependency>

<!-- Also add JPA (not bundled to keep the starter optional) -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

**`application.yml`:**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: myuser
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration

library:
  db:
    default-schema: public   # default: "public"
    ddl-auto: validate       # default: "validate"
```

---

### starter-kafka

Auto-configures a `LibraryKafkaProducer` and an optional `LibraryKafkaConsumer`.  
Requires `spring-kafka` on the classpath (included transitively by this starter).

The consumer is **not** started by default; set `library.kafka.consumer.enabled=true` to activate it and avoid unwanted listeners in producer-only services.

**Dependency:**
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-kafka</artifactId>
</dependency>
```

**`application.yml`:**
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: my-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

library:
  kafka:
    default-topic: my-events               # default: "library-events"
    client-id-prefix: my-service           # default: "library"
    zookeeper-connect: localhost:2181      # default: "localhost:2181"
    schema-registry-url: http://localhost:8081
    consumer:
      enabled: false                       # default: false — set true to activate
    streams:
      application-id: my-streams-app      # default: "library-streams-app"
      state-dir: /tmp/kafka-streams        # default: "/tmp/kafka-streams"
```

**Sending messages:**
```java
@Autowired
LibraryKafkaProducer<String> producer;

producer.send(value);               // → default topic
producer.send(key, value);          // → default topic with key
producer.send(topic, key, value);   // → explicit topic with key
```

**Custom consumer:**  
Extend `LibraryKafkaConsumer` and override `process(V payload)`:
```java
@Component
public class MyConsumer extends LibraryKafkaConsumer<String> {
    @Override
    protected void process(String payload) {
        // your business logic
    }
}
```

---

### starter-test

Provides JUnit 5 / Mockito / Testcontainers utilities for unit and integration tests.  
Add it with `<scope>test</scope>` only.

**Dependency:**
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

**`BaseIntegrationTest`** — abstract base class that applies `@SpringBootTest` and activates the `test` Spring profile:
```java
@ExtendWith(SpringExtension.class)
class MyServiceIntegrationTest extends BaseIntegrationTest {
    // Spring context is started automatically
}
```

**`EmbeddedKafkaTestConfig`** — `@TestConfiguration` that starts an in-process KRaft Kafka broker (no external broker needed):
```java
@Import(EmbeddedKafkaTestConfig.class)
class MyKafkaTest extends BaseIntegrationTest {
    @Autowired
    EmbeddedKafkaBroker broker;
    // ...
}
```

---

## Full example — Pet Store microservice

A complete end-to-end example that exposes a REST API generated from an OpenAPI spec.

### Project structure

```
petstore-service/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/example/petstore/
        │       ├── PetstoreApplication.java
        │       └── controller/
        │           └── PetsControllerImpl.java
        └── resources/
            ├── application.yml
            └── openapi.yaml
```

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- Inherit from library parent to get plugin management -->
  <parent>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-parent</artifactId>
    <version>1.0.1</version>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>petstore-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <openapi.spec-path>${project.basedir}/src/main/resources/openapi.yaml</openapi.spec-path>
    <openapi.api-package>com.example.petstore.api</openapi.api-package>
    <openapi.model-package>com.example.petstore.api.model</openapi.model-package>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-openapi</artifactId>
    </dependency>

    <!-- Required by the generated models for nullable support -->
    <dependency>
      <groupId>org.openapitools</groupId>
      <artifactId>jackson-databind-nullable</artifactId>
    </dependency>

    <!-- Swagger UI at /swagger-ui.html -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- 1. Generate interfaces + models at compile time -->
      <plugin>
        <groupId>org.openapitools</groupId>
        <artifactId>openapi-generator-maven-plugin</artifactId>
      </plugin>
      <!-- 2. Register generated sources -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
      </plugin>
      <!-- 3. Package as a runnable fat JAR (override library default) -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <skip>false</skip>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### src/main/resources/openapi.yaml

```yaml
openapi: "3.0.3"
info:
  title: Pet Store API
  version: "1.0.0"
paths:
  /pets:
    get:
      tags: [pets]
      summary: List all pets
      operationId: listPets
      parameters:
        - name: limit
          in: query
          required: false
          schema:
            type: integer
      responses:
        "200":
          description: A list of pets
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/Pet"
    post:
      tags: [pets]
      summary: Create a pet
      operationId: createPet
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/Pet"
      responses:
        "201":
          description: Pet created
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Pet"
  /pets/{petId}:
    get:
      tags: [pets]
      summary: Info for a specific pet
      operationId: getPetById
      parameters:
        - name: petId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: Expected response to a valid request
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Pet"
        "404":
          description: Pet not found
components:
  schemas:
    Pet:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        tag:
          type: string
    Error:
      type: object
      properties:
        code:
          type: integer
          format: int32
        message:
          type: string
```

### src/main/resources/application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: petstore-service

library:
  openapi:
    enabled: true
    spec-path: classpath:openapi.yaml
    base-package: com.example.petstore.api

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### Application entry point

```java
// src/main/java/com/example/petstore/PetstoreApplication.java
package com.example.petstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PetstoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetstoreApplication.class, args);
    }
}
```

### Implement the generated controller interface

After `mvn generate-sources` the plugin creates:
- `com.example.petstore.api.PetsApi` — Spring MVC interface with all annotations
- `com.example.petstore.api.model.Pet` — DTO

```java
// src/main/java/com/example/petstore/controller/PetsControllerImpl.java
package com.example.petstore.controller;

import com.example.petstore.api.PetsApi;
import com.example.petstore.api.model.Pet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class PetsControllerImpl implements PetsApi {

    private final Map<Long, Pet> store = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    @Override
    public ResponseEntity<List<Pet>> listPets(Integer limit) {
        List<Pet> pets = new ArrayList<>(store.values());
        if (limit != null && limit > 0) {
            pets = pets.subList(0, Math.min(limit, pets.size()));
        }
        return ResponseEntity.ok(pets);
    }

    @Override
    public ResponseEntity<Pet> createPet(Pet pet) {
        pet.setId(counter.getAndIncrement());
        store.put(pet.getId(), pet);
        return ResponseEntity.status(HttpStatus.CREATED).body(pet);
    }

    @Override
    public ResponseEntity<Pet> getPetById(Long petId) {
        Pet pet = store.get(petId);
        return pet != null ? ResponseEntity.ok(pet) : ResponseEntity.notFound().build();
    }
}
```

### Run and test

```bash
# Build the microservice
mvn clean package

# Start it
java -jar target/petstore-service-0.0.1-SNAPSHOT.jar

# Or with the Spring Boot plugin
mvn spring-boot:run

# Create a pet
curl -s -X POST http://localhost:8080/pets \
  -H "Content-Type: application/json" \
  -d '{"name":"Fido","tag":"dog"}' | jq .

# List pets
curl -s http://localhost:8080/pets | jq .

# Get by id
curl -s http://localhost:8080/pets/1 | jq .
```

Interactive Swagger UI: **http://localhost:8080/swagger-ui.html**

---

## Publishing to Nexus / Artifactory

Configure credentials in `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>your-user</username>
      <password>your-password</password>
    </server>
    <server>
      <id>nexus-snapshots</id>
      <username>your-user</username>
      <password>your-password</password>
    </server>
  </servers>
</settings>
```

Then deploy, overriding the Nexus URLs (or set them permanently in the parent POM):

```bash
mvn deploy \
  -Dnexus.releases.url=https://nexus.your-company.com/repository/maven-releases/ \
  -Dnexus.snapshots.url=https://nexus.your-company.com/repository/maven-snapshots/
```
