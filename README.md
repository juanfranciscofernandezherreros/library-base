# library-base

> A collection of opinionated Spring Boot auto-configuration starters for building Java microservices — covering REST/OpenAPI, databases, Kafka, and shared utilities.

---

## Table of contents

- [Overview](#overview)
- [Modules](#modules)
- [Requirements](#requirements)
- [Quick start](#quick-start)
- [Starters reference](#starters-reference)
  - [starter-base](#starter-base)
  - [starter-openapi](#starter-openapi)
  - [starter-db](#starter-db)
  - [starter-kafka](#starter-kafka)
  - [starter-test](#starter-test)
- [Example: microservice with OpenAPI (petstore)](#example-microservice-with-openapi-petstore)
  - [1 · Project structure](#1--project-structure)
  - [2 · pom.xml](#2--pomxml)
  - [3 · openapi.yaml (pre-defined spec)](#3--openapiyaml-pre-defined-spec)
  - [4 · application.yml](#4--applicationyml)
  - [5 · Implement the generated interface](#5--implement-the-generated-interface)
  - [6 · Run and test](#6--run-and-test)
- [Building the library](#building-the-library)
- [Publishing to Nexus](#publishing-to-nexus)

---

## Overview

`library-base` is a multi-module Maven project that provides ready-made Spring Boot starters for the most common cross-cutting concerns in a microservice landscape:

| Concern | Starter |
|---------|---------|
| Shared utilities / base beans | `starter-base` |
| REST API from an OpenAPI spec | `starter-openapi` |
| Relational database (JPA + Flyway) | `starter-db` |
| Apache Kafka producer / consumer | `starter-kafka` |
| Test helpers | `starter-test` |

All starters follow the Spring Boot auto-configuration contract: add the JAR to your classpath and set the relevant `application.yml` properties — no `@Import` annotations required.

---

## Modules

```
library-base/
├── parent/          ← parent POM (Java 17, Spring Boot 3.2.5, plugin management)
├── bom/             ← Bill of Materials (centralised dependency versions)
└── starters/
    ├── starter-base/
    ├── starter-openapi/
    ├── starter-db/
    ├── starter-kafka/
    └── starter-test/
```

---

## Requirements

| Tool | Minimum version |
|------|----------------|
| Java | 17 |
| Maven | 3.9 |
| Spring Boot | 3.2.5 |

---

## Quick start

### 1. Import the BOM

Add the BOM to your project so you never need to specify versions for library-base artefacts:

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-bom</artifactId>
      <version>1.0.0-SNAPSHOT</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### 2. Add the starters you need

```xml
<!-- REST API from an OpenAPI spec -->
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-openapi</artifactId>
</dependency>

<!-- Database (JPA + Flyway) -->
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-db</artifactId>
</dependency>

<!-- Kafka producer / consumer -->
<dependency>
  <groupId>com.github.juanfranciscofernandezherreros</groupId>
  <artifactId>library-base-starter-kafka</artifactId>
</dependency>
```

---

## Starters reference

### starter-base

Provides shared Spring beans and a `BaseService` used by other starters.  
No mandatory configuration — works out of the box.

```yaml
# application.yml (optional)
library:
  base:
    application-name: library-base   # default: "library-base"
    verbose-logging: false           # default: false
```

---

### starter-openapi

Reads an **OpenAPI 3 YAML/JSON spec** and generates:
- **DTOs** → `<basePackage>.dto.*Dto`
- **Controller stubs** → `<basePackage>.controller.*Controller`

Files are written to `outputDir` on every application startup (useful for local development) **and** generated at compile time by the `openapi-generator-maven-plugin` declared in the parent POM.

```yaml
# application.yml
library:
  openapi:
    enabled: true                                       # default: true
    spec-path: classpath:openapi.yaml                   # file-system path, classpath: or http(s):// URL
    output-dir: target/generated-sources/openapi        # default
    base-package: com.example.myservice.api             # default: generated.api
```

The parent POM also exposes Maven properties you can override per-project:

```xml
<properties>
  <openapi.spec-path>${project.basedir}/src/main/resources/openapi.yaml</openapi.spec-path>
  <openapi.api-package>com.example.myservice.api</openapi.api-package>
  <openapi.model-package>com.example.myservice.api.model</openapi.model-package>
</properties>
```

---

### starter-db

Auto-configures a JPA `DataSource` with a PostgreSQL `JpaVendorAdapter`.  
Add `spring-boot-starter-data-jpa` to your project to activate.

```yaml
# application.yml
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

# library-specific overrides
library:
  db:
    default-schema: public   # default: "public"
    ddl-auto: validate       # default: "validate"
```

---

### starter-kafka

Auto-configures a `LibraryKafkaProducer` and an optional `LibraryKafkaConsumer`.  
`KafkaTemplate` (from `spring-kafka`) must be on the classpath.

The consumer bean is **not** started by default; enable it explicitly to avoid unwanted listeners in producer-only services.

```yaml
# application.yml
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

# library-specific configuration
library:
  kafka:
    default-topic: library-events          # default: "library-events"
    client-id-prefix: library              # default: "library"
    zookeeper-connect: localhost:2181      # default: "localhost:2181"
    schema-registry-url: http://localhost:8081  # default: "http://localhost:8081"
    consumer:
      enabled: false                       # default: false — set to true to activate the consumer
    streams:
      application-id: library-streams-app # default: "library-streams-app"
      state-dir: /tmp/kafka-streams       # default: "/tmp/kafka-streams"
```

`LibraryKafkaProducer` exposes three `send` overloads:

```java
producer.send(value);                      // → default topic
producer.send(key, value);                 // → default topic with key
producer.send(topic, key, value);          // → explicit topic with key
```

Override `LibraryKafkaConsumer#process(V payload)` in a subclass to add your own message-handling logic.

---

### starter-test

Provides shared JUnit 5 / Mockito utilities for unit and integration tests.  
Add it with `<scope>test</scope>` only:

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
    // ...
}
```

**`EmbeddedKafkaTestConfig`** — `@TestConfiguration` that starts an in-process KRaft Kafka broker (no external broker required):

```java
@Import(EmbeddedKafkaTestConfig.class)
class MyKafkaTest extends BaseIntegrationTest {
    // ...
}
```

---

## Example: microservice with OpenAPI (petstore)

The following walkthrough creates a fully-working **Pet Store microservice** that exposes the pre-defined OpenAPI spec bundled in the `starter-openapi` test resources.

### 1 · Project structure

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

---

### 2 · pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <!-- ── Inherit from the library parent to get plugin management ── -->
  <parent>
    <groupId>com.github.juanfranciscofernandezherreros</groupId>
    <artifactId>library-base-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <groupId>com.example</groupId>
  <artifactId>petstore-service</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <packaging>jar</packaging>

  <!-- ── Override generated package names ── -->
  <properties>
    <openapi.spec-path>${project.basedir}/src/main/resources/openapi.yaml</openapi.spec-path>
    <openapi.api-package>com.example.petstore.api</openapi.api-package>
    <openapi.model-package>com.example.petstore.api.model</openapi.model-package>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>com.github.juanfranciscofernandezherreros</groupId>
        <artifactId>library-base-bom</artifactId>
        <version>1.0.0-SNAPSHOT</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <dependencies>
    <!-- Spring Boot Web -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- OpenAPI starter: parses spec + generates DTOs and controller interfaces -->
    <dependency>
      <groupId>com.github.juanfranciscofernandezherreros</groupId>
      <artifactId>library-base-starter-openapi</artifactId>
    </dependency>

    <!-- jackson-databind-nullable required by the generated models -->
    <dependency>
      <groupId>org.openapitools</groupId>
      <artifactId>jackson-databind-nullable</artifactId>
    </dependency>

    <!-- Springdoc: serves Swagger UI at /swagger-ui.html -->
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    </dependency>

    <!-- Test -->
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <!-- 1. Generate interfaces + models from openapi.yaml at compile time -->
      <plugin>
        <groupId>org.openapitools</groupId>
        <artifactId>openapi-generator-maven-plugin</artifactId>
      </plugin>

      <!-- 2. Register the generated sources so javac picks them up -->
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>build-helper-maven-plugin</artifactId>
      </plugin>

      <!-- 3. Package as a runnable fat JAR -->
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <configuration>
          <skip>false</skip>  <!-- override the library default -->
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

---

### 3 · openapi.yaml (pre-defined spec)

Copy this file to `src/main/resources/openapi.yaml`.  
This is the same spec included in `starter-openapi` as the default example.

```yaml
openapi: "3.0.3"
info:
  title: Pet Store API
  version: "1.0.0"
paths:
  /pets:
    get:
      tags:
        - pets
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
      tags:
        - pets
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
      tags:
        - pets
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

---

### 4 · application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: petstore-service

# library-base OpenAPI runtime generator (writes files on startup for dev workflows)
library:
  openapi:
    enabled: true
    spec-path: classpath:openapi.yaml
    base-package: com.example.petstore.api

# Springdoc — serves the spec at /v3/api-docs and UI at /swagger-ui.html
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

---

### 5 · Implement the generated interface

After `mvn generate-sources` (or the first `mvn compile`), the plugin generates:

- `com.example.petstore.api.PetsApi` — Spring MVC interface with all annotations
- `com.example.petstore.api.model.Pet` — DTO

Create your controller by implementing the generated interface:

```java
// src/main/java/com/example/petstore/controller/PetsControllerImpl.java
package com.example.petstore.controller;

import com.example.petstore.api.PetsApi;
import com.example.petstore.api.model.Pet;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        if (pet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(pet);
    }
}
```

Entry-point class:

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

---

### 6 · Run and test

```bash
# Build and run
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

Open the interactive Swagger UI at: **http://localhost:8080/swagger-ui.html**

---

## Building the library

```bash
# Install all modules into local .m2
mvn clean install

# Skip tests (faster)
mvn clean install -DskipTests

# Build a single starter
mvn clean install -pl starters/starter-openapi -am
```

---

## Publishing to Nexus

Configure your Nexus credentials in `~/.m2/settings.xml`:

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

Then override the Nexus URLs (or set them in the parent POM):

```bash
mvn deploy \
  -Dnexus.releases.url=https://nexus.your-company.com/repository/maven-releases/ \
  -Dnexus.snapshots.url=https://nexus.your-company.com/repository/maven-snapshots/
```
