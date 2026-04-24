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
- [Example 1 — Pet Store microservice (OpenAPI)](#example-1--pet-store-microservice-openapi)
- [Example 2 — Order Service microservice (OpenAPI + DB + Kafka)](#example-2--order-service-microservice-openapi--db--kafka)
- [Publishing to Nexus / Artifactory](#publishing-to-nexus--artifactory)

---

## Overview

`library-base` is a multi-module Maven project that provides ready-made Spring Boot starters for the most common cross-cutting concerns in a microservice landscape:

| Concern | Starter artifact |
|---------|-----------------|
| Shared utilities / base beans | `library-base-starter-base` |
| REST API from an OpenAPI spec | `library-base-starter-openapi` |
| Relational database (JPA + PostgreSQL) | `library-base-starter-db` |
| Apache Kafka producer / consumer / streams | `library-base-starter-kafka` |
| Test helpers (JUnit 5, embedded Kafka, Testcontainers) | `library-base-starter-test` |

All starters follow the Spring Boot auto-configuration contract: add the JAR to the classpath and set the relevant `application.yml` properties — no `@Import` or `@EnableXxx` annotations required (except for Kafka Streams, see below).

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
- Configures the Maven compiler plugin with Lombok and MapStruct annotation-processor paths in the correct order.
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

Provides shared Spring beans and a `BaseService` that is used as the foundation for the other starters.  
No mandatory configuration — works out of the box.

**Auto-configured bean:**
- `BaseService` — exposes `getApplicationName()` and `isVerboseLogging()`.

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

Reads an **OpenAPI 3 YAML/JSON spec** and on application startup generates Java source files into the configured output directory:
- **DTOs** in `<base-package>.dto` (one `*Dto.java` per schema component)
- **Controller stubs** in `<base-package>.controller` (one `*Controller.java` per tag)

The `openapi-generator-maven-plugin` declared in `library-base-parent` also generates type-safe Spring MVC interfaces and model classes **at compile time** via `mvn generate-sources`.

**Auto-configured beans:**
- `OpenApiGeneratorService` — orchestrates spec parsing and file generation.
- `ApplicationRunner` — triggers `OpenApiGeneratorService.generate()` on startup (skipped when `library.openapi.enabled=false`).

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

**Maven compile-time code-generation** (override per-project):
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

Auto-configures a `HibernateJpaVendorAdapter` targeting PostgreSQL.  
Bundles `spring-boot-starter-data-jpa` and the PostgreSQL JDBC driver — no extra dependencies needed in the consuming service.

**Auto-configured bean:**
- `JpaVendorAdapter` (`HibernateJpaVendorAdapter`) — registered only when no other `JpaVendorAdapter` bean is present. Targets `Database.POSTGRESQL` with DDL generation disabled by default.

**Dependency:**
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-db</artifactId>
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
`spring-kafka` and `kafka-streams` are bundled transitively.

The consumer listener is **not** started by default; set `library.kafka.consumer.enabled=true` to activate it and avoid unwanted listeners in producer-only services.

**Auto-configured beans:**
- `LibraryKafkaProducer<V>` — wraps `KafkaTemplate` and exposes three `send(...)` overloads.
- `LibraryKafkaConsumer<V>` — registered only when `library.kafka.consumer.enabled=true`. Override `process(V payload)` to add business logic.

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
    zookeeper-connect: localhost:2181       # default: "localhost:2181"
    schema-registry-url: http://localhost:8081
    consumer:
      enabled: false                        # default: false — set true to activate
    streams:
      application-id: my-streams-app       # default: "library-streams-app"
      state-dir: /tmp/kafka-streams         # default: "/tmp/kafka-streams"
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
public class OrderEventConsumer extends LibraryKafkaConsumer<String> {
    @Override
    protected void process(String payload) {
        // your business logic
    }
}
```

**Kafka Streams (optional):**  
`LibraryKafkaStreamsConfig` provides a pre-wired `KafkaStreamsConfiguration` and a pass-through example topology. Import it explicitly in your application class:
```java
@Import(LibraryKafkaStreamsConfig.class)
@SpringBootApplication
public class MyApplication { ... }
```

---

### starter-test

Provides shared test infrastructure for unit and integration tests:
- `BaseIntegrationTest` — abstract base class annotated with `@SpringBootTest` and `@ActiveProfiles("test")`.
- `EmbeddedKafkaTestConfig` — `@TestConfiguration` that starts an in-process KRaft Kafka broker (no external broker needed).
- Auto-wires `EmbeddedKafkaBroker` automatically when `spring-kafka-test` is on the test classpath.
- Includes H2 in-memory database, Testcontainers (JUnit 5 extension + Kafka module), and `spring-boot-testcontainers`.

**Dependency** (always `test` scope):
```xml
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-test</artifactId>
  <scope>test</scope>
</dependency>
```

**`BaseIntegrationTest`** — activates the full Spring context and the `test` profile:
```java
@ExtendWith(SpringExtension.class)
class MyServiceIntegrationTest extends BaseIntegrationTest {
    // Spring context is started automatically with the "test" profile active
}
```

**`EmbeddedKafkaTestConfig`** — import when the test needs a Kafka broker but no external one is available:
```java
@Import(EmbeddedKafkaTestConfig.class)
class MyKafkaTest extends BaseIntegrationTest {
    @Autowired
    EmbeddedKafkaBroker broker;
    // ...
}
```

**Testcontainers** — use for tests that require a real PostgreSQL or Kafka container:
```java
@Testcontainers
class MyRepositoryTest extends BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");
}
```

---

## Example 1 — Pet Store microservice (OpenAPI)

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
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-test</artifactId>
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

## Example 2 — Order Service microservice (OpenAPI + DB + Kafka)

A realistic microservice that exposes a REST API from an OpenAPI spec, persists orders in PostgreSQL, and publishes order events to Kafka.

### Project structure

```
order-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/example/orders/
    │   │       ├── OrderServiceApplication.java
    │   │       ├── controller/
    │   │       │   └── OrdersControllerImpl.java
    │   │       ├── entity/
    │   │       │   └── OrderEntity.java
    │   │       ├── repository/
    │   │       │   └── OrderRepository.java
    │   │       └── service/
    │   │           └── OrderService.java
    │   └── resources/
    │       ├── application.yml
    │       ├── openapi.yaml
    │       └── db/
    │           └── migration/
    │               └── V1__create_orders.sql
    └── test/
        └── java/
            └── com/example/orders/
                └── OrdersControllerImplTest.java
```

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-parent</artifactId>
    <version>1.0.1</version>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>order-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>

  <properties>
    <openapi.spec-path>${project.basedir}/src/main/resources/openapi.yaml</openapi.spec-path>
    <openapi.api-package>com.example.orders.api</openapi.api-package>
    <openapi.model-package>com.example.orders.api.model</openapi.model-package>
  </properties>

  <dependencies>
    <!-- Web layer -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- OpenAPI code generation + runtime spec parsing -->
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-openapi</artifactId>
    </dependency>
    <dependency>
      <groupId>org.openapitools</groupId>
      <artifactId>jackson-databind-nullable</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <!-- JPA + PostgreSQL (bundled by starter-db) -->
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-db</artifactId>
    </dependency>

    <!-- Flyway for schema migrations -->
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- Kafka producer + consumer -->
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-kafka</artifactId>
    </dependency>

    <!-- Lombok for boilerplate reduction -->
    <dependency>
      <groupId>org.projectlombok</groupId>
      <artifactId>lombok</artifactId>
      <optional>true</optional>
    </dependency>

    <!-- Test utilities -->
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.openapitools</groupId>
        <artifactId>openapi-generator-maven-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
      </plugin>
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
  title: Order Service API
  version: "1.0.0"
paths:
  /orders:
    get:
      tags: [orders]
      summary: List all orders
      operationId: listOrders
      responses:
        "200":
          description: List of orders
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: "#/components/schemas/Order"
    post:
      tags: [orders]
      summary: Create an order
      operationId: createOrder
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: "#/components/schemas/Order"
      responses:
        "201":
          description: Order created
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Order"
  /orders/{orderId}:
    get:
      tags: [orders]
      summary: Get an order by ID
      operationId: getOrderById
      parameters:
        - name: orderId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        "200":
          description: Order found
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/Order"
        "404":
          description: Order not found
components:
  schemas:
    Order:
      type: object
      properties:
        id:
          type: integer
          format: int64
        product:
          type: string
        quantity:
          type: integer
        status:
          type: string
          enum: [PENDING, CONFIRMED, SHIPPED, CANCELLED]
```

### src/main/resources/application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:postgresql://localhost:5432/ordersdb
    username: orders
    password: secret
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: order-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

library:
  base:
    application-name: order-service
  openapi:
    enabled: true
    spec-path: classpath:openapi.yaml
    base-package: com.example.orders.api
  db:
    default-schema: public
    ddl-auto: validate
  kafka:
    default-topic: order-events
    client-id-prefix: order-service
    consumer:
      enabled: true

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

### src/main/resources/db/migration/V1__create_orders.sql

```sql
CREATE TABLE IF NOT EXISTS orders (
    id       BIGSERIAL PRIMARY KEY,
    product  VARCHAR(255) NOT NULL,
    quantity INT          NOT NULL,
    status   VARCHAR(50)  NOT NULL DEFAULT 'PENDING'
);
```

### Application entry point

```java
// src/main/java/com/example/orders/OrderServiceApplication.java
package com.example.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

### JPA entity

```java
// src/main/java/com/example/orders/entity/OrderEntity.java
package com.example.orders.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String product;
    private Integer quantity;
    private String status = "PENDING";
}
```

### Spring Data repository

```java
// src/main/java/com/example/orders/repository/OrderRepository.java
package com.example.orders.repository;

import com.example.orders.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> { }
```

### Service layer

```java
// src/main/java/com/example/orders/service/OrderService.java
package com.example.orders.service;

import com.example.orders.entity.OrderEntity;
import com.example.orders.repository.OrderRepository;
import com.github.juanfranciscofernandezherreros.library.kafka.producer.LibraryKafkaProducer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final LibraryKafkaProducer<String> producer;

    public OrderService(OrderRepository repository,
                        LibraryKafkaProducer<String> producer) {
        this.repository = repository;
        this.producer = producer;
    }

    public List<OrderEntity> findAll() {
        return repository.findAll();
    }

    public Optional<OrderEntity> findById(Long id) {
        return repository.findById(id);
    }

    public OrderEntity create(OrderEntity order) {
        OrderEntity saved = repository.save(order);
        producer.send(String.valueOf(saved.getId()),
                      "ORDER_CREATED:" + saved.getId());
        return saved;
    }
}
```

### Controller implementation

```java
// src/main/java/com/example/orders/controller/OrdersControllerImpl.java
package com.example.orders.controller;

import com.example.orders.api.OrdersApi;
import com.example.orders.api.model.Order;
import com.example.orders.entity.OrderEntity;
import com.example.orders.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class OrdersControllerImpl implements OrdersApi {

    private final OrderService orderService;

    public OrdersControllerImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public ResponseEntity<List<Order>> listOrders() {
        List<Order> orders = orderService.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orders);
    }

    @Override
    public ResponseEntity<Order> createOrder(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.setProduct(order.getProduct());
        entity.setQuantity(order.getQuantity());
        entity.setStatus(order.getStatus() != null ? order.getStatus().getValue() : "PENDING");
        OrderEntity saved = orderService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<Order> getOrderById(Long orderId) {
        return orderService.findById(orderId)
                .map(e -> ResponseEntity.ok(toDto(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    private Order toDto(OrderEntity entity) {
        Order dto = new Order();
        dto.setId(entity.getId());
        dto.setProduct(entity.getProduct());
        dto.setQuantity(entity.getQuantity());
        dto.setStatus(Order.StatusEnum.fromValue(entity.getStatus()));
        return dto;
    }
}
```

### Integration test

```java
// src/test/java/com/example/orders/OrdersControllerImplTest.java
package com.example.orders;

import com.github.juanfranciscofernandezherreros.library.test.support.BaseIntegrationTest;
import com.github.juanfranciscofernandezherreros.library.test.support.EmbeddedKafkaTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@Import(EmbeddedKafkaTestConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.flyway.enabled=false",
    "library.kafka.consumer.enabled=false"
})
class OrdersControllerImplTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void listOrders_returnsEmptyListInitially() {
        ResponseEntity<String> response = restTemplate.getForEntity("/orders", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

### Run and test

```bash
# Start infrastructure (PostgreSQL + Kafka)
docker run -d --name postgres \
  -e POSTGRES_DB=ordersdb -e POSTGRES_USER=orders -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 postgres:16-alpine

docker run -d --name kafka \
  -e KAFKA_CFG_NODE_ID=0 \
  -e KAFKA_CFG_PROCESS_ROLES=controller,broker \
  -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 \
  -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@localhost:9093 \
  -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -p 9092:9092 bitnami/kafka:3.7

# Build and start the service
mvn clean package -DskipTests
java -jar target/order-service-0.0.1-SNAPSHOT.jar

# Create an order
curl -s -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"product":"Laptop","quantity":2,"status":"PENDING"}' | jq .

# List orders
curl -s http://localhost:8081/orders | jq .

# Get by id
curl -s http://localhost:8081/orders/1 | jq .
```

Interactive Swagger UI: **http://localhost:8081/swagger-ui.html**

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
